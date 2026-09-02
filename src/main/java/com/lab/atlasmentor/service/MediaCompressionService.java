package com.lab.atlasmentor.service;

import com.lab.atlasmentor.util.AttachmentTypeUtil.Category;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * The one shared compression/normalization method for task attachments - both the
 * proof-section and comment-section upload flows go through
 * {@link #compress(byte[], String, Category, String)} via TaskAttachmentUploadService.
 * There is deliberately no per-context copy of any of this logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaCompressionService {

    private static final int MAX_IMAGE_DIMENSION = 1920;
    private static final double IMAGE_QUALITY = 0.8;
    private static final String AUDIO_BITRATE = "48k";
    private static final String VIDEO_MAX_HEIGHT = "720";
    private static final String VIDEO_BITRATE = "1500k";
    private static final String VIDEO_AUDIO_BITRATE = "128k";

    private final FfmpegAvailabilityService ffmpegAvailabilityService;

    public record CompressedFile(byte[] content, String fileName, String contentType, boolean compressed) {}

    /**
     * @param logLabel identifies the upload in log lines (e.g. "task 42 attachment
     *                 photo.png") - purely for the WARN emitted when FFmpeg is
     *                 unavailable, so it's traceable back to the request that hit it.
     */
    public CompressedFile compress(byte[] originalContent, String originalFileName, Category category, String logLabel) {
        try {
            return switch (category) {
                case IMAGE -> compressImage(originalContent, originalFileName);
                case AUDIO -> compressAudio(originalContent, originalFileName, logLabel);
                case VIDEO -> compressVideo(originalContent, originalFileName, logLabel);
                case DOCUMENT -> new CompressedFile(originalContent, originalFileName, "application/octet-stream", false);
            };
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Compression failed for {} ({}) - storing original file uncompressed instead.", logLabel, category, e);
            return new CompressedFile(originalContent, originalFileName, "application/octet-stream", false);
        }
    }

    // ---- Images: resize (no upscale) to max 1920px long side, re-encode JPEG @ ~80% ----

    private CompressedFile compressImage(byte[] originalContent, String originalFileName) throws IOException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(originalContent));
        if (source == null) {
            throw new IOException("Could not decode image: " + originalFileName);
        }

        int longestSide = Math.max(source.getWidth(), source.getHeight());
        Thumbnails.Builder<BufferedImage> builder = Thumbnails.of(source).outputQuality(IMAGE_QUALITY);
        if (longestSide > MAX_IMAGE_DIMENSION) {
            builder = builder.size(MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION);
        } else {
            // Already within bounds - re-encode at the same size rather than upscaling.
            builder = builder.scale(1.0);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        builder.outputFormat("jpg").toOutputStream(out);

        String newFileName = withExtension(originalFileName, "jpg");
        return new CompressedFile(out.toByteArray(), newFileName, "image/jpeg", true);
    }

    // ---- Audio: transcode to AAC ~48kbps via FFmpeg, if available ----

    private CompressedFile compressAudio(byte[] originalContent, String originalFileName, String logLabel)
            throws IOException, InterruptedException {
        if (!ffmpegAvailabilityService.isAvailable()) {
            log.warn("FFmpeg unavailable - storing audio attachment '{}' uncompressed.", logLabel);
            return new CompressedFile(originalContent, originalFileName, "application/octet-stream", false);
        }

        return runFfmpeg(originalContent, originalFileName, logLabel, "m4a", "audio/mp4",
                (inPath, outPath) -> new String[]{
                        "ffmpeg", "-y", "-i", inPath.toString(),
                        "-vn", "-c:a", "aac", "-b:a", AUDIO_BITRATE,
                        outPath.toString()
                });
    }

    // ---- Video: transcode to <=720p / moderate bitrate H.264+AAC via FFmpeg, if available ----

    private CompressedFile compressVideo(byte[] originalContent, String originalFileName, String logLabel)
            throws IOException, InterruptedException {
        if (!ffmpegAvailabilityService.isAvailable()) {
            // Spec-mandated fallback for video specifically: store the original rather
            // than reject or silently drop it.
            log.warn("FFmpeg unavailable - storing video attachment '{}' uncompressed. Video is the file type "
                    + "most likely to blow through storage budgets uncompressed; installing FFmpeg on the "
                    + "server is worth prioritizing.", logLabel);
            return new CompressedFile(originalContent, originalFileName, "application/octet-stream", false);
        }

        return runFfmpeg(originalContent, originalFileName, logLabel, "mp4", "video/mp4",
                (inPath, outPath) -> new String[]{
                        "ffmpeg", "-y", "-i", inPath.toString(),
                        // ProcessBuilder passes args directly (no shell), so no shell quoting here -
                        // ffmpeg's own filter-expression parser handles min()/ih natively.
                        // -2 keeps width even (required by libx264) while preserving aspect ratio;
                        // min(720,ih) caps height at 720 without ever upscaling a shorter source.
                        "-vf", "scale=-2:min(" + VIDEO_MAX_HEIGHT + ",ih)",
                        "-c:v", "libx264", "-b:v", VIDEO_BITRATE,
                        "-c:a", "aac", "-b:a", VIDEO_AUDIO_BITRATE,
                        outPath.toString()
                });
    }

    @FunctionalInterface
    private interface FfmpegCommandBuilder {
        String[] build(Path inPath, Path outPath);
    }

    private CompressedFile runFfmpeg(byte[] originalContent, String originalFileName, String logLabel,
                                      String outExtension, String outContentType, FfmpegCommandBuilder commandBuilder)
            throws IOException, InterruptedException {
        Path tempDir = Files.createTempDirectory("attachment-compress-");
        Path inPath = tempDir.resolve("in-" + UUID.randomUUID() + "-" + originalFileName);
        Path outPath = tempDir.resolve("out-" + UUID.randomUUID() + "." + outExtension);
        try {
            Files.write(inPath, originalContent);

            Process process = new ProcessBuilder(commandBuilder.build(inPath, outPath))
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("FFmpeg timed out compressing " + logLabel);
            }
            if (process.exitValue() != 0 || !Files.exists(outPath) || Files.size(outPath) == 0) {
                throw new IOException("FFmpeg exited with code " + process.exitValue() + " compressing " + logLabel);
            }

            byte[] compressed = Files.readAllBytes(outPath);
            return new CompressedFile(compressed, withExtension(originalFileName, outExtension), outContentType, true);
        } finally {
            deleteQuietly(inPath);
            deleteQuietly(outPath);
            deleteQuietly(tempDir);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort temp-file cleanup; leaving a stray temp file is harmless.
        }
    }

    private String withExtension(String fileName, String newExtension) {
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        return base + "." + newExtension;
    }
}

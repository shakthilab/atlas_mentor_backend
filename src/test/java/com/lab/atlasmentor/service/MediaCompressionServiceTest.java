package com.lab.atlasmentor.service;

import com.lab.atlasmentor.util.AttachmentTypeUtil.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Real (no-mock) image compression via Thumbnailator - proves resize/re-encode
 * actually shrinks a large image and never upscales a small one. Audio/video only
 * exercise the "FFmpeg unavailable" fallback here, since this environment has no
 * FFmpeg installed to compress against (see FfmpegAvailabilityService).
 */
@ExtendWith(MockitoExtension.class)
class MediaCompressionServiceTest {

    @Mock
    private FfmpegAvailabilityService ffmpegAvailabilityService;

    @InjectMocks
    private MediaCompressionService mediaCompressionService;

    @Test
    void largeImageIsResizedToMaxDimensionAndShrinksInBytes() throws IOException {
        byte[] original = solidColorJpeg(3000, 2000, Color.BLUE);

        MediaCompressionService.CompressedFile result =
                mediaCompressionService.compress(original, "big-photo.jpg", Category.IMAGE, "test");

        System.out.printf("[compression report] 3000x2000 solid-color JPEG: %d bytes -> %d bytes (%.0f%% of original)%n",
                original.length, result.content().length, 100.0 * result.content().length / original.length);

        assertTrue(result.compressed());
        assertTrue(result.content().length < original.length,
                "expected compressed image to be smaller than the original 3000x2000 JPEG");

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.content()));
        assertEquals(1920, Math.max(decoded.getWidth(), decoded.getHeight()),
                "long side should be resized down to the 1920px cap");
        assertTrue(decoded.getWidth() <= 1920 && decoded.getHeight() <= 1920);
    }

    @Test
    void largeNoisyImageStillShrinksSubstantially() throws IOException {
        // A solid-color image compresses unrealistically well under JPEG regardless of
        // resizing - random noise is closer to a real photo's entropy, so the size drop
        // here is attributable mainly to the 1920px resize rather than JPEG's own
        // redundancy removal on a trivial input.
        byte[] original = noisyJpeg(3000, 2000);

        MediaCompressionService.CompressedFile result =
                mediaCompressionService.compress(original, "photo.jpg", Category.IMAGE, "test");

        System.out.printf("[compression report] 3000x2000 noisy JPEG: %d bytes -> %d bytes (%.0f%% of original)%n",
                original.length, result.content().length, 100.0 * result.content().length / original.length);

        assertTrue(result.content().length < original.length);
    }

    @Test
    void smallImageIsNotUpscaled() throws IOException {
        byte[] original = solidColorJpeg(800, 600, Color.RED);

        MediaCompressionService.CompressedFile result =
                mediaCompressionService.compress(original, "small-photo.jpg", Category.IMAGE, "test");

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.content()));
        assertEquals(800, decoded.getWidth());
        assertEquals(600, decoded.getHeight());
    }

    @Test
    void imageOutputFileNameIsRenamedToJpgExtension() throws IOException {
        byte[] original = solidColorJpeg(100, 100, Color.GREEN);

        MediaCompressionService.CompressedFile result =
                mediaCompressionService.compress(original, "photo.PNG".toLowerCase(), Category.IMAGE, "test");

        assertEquals("photo.jpg", result.fileName());
    }

    @Test
    void documentsAreStoredAsIsWithNoCompressionAttempted() {
        byte[] original = "not really a pdf, just bytes".getBytes();

        MediaCompressionService.CompressedFile result =
                mediaCompressionService.compress(original, "report.pdf", Category.DOCUMENT, "test");

        assertFalse(result.compressed());
        assertArrayEquals(original, result.content());
        assertEquals("report.pdf", result.fileName());
    }

    @Test
    void audioFallsBackToOriginalWhenFfmpegUnavailable() {
        when(ffmpegAvailabilityService.isAvailable()).thenReturn(false);
        byte[] original = "fake audio bytes".getBytes();

        MediaCompressionService.CompressedFile result =
                mediaCompressionService.compress(original, "note.wav", Category.AUDIO, "test");

        assertFalse(result.compressed());
        assertArrayEquals(original, result.content());
    }

    @Test
    void videoFallsBackToOriginalWhenFfmpegUnavailable() {
        when(ffmpegAvailabilityService.isAvailable()).thenReturn(false);
        byte[] original = "fake video bytes".getBytes();

        MediaCompressionService.CompressedFile result =
                mediaCompressionService.compress(original, "clip.mp4", Category.VIDEO, "test");

        assertFalse(result.compressed());
        assertArrayEquals(original, result.content());
    }

    private byte[] noisyJpeg(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.util.Random random = new java.util.Random(42);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, random.nextInt(0xFFFFFF));
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }

    private byte[] solidColorJpeg(int width, int height, Color color) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }
}

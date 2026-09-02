package com.lab.atlasmentor.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Probes once, at startup, whether `ffmpeg` is on the server's PATH - audio/video
 * compression (MediaCompressionService) needs it; Java has no native audio/video
 * transcoding of its own. Never fails startup: if it's missing, audio/video uploads
 * fall back to storing the original file uncompressed (video - required by spec) with
 * a clear WARN per file, rather than silently skipping compression.
 */
@Slf4j
@Service
public class FfmpegAvailabilityService {

    private volatile boolean available;

    @PostConstruct
    void probe() {
        available = checkAvailable();
        if (available) {
            log.info("FFmpeg detected on PATH - audio/video attachment compression is enabled.");
        } else {
            log.warn("FFmpeg was NOT found on this server's PATH. Audio and video task attachments will be "
                    + "stored uncompressed until FFmpeg is installed - see MediaCompressionService. "
                    + "Install FFmpeg on the deployment host and restart the app to enable compression.");
        }
    }

    public boolean isAvailable() {
        return available;
    }

    private boolean checkAvailable() {
        try {
            Process process = new ProcessBuilder("ffmpeg", "-version").start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            // Covers IOException (binary not found) and InterruptedException alike -
            // any failure to run it at all means "not available."
            return false;
        }
    }
}

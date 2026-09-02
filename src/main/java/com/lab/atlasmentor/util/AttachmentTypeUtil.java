package com.lab.atlasmentor.util;

import java.util.Locale;
import java.util.Map;

/**
 * Single source of truth for "what kind of file is this" across the task-attachment
 * upload pipeline (proof section and comment section alike - see
 * TaskAttachmentUploadService). Used both to validate/reject an upload by extension
 * and to label an already-stored attachment (TaskAttachmentResponse#fileType) so the
 * frontend never has to duplicate this mapping.
 */
public final class AttachmentTypeUtil {

    public enum Category {
        IMAGE, VIDEO, AUDIO, DOCUMENT
    }

    private static final Map<String, Category> EXTENSION_TO_CATEGORY = Map.ofEntries(
            Map.entry("jpg", Category.IMAGE),
            Map.entry("jpeg", Category.IMAGE),
            Map.entry("png", Category.IMAGE),
            Map.entry("webp", Category.IMAGE),

            Map.entry("mp4", Category.VIDEO),
            Map.entry("mov", Category.VIDEO),

            Map.entry("mp3", Category.AUDIO),
            Map.entry("m4a", Category.AUDIO),
            Map.entry("ogg", Category.AUDIO),
            Map.entry("wav", Category.AUDIO),
            // Default output of the browser MediaRecorder API for voice notes (Chrome/most
            // browsers record audio/webm;codecs=opus) - FFmpeg decodes it natively, so it
            // rides the same compress-to-AAC path as every other audio upload.
            Map.entry("webm", Category.AUDIO),

            Map.entry("pdf", Category.DOCUMENT),
            Map.entry("doc", Category.DOCUMENT),
            Map.entry("docx", Category.DOCUMENT),
            Map.entry("xls", Category.DOCUMENT),
            Map.entry("xlsx", Category.DOCUMENT),
            Map.entry("csv", Category.DOCUMENT)
    );

    private AttachmentTypeUtil() {}

    /** The comma-separated list of accepted extensions, for the 400 rejection message. */
    public static final String ACCEPTED_EXTENSIONS_MESSAGE =
            "jpg, jpeg, png, webp, mp4, mov, mp3, m4a, ogg, wav, webm, pdf, doc, docx, xls, xlsx, csv";

    public static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** Null when the extension isn't one of the accepted types. */
    public static Category categorize(String fileName) {
        return EXTENSION_TO_CATEGORY.get(extensionOf(fileName));
    }

    public static boolean isAccepted(String fileName) {
        return categorize(fileName) != null;
    }
}

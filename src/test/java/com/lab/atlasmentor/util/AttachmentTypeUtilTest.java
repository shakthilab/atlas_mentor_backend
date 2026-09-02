package com.lab.atlasmentor.util;

import com.lab.atlasmentor.util.AttachmentTypeUtil.Category;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AttachmentTypeUtilTest {

    @Test
    void categorizesWebmAsAudio() {
        // Chrome/most browsers' MediaRecorder API defaults to audio/webm;codecs=opus for
        // voice notes - must be accepted, not rejected as "unsupported file type."
        assertEquals(Category.AUDIO, AttachmentTypeUtil.categorize("voice-note.webm"));
        assertTrue(AttachmentTypeUtil.isAccepted("voice-note.webm"));
    }

    @Test
    void categorizesKnownExtensionsCorrectly() {
        assertEquals(Category.IMAGE, AttachmentTypeUtil.categorize("photo.JPG"));
        assertEquals(Category.VIDEO, AttachmentTypeUtil.categorize("clip.mp4"));
        assertEquals(Category.AUDIO, AttachmentTypeUtil.categorize("note.mp3"));
        assertEquals(Category.DOCUMENT, AttachmentTypeUtil.categorize("report.pdf"));
    }

    @Test
    void rejectsUnknownExtensions() {
        assertNull(AttachmentTypeUtil.categorize("malware.exe"));
        assertFalse(AttachmentTypeUtil.isAccepted("malware.exe"));
        assertNull(AttachmentTypeUtil.categorize("no-extension"));
    }
}

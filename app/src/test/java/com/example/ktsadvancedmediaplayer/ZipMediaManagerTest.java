package com.example.ktsadvancedmediaplayer;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ZipMediaManagerTest {

    @Test
    public void testNaturalOrderSorting() {
        List<String> tracks = Arrays.asList(
                "Track 10.mp3",
                "Track 1.mp3",
                "Track 20.mp3",
                "Track 2.mp3",
                "Track 3.mp3",
                "Track 11.mp3"
        );

        List<String> sorted = new ArrayList<>(tracks);
        sorted.sort(ZipMediaManager.NATURAL_ORDER_COMPARATOR);

        List<String> expected = Arrays.asList(
                "Track 1.mp3",
                "Track 2.mp3",
                "Track 3.mp3",
                "Track 10.mp3",
                "Track 11.mp3",
                "Track 20.mp3"
        );

        assertEquals(expected, sorted);
    }

    @Test
    public void testMultiDiscOrdering() {
        List<String> tracks = Arrays.asList(
                "Disc 2/01 - Song.mp3",
                "Disc 1/10 - Song.mp3",
                "Disc 1/02 - Song.mp3",
                "Disc 1/01 - Song.mp3",
                "Disc 2/02 - Song.mp3"
        );

        List<String> sorted = new ArrayList<>(tracks);
        sorted.sort(ZipMediaManager.NATURAL_ORDER_COMPARATOR);

        List<String> expected = Arrays.asList(
                "Disc 1/01 - Song.mp3",
                "Disc 1/02 - Song.mp3",
                "Disc 1/10 - Song.mp3",
                "Disc 2/01 - Song.mp3",
                "Disc 2/02 - Song.mp3"
        );

        assertEquals(expected, sorted);
    }

    @Test
    public void testAudioAndZipDetection() {
        assertTrue(ZipMediaManager.isZipFile("album.zip"));
        assertTrue(ZipMediaManager.isZipFile("ALBUM.ZIP"));
        assertFalse(ZipMediaManager.isZipFile("album.mp3"));

        assertTrue(ZipMediaManager.isAudioFile("song.mp3"));
        assertTrue(ZipMediaManager.isAudioFile("recording.m4a"));
        assertTrue(ZipMediaManager.isAudioFile("speech.wav"));
        assertTrue(ZipMediaManager.isAudioFile("audiobook.m4b"));
        assertTrue(ZipMediaManager.isAudioFile("track.opus"));
        assertTrue(ZipMediaManager.isAudioFile("track.flac"));
        assertFalse(ZipMediaManager.isAudioFile("cover.jpg"));
        assertFalse(ZipMediaManager.isAudioFile("info.txt"));
    }
}

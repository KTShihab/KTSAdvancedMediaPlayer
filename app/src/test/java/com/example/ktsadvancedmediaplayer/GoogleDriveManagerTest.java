package com.example.ktsadvancedmediaplayer;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GoogleDriveManagerTest {

    @Test
    public void testUrlDetection() {
        assertTrue(GoogleDriveManager.isGoogleDriveUrl("https://drive.google.com/drive/folders/1abcDEFghiJKLmnOP-qrsTUVwxYZ"));
        assertTrue(GoogleDriveManager.isGoogleDriveUrl("https://drive.google.com/file/d/1abcDEFghiJKLmnOP-qrsTUVwxYZ/view?usp=sharing"));
        assertTrue(GoogleDriveManager.isGoogleDriveUrl("https://drive.google.com/open?id=1abcDEFghiJKLmnOP-qrsTUVwxYZ"));
        assertFalse(GoogleDriveManager.isGoogleDriveUrl("https://example.com/audio.mp3"));

        assertTrue(GoogleDriveManager.isDriveFolder("https://drive.google.com/drive/folders/1abcDEFghiJKLmnOP-qrsTUVwxYZ"));
        assertTrue(GoogleDriveManager.isDriveFolder("https://drive.google.com/drive/u/0/folders/1abcDEFghiJKLmnOP-qrsTUVwxYZ?usp=sharing"));
        assertFalse(GoogleDriveManager.isDriveFolder("https://drive.google.com/file/d/1abcDEFghiJKLmnOP-qrsTUVwxYZ/view"));

        assertTrue(GoogleDriveManager.isDriveFile("https://drive.google.com/file/d/1abcDEFghiJKLmnOP-qrsTUVwxYZ/view?usp=sharing"));
        assertTrue(GoogleDriveManager.isDriveFile("https://drive.google.com/uc?id=1abcDEFghiJKLmnOP-qrsTUVwxYZ&export=download"));
    }

    @Test
    public void testIdExtraction() {
        assertEquals("1abcDEFghiJKLmnOP-qrsTUVwxYZ",
                GoogleDriveManager.extractDriveId("https://drive.google.com/drive/folders/1abcDEFghiJKLmnOP-qrsTUVwxYZ"));

        assertEquals("1abcDEFghiJKLmnOP-qrsTUVwxYZ",
                GoogleDriveManager.extractDriveId("https://drive.google.com/drive/u/2/folders/1abcDEFghiJKLmnOP-qrsTUVwxYZ?usp=sharing"));

        assertEquals("1FILE_id_TEST_1234567890",
                GoogleDriveManager.extractDriveId("https://drive.google.com/file/d/1FILE_id_TEST_1234567890/view?usp=sharing"));

        assertEquals("1OPEN_id_TEST_1234567890",
                GoogleDriveManager.extractDriveId("https://drive.google.com/open?id=1OPEN_id_TEST_1234567890"));
    }

    @Test
    public void testDirectStreamUrl() {
        String streamUrl = GoogleDriveManager.getDirectStreamUrl("test1234567890");
        assertTrue(streamUrl.contains("test1234567890"));
        assertTrue(streamUrl.contains("export=download"));
    }

    @Test
    public void testNaturalOrderingOfDriveTracks() {
        List<GoogleDriveManager.DriveTrack> tracks = new ArrayList<>(Arrays.asList(
                new GoogleDriveManager.DriveTrack("id10", "10 - Track Ten.mp3", "http://url10"),
                new GoogleDriveManager.DriveTrack("id1", "1 - Track One.mp3", "http://url1"),
                new GoogleDriveManager.DriveTrack("id2", "2 - Track Two.mp3", "http://url2"),
                new GoogleDriveManager.DriveTrack("id20", "20 - Track Twenty.mp3", "http://url20")
        ));

        tracks.sort((t1, t2) -> ZipMediaManager.NATURAL_ORDER_COMPARATOR.compare(t1.title, t2.title));

        assertEquals("1 - Track One.mp3", tracks.get(0).title);
        assertEquals("2 - Track Two.mp3", tracks.get(1).title);
        assertEquals("10 - Track Ten.mp3", tracks.get(2).title);
        assertEquals("20 - Track Twenty.mp3", tracks.get(3).title);
    }
}

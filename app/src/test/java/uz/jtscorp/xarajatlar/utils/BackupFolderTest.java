package uz.jtscorp.xarajatlar.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

public class BackupFolderTest {
    private File root;

    @Before
    public void setUp() throws IOException {
        root = Files.createTempDirectory("backup-folder-test").toFile();
    }

    @After
    public void tearDown() throws IOException {
        try (Stream<java.nio.file.Path> paths = Files.walk(root.toPath())) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        }
    }

    @Test
    public void ensureCreatesTheFolderWhenItIsMissing() throws IOException {
        File folder = new File(root, "backups");

        File result = BackupFolder.ensure(folder);

        assertEquals(folder, result);
        assertTrue(folder.isDirectory());
    }

    @Test
    public void ensureLeavesAnExistingFolderAndItsContentsUntouched() throws IOException {
        File folder = new File(root, "backups");
        assertTrue(folder.mkdirs());
        File other = write(new File(folder, "holiday.jpg"), "not ours");
        File old = write(new File(folder, "xarajatlar_backup_2026-04-05_132444.zip"), "old backup");
        File nested = write(new File(new File(folder, "teg_local"), "notes.txt"), "nested");

        BackupFolder.ensure(folder);

        assertEquals("not ours", read(other));
        assertEquals("old backup", read(old));
        assertEquals("nested", read(nested));
        assertEquals(3, folder.list().length);
    }

    @Test
    public void ensureRefusesWhenAFileHasTheFolderName() throws IOException {
        File file = write(new File(root, "backups"), "a file, not a folder");

        assertThrows(NotDirectoryException.class, () -> BackupFolder.ensure(file));

        assertEquals("a file, not a folder", read(file));
    }

    @Test
    public void saveAddsTheBackupWithoutTouchingOtherFiles() throws IOException {
        File folder = new File(root, "backups");
        assertTrue(folder.mkdirs());
        File other = write(new File(folder, "holiday.jpg"), "not ours");
        File source = write(new File(root, "temp.zip"), "new backup");

        File saved = BackupFolder.save(folder, source, "xarajatlar_backup_2026-10-09_220000.zip");

        assertEquals(new File(folder, "xarajatlar_backup_2026-10-09_220000.zip"), saved);
        assertEquals("new backup", read(saved));
        assertEquals("not ours", read(other));
        String[] names = folder.list();
        Arrays.sort(names);
        assertArrayEquals(new String[]{"holiday.jpg", "xarajatlar_backup_2026-10-09_220000.zip"}, names);
    }

    @Test
    public void saveCreatesTheFolderWhenItIsMissing() throws IOException {
        File folder = new File(root, "backups");
        File source = write(new File(root, "temp.zip"), "new backup");

        File saved = BackupFolder.save(folder, source, "backup.zip");

        assertEquals("new backup", read(saved));
    }

    @Test
    public void saveNeverOverwritesAnExistingBackupWithTheSameName() throws IOException {
        File folder = new File(root, "backups");
        assertTrue(folder.mkdirs());
        File existing = write(new File(folder, "backup.zip"), "earlier backup");
        File source = write(new File(root, "temp.zip"), "new backup");

        File saved = BackupFolder.save(folder, source, "backup.zip");

        assertNotEquals(existing, saved);
        assertEquals("backup_1.zip", saved.getName());
        assertEquals("earlier backup", read(existing));
        assertEquals("new backup", read(saved));
    }

    private static File write(File file, String text) throws IOException {
        file.getParentFile().mkdirs();
        Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}

package uz.jtscorp.xarajatlar.utils;

import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;

/**
 * Zaxiralar uchun standart papka: telefon xotirasining ildizidagi "backups". Ilova o'chirilganda
 * yo'qolmaydi. Bu klass papkaga faqat fayl qo'shadi — hech narsani o'chirmaydi va ustiga yozmaydi.
 */
public final class BackupFolder {
    public static final String NAME = "backups";

    private BackupFolder() {}

    public static File directory() {
        return new File(Environment.getExternalStorageDirectory(), NAME);
    }

    /** "Barcha fayllarga kirish" ruxsati berilganmi. */
    public static boolean hasAccess() {
        return Environment.isExternalStorageManager();
    }

    /** Papka bor bo'lsa o'zini qaytaradi (ichiga tegmaydi), yo'q bo'lsa yaratadi. */
    static File ensure(File directory) throws IOException {
        if (directory.isDirectory()) return directory;
        if (directory.exists()) throw new NotDirectoryException(directory.getPath());
        if (!directory.mkdirs() && !directory.isDirectory()) {
            throw new IOException("Cannot create " + directory.getPath());
        }
        return directory;
    }

    /** Faylni papkaga nusxalaydi; shu nomli fayl bo'lsa yangi nom tanlaydi. */
    static File save(File directory, File source, String filename) throws IOException {
        File target = unusedFile(ensure(directory), filename);
        Files.copy(source.toPath(), target.toPath());
        return target;
    }

    private static File unusedFile(File directory, String filename) {
        File candidate = new File(directory, filename);
        int dot = filename.lastIndexOf('.');
        String base = dot < 0 ? filename : filename.substring(0, dot);
        String extension = dot < 0 ? "" : filename.substring(dot);
        for (int suffix = 1; candidate.exists(); suffix++) {
            candidate = new File(directory, base + "_" + suffix + extension);
        }
        return candidate;
    }
}

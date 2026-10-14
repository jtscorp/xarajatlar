package uz.jtscorp.xarajatlar.utils;

import android.content.Context;
import java.io.File;
import java.io.IOException;

/** Restricts evidence file operations to the app-owned debt media directory. */
public final class EvidenceFiles {
    private EvidenceFiles() {}

    public static boolean isManaged(Context context, String path) {
        if (path == null || path.isBlank()) return false;
        try {
            File root = new File(context.getFilesDir(), "debts").getCanonicalFile();
            File candidate = new File(path).getCanonicalFile();
            return candidate.getPath().startsWith(root.getPath() + File.separator);
        } catch (IOException error) {
            return false;
        }
    }

    public static boolean deleteManaged(Context context, String path) {
        if (!isManaged(context, path)) return false;
        File file = new File(path);
        return !file.exists() || file.delete();
    }

    public static File managedRegularFile(Context context, String path) {
        if (!isManaged(context, path)) return null;
        try {
            File file = new File(path).getCanonicalFile();
            return file.isFile() ? file : null;
        } catch (IOException error) {
            return null;
        }
    }

    public static String backupPath(Context context, String path, int evidenceId) {
        if (isManaged(context, path)) {
            try {
                return new File(path).getCanonicalPath();
            } catch (IOException ignored) {
                // Fall through to a path that reveals nothing outside app storage.
            }
        }
        return "missing/evidence-" + evidenceId + ".missing";
    }
}

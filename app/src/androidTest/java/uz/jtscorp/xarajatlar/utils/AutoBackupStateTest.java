package uz.jtscorp.xarajatlar.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AutoBackupStateTest {
    private final Context context =
            InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Before
    @After
    public void reset() {
        AutoBackupState.clear(context);
    }

    @Test
    public void autoBackupIsEnabledByDefault() {
        assertTrue(AutoBackupState.isEnabled(context));
        assertEquals(0, AutoBackupState.lastSuccessTime(context));
        assertNull(AutoBackupState.lastError(context));
    }

    @Test
    public void disablingIsRemembered() {
        AutoBackupState.setEnabled(context, false);

        assertFalse(AutoBackupState.isEnabled(context));
    }

    @Test
    public void failureIsReportedUntilTheNextSuccess() {
        AutoBackupState.recordSuccess(context, 1_000);
        AutoBackupState.recordFailure(context, "no permission");

        assertEquals("no permission", AutoBackupState.lastError(context));
        assertEquals(1_000, AutoBackupState.lastSuccessTime(context));

        AutoBackupState.recordSuccess(context, 2_000);

        assertNull(AutoBackupState.lastError(context));
        assertEquals(2_000, AutoBackupState.lastSuccessTime(context));
    }

    @Test
    public void failureWithoutAMessageIsStillVisible() {
        AutoBackupState.recordFailure(context, null);

        assertEquals("", AutoBackupState.lastError(context));
    }
}

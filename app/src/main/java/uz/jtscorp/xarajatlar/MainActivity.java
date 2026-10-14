package uz.jtscorp.xarajatlar;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import uz.jtscorp.xarajatlar.ui.camera.OcrScanActivity;
import uz.jtscorp.xarajatlar.ui.camera.QrScanActivity;
import uz.jtscorp.xarajatlar.ui.expense.ManualExpenseActivity;
import uz.jtscorp.xarajatlar.ui.income.AddIncomeActivity;
import uz.jtscorp.xarajatlar.utils.AutoBackupWorker;
import uz.jtscorp.xarajatlar.utils.SecurePreferences;

public class MainActivity extends AppCompatActivity {
    private static final long LOCK_TIMEOUT = 60_000;
    private SecurePreferences securePrefs;
    private FloatingActionButton addButton;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        securePrefs = new SecurePreferences(this);
        View navHostView = findViewById(R.id.nav_host_fragment);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        addButton = findViewById(R.id.fabMain);
        ViewCompat.setOnApplyWindowInsetsListener(navHostView, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, bars.top, 0, view.getPaddingBottom());
            return insets;
        });
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, 0, 0, bars.bottom);
            return insets;
        });
        bottomNav.addOnLayoutChangeListener((view, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> navHostView.setPadding(
                navHostView.getPaddingLeft(), navHostView.getPaddingTop(),
                navHostView.getPaddingRight(), bottom - top));
        NavHostFragment host = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (host != null) {
            NavController controller = host.getNavController();
            NavigationUI.setupWithNavController(bottomNav, controller);
            controller.addOnDestinationChangedListener((ignored, destination, arguments) -> {
                boolean visible = destination.getId() == R.id.navigation_history;
                if (visible) addButton.show(); else addButton.hide();
            });
        }
        addButton.setOnClickListener(v -> showAddSheet());
        scheduleAutoBackup();
    }

    public void showAddSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = LayoutInflater.from(this).inflate(R.layout.sheet_add_transaction, null);
        bindAction(sheet, R.id.addManualExpense, dialog, ManualExpenseActivity.class);
        bindAction(sheet, R.id.addIncome, dialog, AddIncomeActivity.class);
        bindAction(sheet, R.id.addQrExpense, dialog, QrScanActivity.class);
        bindAction(sheet, R.id.addOcrExpense, dialog, OcrScanActivity.class);
        dialog.setContentView(sheet);
        dialog.show();
    }

    private void bindAction(View sheet, int id, BottomSheetDialog dialog, Class<?> activity) {
        sheet.findViewById(id).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, activity));
        });
    }

    @Override protected void onResume() {
        super.onResume();
        if (System.currentTimeMillis() - securePrefs.getLastLockTime() > LOCK_TIMEOUT) {
            Intent lock = new Intent(this, uz.jtscorp.xarajatlar.ui.auth.LockActivity.class);
            lock.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(lock);
            finish();
        }
    }

    @Override protected void onPause() {
        super.onPause();
        securePrefs.setLastLockTime(System.currentTimeMillis());
    }

    private void scheduleAutoBackup() {
        AutoBackupWorker.sync(this);
    }
}

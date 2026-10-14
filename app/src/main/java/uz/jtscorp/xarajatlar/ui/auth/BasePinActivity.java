package uz.jtscorp.xarajatlar.ui.auth;

import android.os.Bundle;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.utils.SecurePreferences;

/**
 * Umumiy PIN kiritish logikasi — keypad, dot lar, edge-to-edge.
 * LockActivity va SetupActivity shu klassdan extend qiladi.
 */
public abstract class BasePinActivity extends AppCompatActivity {
    protected static final int PIN_LENGTH = 4;

    protected TextView titleText, subtitleText, errorText;
    protected LinearLayout pinDotsContainer;
    protected GridLayout keypadGrid;
    protected MaterialButton biometricButton;
    protected SecurePreferences securePrefs;

    private final View[] pinDots = new View[PIN_LENGTH];
    protected final StringBuilder currentPin = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_base);

        if (securePrefs == null) {
            securePrefs = new SecurePreferences(this);
        }

        // Edge-to-edge insets
        View pinRoot = findViewById(R.id.pinRoot);
        ViewCompat.setOnApplyWindowInsetsListener(pinRoot, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    v.getPaddingLeft(),
                    insets.top + dpToPx(16),
                    v.getPaddingRight(),
                    insets.bottom + dpToPx(16));
            return windowInsets;
        });

        titleText = findViewById(R.id.titleText);
        subtitleText = findViewById(R.id.subtitleText);
        errorText = findViewById(R.id.errorText);
        pinDotsContainer = findViewById(R.id.pinDotsContainer);
        keypadGrid = findViewById(R.id.keypadGrid);
        biometricButton = findViewById(R.id.biometricButton);

        createPinDots();
        setupKeypad();
        updatePinDots();
    }

    private void createPinDots() {
        int dotSize = dpToPx(16);
        int dotMargin = dpToPx(12);
        for (int i = 0; i < PIN_LENGTH; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dotSize, dotSize);
            params.setMargins(dotMargin, 0, dotMargin, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.pin_dot_empty);
            pinDotsContainer.addView(dot);
            pinDots[i] = dot;
        }
    }

    private void setupKeypad() {
        float density = getResources().getDisplayMetrics().density;
        int screenWidthDp = (int) (getResources().getDisplayMetrics().widthPixels / density);
        // 3 columns, padding (32dp * 2), margins (6dp * 6)
        int availableDp = screenWidthDp - 64 - 36;
        int buttonSizeDp = Math.min(availableDp / 3, 88);
        buttonSizeDp = Math.max(buttonSizeDp, 64);
        int buttonSizePx = dpToPx(buttonSizeDp);
        int marginPx = dpToPx(6);

        String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫"};

        for (String key : keys) {
            MaterialButton button = new MaterialButton(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = buttonSizePx;
            params.height = dpToPx(64);
            params.setMargins(marginPx, marginPx, marginPx, marginPx);
            button.setLayoutParams(params);
            button.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.app_surface)));
            button.setTextColor(getColor(R.color.app_text));
            button.setStrokeColor(ColorStateList.valueOf(getColor(R.color.app_surface_variant)));
            button.setStrokeWidth(dpToPx(1));
            button.setCornerRadius(dpToPx(20));
            button.setElevation(0);

            if (key.isEmpty()) {
                button.setVisibility(View.INVISIBLE);
            } else if (key.equals("⌫")) {
                button.setText(key);
                button.setContentDescription(getString(R.string.pin_delete_digit));
                button.setOnClickListener(v -> onBackspaceClick());
            } else {
                button.setText(key);
                button.setOnClickListener(v -> onNumberClick(key));
            }

            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            keypadGrid.addView(button);
        }
    }

    private void onNumberClick(String number) {
        if (!canInput()) return;

        if (currentPin.length() < PIN_LENGTH) {
            currentPin.append(number);
            updatePinDots();

            if (currentPin.length() == PIN_LENGTH) {
                onPinComplete(currentPin.toString());
            }
        }
    }

    private void onBackspaceClick() {
        if (!canInput()) return;

        if (currentPin.length() > 0) {
            currentPin.deleteCharAt(currentPin.length() - 1);
            updatePinDots();
            errorText.setVisibility(View.GONE);
        }
    }

    protected void updatePinDots() {
        for (int i = 0; i < PIN_LENGTH; i++) {
            pinDots[i].setBackgroundResource(i < currentPin.length() ?
                    R.drawable.pin_dot_filled : R.drawable.pin_dot_empty);
        }
    }

    protected void resetPin() {
        currentPin.setLength(0);
        updatePinDots();
    }

    protected int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    /** PIN to'liq kiritilganda chaqiriladi */
    protected abstract void onPinComplete(String pin);

    /** Input qabul qilish mumkinmi (lockout holat uchun) */
    protected boolean canInput() {
        return true;
    }
}

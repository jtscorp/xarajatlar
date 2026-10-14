package uz.jtscorp.xarajatlar.ui.auth;

import uz.jtscorp.xarajatlar.R;
import android.content.Intent;
import android.os.Bundle;

public class SetupActivity extends BasePinActivity {

    private String firstPin = null;
    private boolean isConfirmMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        titleText.setText(R.string.pin_create_title);
        subtitleText.setText(R.string.pin_create_subtitle);
    }

    @Override
    protected void onPinComplete(String pin) {
        if (!isConfirmMode) {
            // Birinchi kiritish
            firstPin = pin;
            isConfirmMode = true;
            titleText.setText(R.string.pin_confirm_title);
            subtitleText.setText(R.string.pin_confirm_subtitle);
            resetPin();
        } else {
            // Tasdiqlash
            if (pin.equals(firstPin)) {
                securePrefs.savePin(firstPin);
                securePrefs.setSetupComplete(true);

                Intent intent = new Intent(this, LockActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                errorText.setText(R.string.pin_mismatch);
                errorText.setVisibility(android.view.View.VISIBLE);
                resetPin();
            }
        }
    }
}

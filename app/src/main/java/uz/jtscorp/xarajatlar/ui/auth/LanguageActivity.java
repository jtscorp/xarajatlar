package uz.jtscorp.xarajatlar.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.radiobutton.MaterialRadioButton;

import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.utils.AppLanguage;

/** Birinchi ochilishda, PIN yaratishdan oldin til tanlash. */
public class LanguageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);

        View root = findViewById(R.id.languageRoot);
        int padding = root.getPaddingLeft();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(padding, bars.top + padding, padding, bars.bottom + padding);
            return windowInsets;
        });

        RadioGroup group = findViewById(R.id.languageGroup);
        String[] names = getResources().getStringArray(R.array.language_names);
        int selected = AppLanguage.indexOf(AppLanguage.current(this));
        for (int i = 0; i < names.length; i++) {
            MaterialRadioButton button = new MaterialRadioButton(this);
            button.setId(View.generateViewId());
            button.setText(names[i]);
            button.setTextSize(18);
            button.setMinHeight(getResources().getDimensionPixelSize(R.dimen.spacing_large) * 2);
            button.setTag(AppLanguage.TAGS[i]);
            button.setChecked(i == selected);
            group.addView(button, new RadioGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        // Listener holat tiklangandan keyin ulanadi, aks holda qayta yaratilganda o'zini chaqiradi.
        group.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            String tag = (String) radioGroup.findViewById(checkedId).getTag();
            if (!tag.equals(AppLanguage.current(this))) AppLanguage.set(tag);
        });

        findViewById(R.id.languageContinue).setOnClickListener(v -> {
            // Tanlov aniq saqlanadi: aks holda LockActivity keyinroq o'zbekchani o'rnatib yuboradi.
            AppLanguage.set(AppLanguage.current(this));
            startActivity(new Intent(this, SetupActivity.class));
            finish();
        });
    }
}

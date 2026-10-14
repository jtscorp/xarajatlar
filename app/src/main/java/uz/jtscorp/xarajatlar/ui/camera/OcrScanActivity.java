package uz.jtscorp.xarajatlar.ui.camera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;

import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uz.jtscorp.xarajatlar.R;

@ExperimentalGetImage
public class OcrScanActivity extends AppCompatActivity {

    private static final String TAG = "OcrScanActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final double MIN_AMOUNT = 100.0;
    private static final Pattern NUMBER_PATTERN =
            Pattern.compile("(\\d[\\d\\s]*[.,]?\\d*)");

    private PreviewView previewView;
    private NumberOverlayView numberOverlay;
    private View viewfinder;
    private TextView statusText;
    private ExecutorService cameraExecutor;
    private TextRecognizer textRecognizer;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr_scan);

        previewView = findViewById(R.id.previewView);
        numberOverlay = findViewById(R.id.numberOverlay);
        viewfinder = findViewById(R.id.viewfinder);
        statusText = findViewById(R.id.statusText);

        // Cancel button with status bar inset
        View cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> finish());
        ViewCompat.setOnApplyWindowInsetsListener(cancelButton, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ((ConstraintLayout.LayoutParams) v.getLayoutParams())
                    .topMargin = insets.top + Math.round(8 * getResources().getDisplayMetrics().density);
            v.requestLayout();
            return windowInsets;
        });

        // Number selection callback
        numberOverlay.setOnNumberSelectedListener(amount -> {
            if (getCallingActivity() != null) {
                // Launched with startActivityForResult (from AddExpenseActivity)
                Intent result = new Intent();
                result.putExtra("amount", amount);
                setResult(RESULT_OK, result);
            } else {
                // Launched with startActivity (from MainActivity FAB)
                Intent intent = new Intent(this,
                        uz.jtscorp.xarajatlar.ui.expense.ManualExpenseActivity.class);
                intent.putExtra("amount", amount);
                startActivity(intent);
            }
            finish();
        });

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Set viewfinder bounds for dimming after layout
        viewfinder.post(() -> {
            Rect vfRect = new Rect(
                    viewfinder.getLeft(), viewfinder.getTop(),
                    viewfinder.getRight(), viewfinder.getBottom());
            numberOverlay.setViewfinderRect(vfRect);
        });

        if (checkCameraPermission()) {
            startCamera();
        } else {
            explainCameraPermission();
        }
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                REQUEST_CAMERA_PERMISSION);
    }

    private void explainCameraPermission() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.camera_permission_title)
                .setMessage(R.string.ocr_camera_permission_message)
                .setPositiveButton(R.string.allow_camera, (dialog, which) -> requestCameraPermission())
                .setNegativeButton(R.string.enter_manually, (dialog, which) -> openManualEntry())
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    private void openManualEntry() {
        if (getCallingActivity() == null) startActivity(new Intent(this,
                uz.jtscorp.xarajatlar.ui.expense.ManualExpenseActivity.class));
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.camera_unavailable)
                        .setMessage(R.string.camera_denied_message)
                        .setPositiveButton(R.string.enter_manually, (dialog, which) -> openManualEntry())
                        .setNegativeButton(R.string.close, (dialog, which) -> finish())
                        .show();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e(TAG, "Camera bind failed", e);
                Toast.makeText(this, R.string.camera_failed, Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyzeImage(ImageProxy imageProxy) {
        if (isProcessing.get()) {
            imageProxy.close();
            return;
        }

        var mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        isProcessing.set(true);

        int rotation = imageProxy.getImageInfo().getRotationDegrees();
        InputImage inputImage = InputImage.fromMediaImage(mediaImage, rotation);

        textRecognizer.process(inputImage)
                .addOnSuccessListener(text -> {
                    List<NumberOverlayView.DetectedNumber> numbers =
                            extractNumbers(text, imageProxy.getWidth(), imageProxy.getHeight(), rotation);

                    runOnUiThreadSafe(() -> {
                        numberOverlay.setDetectedNumbers(numbers);
                        if (numbers.isEmpty()) {
                            statusText.setText(R.string.ocr_status_scanning);
                        } else {
                            statusText.setText(R.string.ocr_status_tap);
                        }
                    });
                })
                .addOnFailureListener(e -> Log.e(TAG, "Text recognition failed", e))
                .addOnCompleteListener(task -> {
                    imageProxy.close();
                    isProcessing.set(false);
                });
    }

    private List<NumberOverlayView.DetectedNumber> extractNumbers(
            Text text, int imageWidth, int imageHeight, int rotation) {

        List<NumberOverlayView.DetectedNumber> result = new ArrayList<>();

        // Get viewfinder bounds in screen coordinates
        if (viewfinder.getWidth() == 0 || viewfinder.getHeight() == 0) return result;
        if (previewView.getWidth() == 0 || previewView.getHeight() == 0) return result;

        int vfLeft = viewfinder.getLeft();
        int vfTop = viewfinder.getTop();
        int vfRight = viewfinder.getRight();
        int vfBottom = viewfinder.getBottom();

        // Account for rotation: swap image dimensions if rotated 90/270
        int effectiveWidth = (rotation == 90 || rotation == 270) ? imageHeight : imageWidth;
        int effectiveHeight = (rotation == 90 || rotation == 270) ? imageWidth : imageHeight;

        // Scale factors: image coordinates -> preview coordinates
        float previewW = previewView.getWidth();
        float previewH = previewView.getHeight();

        // PreviewView uses FILL scaling by default
        float scaleX = previewW / effectiveWidth;
        float scaleY = previewH / effectiveHeight;
        float scale = Math.max(scaleX, scaleY);

        float offsetX = (previewW - effectiveWidth * scale) / 2f;
        float offsetY = (previewH - effectiveHeight * scale) / 2f;

        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                for (Text.Element element : line.getElements()) {
                    String elementText = element.getText();
                    Matcher matcher = NUMBER_PATTERN.matcher(elementText);
                    if (matcher.find()) {
                        String numStr = matcher.group(1);
                        if (numStr == null) continue;
                        numStr = numStr.replaceAll("\\s", "").replace(",", ".");
                        try {
                            double value = Double.parseDouble(numStr);
                            if (value < MIN_AMOUNT) continue;

                            Rect bbox = element.getBoundingBox();
                            if (bbox == null) continue;

                            // Transform to screen coordinates
                            int screenLeft = (int) (bbox.left * scale + offsetX);
                            int screenTop = (int) (bbox.top * scale + offsetY);
                            int screenRight = (int) (bbox.right * scale + offsetX);
                            int screenBottom = (int) (bbox.bottom * scale + offsetY);

                            // Check if within viewfinder bounds
                            if (screenLeft >= vfLeft && screenRight <= vfRight
                                    && screenTop >= vfTop && screenBottom <= vfBottom) {

                                Rect screenBounds = new Rect(
                                        screenLeft, screenTop, screenRight, screenBottom);
                                String displayText = formatNumber(value);
                                result.add(new NumberOverlayView.DetectedNumber(
                                        value, displayText, screenBounds));
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
        return result;
    }

    private String formatNumber(double value) {
        return uz.jtscorp.xarajatlar.utils.Formats.number(value);
    }

    private void runOnUiThreadSafe(Runnable action) {
        if (!isFinishing() && !isDestroyed()) {
            runOnUiThread(action);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        textRecognizer.close();
    }
}

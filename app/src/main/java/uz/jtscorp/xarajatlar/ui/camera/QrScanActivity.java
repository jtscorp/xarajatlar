package uz.jtscorp.xarajatlar.ui.camera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.ui.expense.ReceiptConfirmActivity;
import uz.jtscorp.xarajatlar.ui.expense.ManualExpenseActivity;
import uz.jtscorp.xarajatlar.utils.ReceiptUrls;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@androidx.camera.core.ExperimentalGetImage
public class QrScanActivity extends AppCompatActivity {

    private static final String TAG = "QrScanActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private PreviewView previewView;
    private ProgressBar progressBar;
    private TextView statusText;
    private View scanOverlay;
    private ExecutorService cameraExecutor;
    private final Handler receiptHandler = new Handler(Looper.getMainLooper());
    private WebView receiptWebView;
    private boolean receiptHandled;
    private long receiptStarted;
    private long receiptDeadline;
    private BarcodeScanner barcodeScanner;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scan);

        previewView = findViewById(R.id.previewView);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
        scanOverlay = findViewById(R.id.scanOverlay);

        // Edge-to-edge: cancel button ga status bar inset qo'shish
        View cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> finish());
        ViewCompat.setOnApplyWindowInsetsListener(cancelButton, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, 0);
            ((androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) v.getLayoutParams())
                    .topMargin = insets.top + dpToPx(8);
            v.requestLayout();
            return windowInsets;
        });

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // Setup barcode scanner
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        cameraExecutor = Executors.newSingleThreadExecutor();

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
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.camera_permission_title)
                .setMessage(R.string.qr_camera_permission_message)
                .setPositiveButton(R.string.allow_camera, (dialog, which) -> requestCameraPermission())
                .setNegativeButton(R.string.enter_manually, (dialog, which) -> openManualEntry())
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    private void openManualEntry() {
        if (getCallingActivity() == null) startActivity(new Intent(this, ManualExpenseActivity.class));
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
                new MaterialAlertDialogBuilder(this)
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

                Log.d(TAG, "Camera started successfully");

            } catch (Exception e) {
                Log.e(TAG, "Camera bind failed", e);
                Toast.makeText(this, getString(R.string.camera_failed_detail, e.getMessage()), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyzeImage(ImageProxy imageProxy) {
        if (isProcessing.get()) {
            imageProxy.close();
            return;
        }

        var mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage,
                    imageProxy.getImageInfo().getRotationDegrees());

            barcodeScanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            String rawValue = barcode.getRawValue();
                            Log.d(TAG, "QR kod topildi: " + rawValue);

                            String receiptUrl = ReceiptUrls.normalizeOrNull(rawValue);
                            if (receiptUrl != null) {
                                if (isProcessing.compareAndSet(false, true)) {
                                    vibrateSuccess();
                                    processReceiptUrl(receiptUrl);
                                }
                                return;
                            } else if (rawValue != null) {
                                Log.d(TAG, "QR kod soliq.uz emas: " + rawValue);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Barcode scanning failed", e);
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
        } else {
            imageProxy.close();
        }
    }

    private void vibrateSuccess() {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    private void processReceiptUrl(String url) {
        runOnUiThreadSafe(() -> {
            progressBar.setVisibility(View.VISIBLE);
            statusText.setText(R.string.receipt_loading);
            receiptStarted = android.os.SystemClock.uptimeMillis();
            receiptDeadline = receiptStarted + 20000;
            receiptWebView = new WebView(this);
            receiptWebView.setAlpha(0);
            receiptWebView.setFocusable(false);
            receiptWebView.getSettings().setJavaScriptEnabled(true);
            receiptWebView.getSettings().setDomStorageEnabled(true);
            receiptWebView.getSettings().setBlockNetworkImage(true);
            receiptWebView.getSettings().setAllowFileAccess(false);
            receiptWebView.getSettings().setAllowContentAccess(false);
            receiptWebView.getSettings().setMixedContentMode(
                    android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
            receiptWebView.setWebViewClient(new WebViewClient() {
                @Override public void onPageStarted(WebView view, String pageUrl,
                                                    android.graphics.Bitmap favicon) {
                    // Read the response to the page's own signed request; no signature is copied.
                    view.evaluateJavascript("(function(){var original=window.fetch;window.fetch=function(input,options){return original.apply(this,arguments).then(function(response){if(response.ok&&String(input)==='https://new-ofd.soliq.uz/api/payment'){response.clone().text().then(function(body){window.__xarajatlarPaymentResponse=body})}return response})}})()", null);
                }

                @Override public boolean shouldOverrideUrlLoading(WebView view,
                                                                    WebResourceRequest request) {
                    return ReceiptUrls.normalizeOrNull(request.getUrl().toString()) == null;
                }

                @Override public void onReceivedError(WebView view, WebResourceRequest request,
                                                       android.webkit.WebResourceError error) {
                    if (request.isForMainFrame()) failReceipt(url);
                }
            });
            addContentView(receiptWebView, new ViewGroup.LayoutParams(1, 1));
            receiptWebView.loadUrl(url);
            receiptHandler.postDelayed(() -> pollReceiptPage(url), 400);
        });
    }

    private void pollReceiptPage(String url) {
        if (receiptHandled || isFinishing() || isDestroyed() || receiptWebView == null) return;
        if (android.os.SystemClock.uptimeMillis() >= receiptDeadline) {
            failReceipt(url);
            return;
        }
        receiptWebView.evaluateJavascript(
                "JSON.stringify({api:window.__xarajatlarPaymentResponse||'',text:document.body.innerText||''})",
                json -> {
            if (receiptHandled || isFinishing() || isDestroyed()) return;
            try {
                String snapshot = new JSONArray("[" + json + "]").getString(0);
                JSONObject fields = new JSONObject(snapshot);
                String pageText = fields.optString("text", "");
                String apiResponse = fields.optString("api", "");
                ReceiptTextParser.Result page = ReceiptTextParser.parse(pageText);
                double apiAmount = OfdPaymentParser.total(apiResponse);
                long elapsed = android.os.SystemClock.uptimeMillis() - receiptStarted;
                if (apiAmount > 0 && (!page.shopName.isEmpty() || elapsed >= 4000)) {
                    receiptHandled = true;
                    Log.d(TAG, "Receipt amount read from signed OFD response");
                    openReceiptConfirm(apiAmount, page.shopName, url,
                            OfdPaymentParser.itemsJson(apiResponse));
                    return;
                }
                if (page.amount > 0 && elapsed >= 5000) {
                    receiptHandled = true;
                    Log.d(TAG, "Receipt amount read from rendered page");
                    openReceiptConfirm(page.amount, page.shopName, url, null);
                    return;
                }
            } catch (JSONException error) {
                Log.w(TAG, "Receipt page text is not ready", error);
            }
            receiptHandler.postDelayed(() -> pollReceiptPage(url), 400);
        });
    }

    private void failReceipt(String url) {
        if (receiptHandled || isFinishing() || isDestroyed()) return;
        receiptHandled = true;
        progressBar.setVisibility(View.GONE);
        Toast.makeText(this, R.string.receipt_load_failed, Toast.LENGTH_LONG).show();
        openReceiptConfirm(0, "", url, null);
    }

    private void openReceiptConfirm(double amount, String shopName, String receiptUrl,
                                    String receiptItemsJson) {
        Intent intent = new Intent(this, ReceiptConfirmActivity.class);
        intent.putExtra(ReceiptConfirmActivity.EXTRA_AMOUNT, amount);
        intent.putExtra(ReceiptConfirmActivity.EXTRA_STORE_NAME, shopName);
        intent.putExtra(ReceiptConfirmActivity.EXTRA_DATE, System.currentTimeMillis());
        intent.putExtra(ReceiptConfirmActivity.EXTRA_RECEIPT_URL, receiptUrl);
        intent.putExtra(ReceiptConfirmActivity.EXTRA_RECEIPT_ITEMS, receiptItemsJson);
        startActivity(intent);
        finish();
    }

    /** Lifecycle-safe runOnUiThread — isFinishing bo'lsa chaqirmaydi */
    private void runOnUiThreadSafe(Runnable action) {
        if (!isFinishing() && !isDestroyed()) {
            runOnUiThread(action);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        receiptHandler.removeCallbacksAndMessages(null);
        if (receiptWebView != null) {
            receiptWebView.stopLoading();
            receiptWebView.destroy();
        }
        cameraExecutor.shutdown();
        barcodeScanner.close();
    }
}

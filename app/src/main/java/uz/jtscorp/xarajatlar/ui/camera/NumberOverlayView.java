package uz.jtscorp.xarajatlar.ui.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class NumberOverlayView extends View {

    public static class DetectedNumber {
        public final double value;
        public final String displayText;
        public final Rect screenBounds;

        public DetectedNumber(double value, String displayText, Rect screenBounds) {
            this.value = value;
            this.displayText = displayText;
            this.screenBounds = screenBounds;
        }
    }

    public interface OnNumberSelectedListener {
        void onNumberSelected(double amount);
    }

    private List<DetectedNumber> detectedNumbers = new ArrayList<>();
    private OnNumberSelectedListener listener;
    private final Paint boxPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint bgPaint = new Paint();
    private final float cornerRadius;
    private Rect viewfinderRect;
    private final Paint dimmingPaint = new Paint();

    public NumberOverlayView(Context context) {
        this(context, null);
    }

    public NumberOverlayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NumberOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        float density = context.getResources().getDisplayMetrics().density;
        cornerRadius = 8 * density;

        boxPaint.setColor(Color.parseColor("#4CAF50"));
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(3 * density);
        boxPaint.setAntiAlias(true);

        bgPaint.setColor(Color.parseColor("#664CAF50"));
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setAntiAlias(true);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(14 * density);
        textPaint.setAntiAlias(true);
        textPaint.setFakeBoldText(true);
        textPaint.setShadowLayer(2 * density, 1, 1, Color.BLACK);

        dimmingPaint.setColor(Color.parseColor("#80000000"));
        dimmingPaint.setStyle(Paint.Style.FILL);
    }

    public void setOnNumberSelectedListener(OnNumberSelectedListener listener) {
        this.listener = listener;
    }

    public void setDetectedNumbers(List<DetectedNumber> numbers) {
        this.detectedNumbers = numbers != null ? numbers : new ArrayList<>();
        invalidate();
    }

    public void setViewfinderRect(Rect rect) {
        this.viewfinderRect = rect;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw dimming outside viewfinder
        if (viewfinderRect != null) {
            int w = getWidth();
            int h = getHeight();
            canvas.drawRect(0, 0, w, viewfinderRect.top, dimmingPaint);
            canvas.drawRect(0, viewfinderRect.bottom, w, h, dimmingPaint);
            canvas.drawRect(0, viewfinderRect.top, viewfinderRect.left, viewfinderRect.bottom, dimmingPaint);
            canvas.drawRect(viewfinderRect.right, viewfinderRect.top, w, viewfinderRect.bottom, dimmingPaint);
        }

        for (DetectedNumber number : detectedNumbers) {
            RectF rect = new RectF(number.screenBounds);
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint);
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, boxPaint);
            float textWidth = textPaint.measureText(number.displayText);
            float textX = rect.centerX() - textWidth / 2;
            float textY = rect.top - 8 * getResources().getDisplayMetrics().density;
            if (textY < textPaint.getTextSize()) {
                textY = rect.bottom + textPaint.getTextSize() + 4 * getResources().getDisplayMetrics().density;
            }
            canvas.drawText(number.displayText, textX, textY, textPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && listener != null) {
            float x = event.getX();
            float y = event.getY();
            float padding = 20 * getResources().getDisplayMetrics().density;
            for (DetectedNumber number : detectedNumbers) {
                Rect bounds = number.screenBounds;
                if (x >= bounds.left - padding && x <= bounds.right + padding
                        && y >= bounds.top - padding && y <= bounds.bottom + padding) {
                    listener.onNumberSelected(number.value);
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }
}

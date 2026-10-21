package com.example.myapp;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private final float[] rotationMatrix = new float[9];
    private final float[] orientation = new float[3];
    private final float[] gravity = new float[3];
    private final float[] magnetic = new float[3];

    private boolean hasGravity = false;
    private boolean hasMagnetic = false;

    private TextView degreeText;
    private TextView directionText;
    private TextView statusText;
    private CompassView compassView;

    private float heading = 0f;
    private boolean sensorAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sensorManager = (SensorManager) getSystemService(
                Context.SENSOR_SERVICE
        );

        rotationSensor = sensorManager.getDefaultSensor(
                Sensor.TYPE_ROTATION_VECTOR
        );

        accelerometer = sensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER
        );

        magnetometer = sensorManager.getDefaultSensor(
                Sensor.TYPE_MAGNETIC_FIELD
        );

        createInterface();
    }

    private void createInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(24, 35, 24, 25);
        root.setBackgroundColor(Color.rgb(12, 17, 30));

        TextView title = new TextView(this);
        title.setText("COMPASS");
        title.setTextSize(30);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);

        TextView subtitle = new TextView(this);
        subtitle.setText("البوصلة الرقمية");
        subtitle.setTextSize(17);
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setGravity(Gravity.CENTER);

        degreeText = new TextView(this);
        degreeText.setText("0°");
        degreeText.setTextSize(38);
        degreeText.setTextColor(Color.WHITE);
        degreeText.setTypeface(Typeface.DEFAULT_BOLD);
        degreeText.setGravity(Gravity.CENTER);

        directionText = new TextView(this);
        directionText.setText("شمال");
        directionText.setTextSize(22);
        directionText.setTextColor(Color.rgb(255, 75, 75));
        directionText.setGravity(Gravity.CENTER);

        compassView = new CompassView(this);

        statusText = new TextView(this);
        statusText.setTextSize(14);
        statusText.setTextColor(Color.LTGRAY);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 15, 0, 0);

        root.addView(title);
        root.addView(subtitle);

        LinearLayout.LayoutParams degreeParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        degreeParams.topMargin = 18;
        root.addView(degreeText, degreeParams);

        root.addView(directionText);

        LinearLayout.LayoutParams compassParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                );
        compassParams.topMargin = 10;
        compassParams.bottomMargin = 10;

        root.addView(compassView, compassParams);
        root.addView(statusText);

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sensorManager == null) {
            statusText.setText("تعذر الوصول إلى مستشعرات الهاتف");
            return;
        }

        sensorAvailable = false;
        hasGravity = false;
        hasMagnetic = false;

        if (rotationSensor != null) {
            sensorManager.registerListener(
                    this,
                    rotationSensor,
                    SensorManager.SENSOR_DELAY_UI
            );
            sensorAvailable = true;

        } else {
            if (accelerometer != null) {
                sensorManager.registerListener(
                        this,
                        accelerometer,
                        SensorManager.SENSOR_DELAY_UI
                );
                sensorAvailable = true;
            }

            if (magnetometer != null) {
                sensorManager.registerListener(
                        this,
                        magnetometer,
                        SensorManager.SENSOR_DELAY_UI
                );
                sensorAvailable = true;
            }
        }

        if (!sensorAvailable) {
            statusText.setText("مستشعر البوصلة غير متوفر على هذا الجهاز");
        } else {
            statusText.setText("حرّك الهاتف بشكل رقم 8 لمعايرة البوصلة");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {

            SensorManager.getRotationMatrixFromVector(
                    rotationMatrix,
                    event.values
            );

            SensorManager.getOrientation(
                    rotationMatrix,
                    orientation
            );

            updateHeading((float) Math.toDegrees(orientation[0]));

        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            System.arraycopy(
                    event.values, 0, gravity, 0, gravity.length
            );

            hasGravity = true;
            updateFallbackCompass();

        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {

            System.arraycopy(
                    event.values, 0, magnetic, 0, magnetic.length
            );

            hasMagnetic = true;
            updateFallbackCompass();
        }
    }

    private void updateFallbackCompass() {
        if (!hasGravity || !hasMagnetic) {
            return;
        }

        boolean success = SensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                gravity,
                magnetic
        );

        if (success) {
            SensorManager.getOrientation(
                    rotationMatrix,
                    orientation
            );

            updateHeading(
                    (float) Math.toDegrees(orientation[0])
            );
        }
    }

    private void updateHeading(float degrees) {
        heading = (degrees + 360f) % 360f;

        String direction;

        if (heading >= 337.5f || heading < 22.5f) {
            direction = "شمال";
        } else if (heading < 67.5f) {
            direction = "شمال شرقي";
        } else if (heading < 112.5f) {
            direction = "شرق";
        } else if (heading < 157.5f) {
            direction = "جنوب شرقي";
        } else if (heading < 202.5f) {
            direction = "جنوب";
        } else if (heading < 247.5f) {
            direction = "جنوب غربي";
        } else if (heading < 292.5f) {
            direction = "غرب";
        } else {
            direction = "شمال غربي";
        }

        degreeText.setText(
                String.format(Locale.getDefault(), "%.0f°", heading)
        );

        directionText.setText(direction);

        compassView.invalidate();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (statusText == null) {
            return;
        }

        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            statusText.setText(
                    "دقة المستشعر منخفضة؛ أبعد الهاتف عن المغناطيس"
            );
        } else {
            statusText.setText(
                    "حرّك الهاتف بشكل رقم 8 لمعايرة البوصلة"
            );
        }
    }

    private class CompassView extends View {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public CompassView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;
            float radius = Math.min(centerX, centerY) * 0.82f;

            // دائرة البوصلة
            paint.setColor(Color.rgb(28, 37, 57));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(centerX, centerY, radius, paint);

            paint.setColor(Color.rgb(85, 105, 135));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3f);
            canvas.drawCircle(centerX, centerY, radius, paint);

            // تدريجات البوصلة
            for (int i = 0; i < 360; i += 10) {
                canvas.save();
                canvas.rotate(i, centerX, centerY);

                paint.setColor(Color.LTGRAY);
                paint.setStrokeWidth(i % 30 == 0 ? 3f : 1.5f);

                float start = radius - (i % 30 == 0 ? 18 : 10);
                canvas.drawLine(
                        centerX, centerY - start,
                        centerX, centerY - radius + 5,
                        paint
                );

                canvas.restore();
            }

            // الجهات الأربع
            drawLabel(canvas, "ش", centerX, centerY - radius + 38,
                    Color.RED, 22);
            drawLabel(canvas, "ج", centerX, centerY + radius - 18,
                    Color.WHITE, 20);
            drawLabel(canvas, "غ", centerX - radius + 25, centerY + 7,
                    Color.WHITE, 20);
            drawLabel(canvas, "ق", centerX + radius - 25, centerY + 7,
                    Color.WHITE, 20);

            // السهم الأحمر باتجاه الشمال المغناطيسي
            canvas.save();
            canvas.rotate(-heading, centerX, centerY);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(255, 65, 75));

            android.graphics.Path northArrow =
                    new android.graphics.Path();

            northArrow.moveTo(centerX, centerY - radius + 52);
            northArrow.lineTo(centerX - 17, centerY);
            northArrow.lineTo(centerX, centerY - 9);
            northArrow.lineTo(centerX + 17, centerY);
            northArrow.close();

            canvas.drawPath(northArrow, paint);

            paint.setColor(Color.WHITE);

            android.graphics.Path southArrow =
                    new android.graphics.Path();

            southArrow.moveTo(centerX, centerY + radius - 52);
            southArrow.lineTo(centerX - 17, centerY);
            southArrow.lineTo(centerX, centerY + 9);
            southArrow.lineTo(centerX + 17, centerY);
            southArrow.close();

            canvas.drawPath(southArrow, paint);

            canvas.restore();

            // مركز البوصلة
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(centerX, centerY, 7, paint);
        }

        private void drawLabel(
                Canvas canvas,
                String text,
                float x,
                float y,
                int color,
                float size
        ) {
            paint.setColor(color);
            paint.setTextSize(size);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setStyle(Paint.Style.FILL);

            canvas.drawText(text, x, y, paint);
        }
    }
}
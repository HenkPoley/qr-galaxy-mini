package com.example.qrgalaxy;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.io.IOException;

public class MainActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private SurfaceView preview;
    private TextView resultView;
    private Button openButton;
    private Camera camera;
    private QRCodeReader reader;
    private Handler handler;
    private boolean decoding;
    private boolean previewing;
    private String lastResult;
    private int cameraId = -1;
    private int displayOrientation = 90;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reader = new QRCodeReader();
        handler = new Handler();
        buildLayout();
    }

    private void buildLayout() {
        FrameLayout root = new FrameLayout(this);

        preview = new SurfaceView(this);
        preview.getHolder().addCallback(this);
        root.addView(preview, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        TextView guide = new TextView(this);
        guide.setText("Point the camera at a QR code");
        guide.setTextColor(Color.WHITE);
        guide.setTextSize(18);
        guide.setGravity(Gravity.CENTER);
        guide.setBackgroundColor(0x66000000);
        FrameLayout.LayoutParams guideParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP);
        root.addView(guide, guideParams);

        FrameLayout panel = new FrameLayout(this);
        panel.setBackgroundColor(0xcc000000);
        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(104),
                Gravity.BOTTOM);
        root.addView(panel, panelParams);

        resultView = new TextView(this);
        resultView.setText("No QR code found yet");
        resultView.setTextColor(Color.WHITE);
        resultView.setTextSize(16);
        resultView.setPadding(dp(10), dp(8), dp(10), 0);
        resultView.setSingleLine(false);
        panel.addView(resultView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(56),
                Gravity.TOP));

        openButton = new Button(this);
        openButton.setText("Open");
        openButton.setEnabled(false);
        openButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLastResult();
            }
        });
        FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44),
                Gravity.BOTTOM);
        buttonParams.leftMargin = dp(8);
        buttonParams.rightMargin = dp(8);
        panel.addView(openButton, buttonParams);

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        openCamera();
    }

    @Override
    protected void onPause() {
        releaseCamera();
        super.onPause();
    }

    private void openCamera() {
        if (camera != null) {
            return;
        }
        try {
            cameraId = findBackCameraId();
            camera = cameraId >= 0 ? Camera.open(cameraId) : Camera.open();
            displayOrientation = getCameraDisplayOrientation(cameraId);
            camera.setDisplayOrientation(displayOrientation);
            Camera.Parameters params = camera.getParameters();
            if (params.getSupportedFocusModes() != null
                    && params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            camera.setParameters(params);
            camera.setPreviewCallback(this);
            if (preview.getHolder().getSurface() != null) {
                startPreview(preview.getHolder());
            }
        } catch (RuntimeException ex) {
            Toast.makeText(this, "Could not open camera", Toast.LENGTH_LONG).show();
        }
    }

    private void startPreview(SurfaceHolder holder) {
        if (camera == null || previewing) {
            return;
        }
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
            previewing = true;
            requestAutoFocus();
        } catch (IOException ex) {
            Toast.makeText(this, "Could not start camera preview", Toast.LENGTH_LONG).show();
        }
    }

    private int findBackCameraId() {
        int count = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < count; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return i;
            }
        }
        return count > 0 ? 0 : -1;
    }

    private int getCameraDisplayOrientation(int id) {
        if (id < 0) {
            return 90;
        }
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(id, info);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            case Surface.ROTATION_0:
            default:
                degrees = 0;
                break;
        }
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (360 - ((info.orientation + degrees) % 360)) % 360;
        }
        return (info.orientation - degrees + 360) % 360;
    }

    private void requestAutoFocus() {
        if (camera == null) {
            return;
        }
        try {
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            requestAutoFocus();
                        }
                    }, 1800);
                }
            });
        } catch (RuntimeException ignored) {
            // Some Gingerbread camera drivers report autofocus but throw at runtime.
        }
    }

    private void releaseCamera() {
        handler.removeCallbacksAndMessages(null);
        if (camera != null) {
            try {
                camera.setPreviewCallback(null);
                camera.stopPreview();
            } catch (RuntimeException ignored) {
            }
            camera.release();
            camera = null;
        }
        previewing = false;
        decoding = false;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startPreview(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (camera == null) {
            return;
        }
        try {
            camera.stopPreview();
        } catch (RuntimeException ignored) {
        }
        previewing = false;
        startPreview(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCamera();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (decoding || data == null) {
            return;
        }
        decoding = true;
        try {
            Camera.Size size = camera.getParameters().getPreviewSize();
            Result result = decode(data, size.width, size.height);
            if (result != null) {
                lastResult = result.getText();
                resultView.setText(lastResult);
                openButton.setEnabled(true);
            }
        } finally {
            decoding = false;
        }
    }

    private Result decode(byte[] data, int width, int height) {
        RotatedFrame frame = rotate(data, width, height, displayOrientation);
        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                frame.data, frame.width, frame.height, 0, 0, frame.width, frame.height, false);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            return reader.decode(bitmap);
        } catch (NotFoundException ex) {
            return null;
        } catch (ReaderException ex) {
            return null;
        } finally {
            reader.reset();
        }
    }

    private RotatedFrame rotate(byte[] data, int width, int height, int degrees) {
        if (degrees == 0) {
            return new RotatedFrame(data, width, height);
        }
        byte[] rotated = new byte[data.length];
        if (degrees == 180) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    rotated[(height - y - 1) * width + width - x - 1] = data[y * width + x];
                }
            }
            return new RotatedFrame(rotated, width, height);
        }
        if (degrees == 270) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    rotated[(width - x - 1) * height + y] = data[y * width + x];
                }
            }
            return new RotatedFrame(rotated, height, width);
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                rotated[x * height + height - y - 1] = data[x + y * width];
            }
        }
        return new RotatedFrame(rotated, height, width);
    }

    private static class RotatedFrame {
        final byte[] data;
        final int width;
        final int height;

        RotatedFrame(byte[] data, int width, int height) {
            this.data = data;
            this.width = width;
            this.height = height;
        }
    }

    private void openLastResult() {
        if (lastResult == null || lastResult.length() == 0) {
            return;
        }
        String uriText = lastResult;
        if (uriText.indexOf("://") < 0 && uriText.indexOf('.') > 0) {
            uriText = "http://" + uriText;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uriText)));
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, lastResult, Toast.LENGTH_LONG).show();
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}

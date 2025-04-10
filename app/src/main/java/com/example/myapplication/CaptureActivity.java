package com.example.myapplication;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@androidx.camera.core.ExperimentalGetImage
public class CaptureActivity extends AppCompatActivity {
    private PreviewView previewView;
    private Button captureButton;
    private ImageView capturedImageView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        previewView = findViewById(R.id.previewView);
        captureButton = findViewById(R.id.captureButton);
        capturedImageView = findViewById(R.id.capturedImageView);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
        } else {
            startCamera();
        }

        captureButton.setOnClickListener(v -> takePhoto());
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission is needed", Toast.LENGTH_SHORT).show();
        }
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        imageCapture = new ImageCapture.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                Bitmap bitmap = imageProxyToBitmap(image);
                runOnUiThread(() -> capturedImageView.setImageBitmap(bitmap));
                insertImageToDatabase(bitmap);
                analyzeImage(bitmap);
                image.close();
            }
        });
    }

    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        Image image = imageProxy.getImage();
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private void insertImageToDatabase(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        byte[] imageBytes = stream.toByteArray();

        String username = getSharedPreferences("MyPrefs", MODE_PRIVATE).getString("username", "default_user");
        ImageDatabaseHelper dbHelper = ImageDatabaseHelper.getInstance(this, username);

        dbHelper.insertImage(imageBytes);
    }

    private void analyzeImage(Bitmap bitmap) {
        ProgressDialog progressDialog = new ProgressDialog(CaptureActivity.this);
        progressDialog.setMessage("Analyzing image...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        String base64Image = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);

        OkHttpClient client = new OkHttpClient();

        JSONObject payload = new JSONObject();
        try {
            JSONArray parts = new JSONArray();
            parts.put(new JSONObject().put("text", "Analyze this skin image for acne, wrinkles, or other skin conditions. Be specific."));
            parts.put(new JSONObject().put("inlineData", new JSONObject()
                    .put("mimeType", "image/jpeg")
                    .put("data", base64Image)));

            JSONArray contents = new JSONArray();
            contents.put(new JSONObject().put("parts", parts));

            payload.put("contents", contents);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key=AIzaSyBsZh3gwYNbDIHkvc-8uTq03G5KamMu7bA ")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(CaptureActivity.this, "Failed to analyze image", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String res = response.body().string();
                    Log.d("Gemini", "Response: " + res);

                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        try {
                            JSONObject json = new JSONObject(res);
                            JSONArray candidates = json.getJSONArray("candidates");
                            JSONObject firstCandidate = candidates.getJSONObject(0);
                            JSONObject content = firstCandidate.getJSONObject("content");
                            JSONArray parts = content.getJSONArray("parts");
                            String result = parts.getJSONObject(0).getString("text");

                            new AlertDialog.Builder(CaptureActivity.this)
                                    .setTitle("Skin Analysis Result")
                                    .setMessage(result)
                                    .setPositiveButton("View Recommendations", (dialog, which) -> {
                                        Intent intent = new Intent(CaptureActivity.this, RecommendationActivity.class);
                                        intent.putExtra("analysis_result", result);
                                        startActivity(intent);
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(CaptureActivity.this, "Error analyzing image", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
}
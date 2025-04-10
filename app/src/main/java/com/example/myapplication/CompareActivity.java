package com.example.myapplication;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CompareActivity extends AppCompatActivity {

    ImageView lastImageView, currentImageView;
    TextView compareResultTextView;
    Button btnAnalyzeComparison;

    byte[] lastImageBytes, currentImageBytes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.compare_activity);

        lastImageView = findViewById(R.id.lastImageView);
        currentImageView = findViewById(R.id.currentImageView);
        compareResultTextView = findViewById(R.id.compareResultTextView);
        btnAnalyzeComparison = findViewById(R.id.btnAnalyzeComparison);

        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String username = prefs.getString("username", "default_user");
        ImageDatabaseHelper dbHelper = ImageDatabaseHelper.getInstance(this, username);

        lastImageBytes = dbHelper.getSecondLastImage();
        currentImageBytes = dbHelper.getLastImage();

        if (lastImageBytes != null) {
            Bitmap lastBitmap = BitmapFactory.decodeByteArray(lastImageBytes, 0, lastImageBytes.length);
            lastImageView.setImageBitmap(lastBitmap);
        }

        if (currentImageBytes != null) {
            Bitmap currentBitmap = BitmapFactory.decodeByteArray(currentImageBytes, 0, currentImageBytes.length);
            currentImageView.setImageBitmap(currentBitmap);
        }

        btnAnalyzeComparison.setOnClickListener(v -> analyzeImages(lastImageBytes, currentImageBytes));

        FloatingActionButton btnChatBot = findViewById(R.id.btnChatBot);

        btnChatBot.setOnClickListener(v -> showChatBotDialog());
    }
    //FloatingActionButton btnChatBot = findViewById(R.id.btnChatBot);

    // btnChatBot.setOnClickListener(v -> showChatBotDialog());
    private void showChatBotDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ask SkinBot");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        EditText userInput = new EditText(this);
        userInput.setHint("Ask about skincare, acne, etc.");
        layout.addView(userInput);

        TextView responseView = new TextView(this);
        layout.addView(responseView);

        builder.setView(layout);

        builder.setPositiveButton("Ask", null);  // We’ll override this later
        builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Override the Ask button to prevent auto-close
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String question = userInput.getText().toString();
            if (!question.isEmpty()) {
                askGeminiBot(question, responseView);
            } else {
                responseView.setText("Please ask something!");
            }
        });
    }


    private void askGeminiBot(String question, TextView responseView) {
        responseView.setText("Thinking...");

        JSONArray parts = new JSONArray();
        try {
            parts.put(new JSONObject().put("text", question));
        } catch (JSONException e) {
            responseView.setText("Error creating request.");
            return;
        }

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("contents", new JSONArray().put(new JSONObject()
                    .put("role", "user")
                    .put("parts", parts)));
        } catch (JSONException e) {
            responseView.setText("Error creating request body.");
            return;
        }

        Request request = new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key=AIzaSyBsZh3gwYNbDIHkvc-8uTq03G5KamMu7bA")
                .post(RequestBody.create(
                        requestBody.toString(),
                        MediaType.parse("application/json")))
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> responseView.setText("Network error."));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> responseView.setText("API error."));
                    return;
                }

                String resultJson = response.body().string();
                try {
                    JSONObject result = new JSONObject(resultJson);
                    JSONArray candidates = result.getJSONArray("candidates");
                    JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
                    JSONArray parts = content.getJSONArray("parts");
                    String botReply = parts.getJSONObject(0).getString("text");

                    runOnUiThread(() -> responseView.setText(botReply));
                } catch (JSONException e) {
                    runOnUiThread(() -> responseView.setText("Parsing error."));
                }
            }
        });
    }


    private void analyzeImages(byte[] img1, byte[] img2) {
        compareResultTextView.setText("Analyzing...");

        String base64Image1 = Base64.encodeToString(img1, Base64.NO_WRAP);
        String base64Image2 = Base64.encodeToString(img2, Base64.NO_WRAP);

        JSONArray parts = new JSONArray();
        try {
            parts.put(new JSONObject().put("text", "Compare the two skin images provided. Determine if there is any improvement, worsening, or no significant change in skin condition such as acne, dryness, or glow. Give a brief explanation."));

            parts.put(new JSONObject().put("inline_data", new JSONObject()
                    .put("mime_type", "image/jpeg")
                    .put("data", base64Image1)));

            parts.put(new JSONObject().put("inline_data", new JSONObject()
                    .put("mime_type", "image/jpeg")
                    .put("data", base64Image2)));
        } catch (JSONException e) {
            compareResultTextView.setText("Error building JSON.");
            return;
        }

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("contents", new JSONArray().put(new JSONObject()
                    .put("role", "user")
                    .put("parts", parts)));
        } catch (JSONException e) {
            compareResultTextView.setText("Error building request body.");
            return;
        }

        Request request = new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key=AIzaSyBsZh3gwYNbDIHkvc-8uTq03G5KamMu7bA ")
                .post(RequestBody.create(
                        requestBody.toString(),
                        MediaType.parse("application/json")))
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> compareResultTextView.setText("Network error"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> compareResultTextView.setText("API Error"));
                    return;
                }

                String resultJson = response.body().string();
                try {
                    JSONObject result = new JSONObject(resultJson);
                    JSONArray candidates = result.getJSONArray("candidates");
                    JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
                    JSONArray parts = content.getJSONArray("parts");
                    String analysis = parts.getJSONObject(0).getString("text");

                    runOnUiThread(() -> compareResultTextView.setText(analysis));
                } catch (JSONException e) {
                    runOnUiThread(() -> compareResultTextView.setText("Parsing error"));
                }
            }
        });
    }
}
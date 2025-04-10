package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class RecommendationActivity extends AppCompatActivity {

    private TextView tvAnalysisResult, tvProductList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis_result);

        tvAnalysisResult = findViewById(R.id.tvAnalysisResult);
        tvProductList = findViewById(R.id.tvProductList);

        String analysis = getIntent().getStringExtra("analysis_result");
        tvAnalysisResult.setText(analysis);

        // Simple logic to recommend based on keywords (can be improved)
        StringBuilder recommendations = new StringBuilder();
        if (analysis.toLowerCase().contains("acne")) {
            recommendations.append("• Salicylic Acid Cleanser\n• Benzoyl Peroxide Gel\n• Tea Tree Oil\n\n");
        }
        if (analysis.toLowerCase().contains("wrinkle")) {
            recommendations.append("• Retinol Cream\n• Hyaluronic Acid Serum\n• Vitamin C Moisturizer\n\n");
        }
        if (analysis.toLowerCase().contains("dry")) {
            recommendations.append("• Ceramide Moisturizer\n• Hydrating Sheet Mask\n• Aloe Vera Gel\n\n");
        }
        if (recommendations.length() == 0) {
            recommendations.append("• Gentle Cleanser\n• Sunscreen SPF 50+\n• Daily Moisturizer");
        }

        tvProductList.setText(recommendations.toString());
        Button btnCompareWithLast = findViewById(R.id.btnCompareWithLast);
        btnCompareWithLast.setOnClickListener(v -> {
            Intent intent = new Intent(RecommendationActivity.this, CompareActivity.class);
            startActivity(intent);
        });

    }
}
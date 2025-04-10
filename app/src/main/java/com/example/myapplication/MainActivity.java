package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    EditText emailEditText, passwordEditText;
    Button loginButton, signupButton;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        signupButton = findViewById(R.id.signupButton);

        dbHelper = new DatabaseHelper(this);

        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (dbHelper.checkUser(email, password)) {
                // Save user email in SharedPreferences
                saveLoggedInUser(email);

                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, CaptureActivity.class));
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            }
        });

        signupButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (dbHelper.insertUser(email, password)) {
                saveLoggedInUser(email);

                Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show();
                //startActivity(new Intent(this, CaptureActivity.class));
            } else {
                Toast.makeText(this, "User already exists", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveLoggedInUser(String email) {
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        prefs.edit().putString("username", email).apply();
    }
}

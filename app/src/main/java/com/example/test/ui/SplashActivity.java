package com.example.test.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.example.test.R;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1500; // 2 секунды

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Задержка перед переходом на MainActivity
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);

            // Анимация плавного перехода
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

            finish(); // чтобы SplashActivity не оставался в back stack
        }, SPLASH_DELAY);
    }
}

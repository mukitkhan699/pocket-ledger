package com.pocketledger.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.pocketledger.R;
import com.pocketledger.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = SessionManager.get(this).isLoggedIn()
                    ? new Intent(this, MainActivity.class)
                    : new Intent(this, AuthActivity.class);
            startActivity(intent);
            finish();
        }, 650L);
    }
}

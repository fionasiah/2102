package com.driving.self.sueno.sueno;

/**
 * Created by Asus on 1/11/2017.
 */

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.annotation.Nullable;

public class SplashScreen extends AppCompatActivity {
    private static int SPLASH_TIME_OUT = 1500;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent homeIntent = new Intent(SplashScreen.this, MapsActivity.class);
//                SplashScreen.this.startActivity(homeIntent);
                startActivity(homeIntent);
                finish();
            }

        },SPLASH_TIME_OUT);

    }
}
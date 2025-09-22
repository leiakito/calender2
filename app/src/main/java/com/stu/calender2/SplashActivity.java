package com.stu.calender2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 3000; // 3秒
    private static final String[] SPLASH_IMAGES = {"1.png", "2.png"}; // assets中的图片文件名
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        // 获取随机图片并应用淡入动画
        ImageView splashImage = findViewById(R.id.splash_image);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        splashImage.startAnimation(fadeIn);
        
        loadRandomImage(splashImage);
        
        // 延迟3秒后跳转到主界面
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // 应用淡出动画
            Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
            splashImage.startAnimation(fadeOut);
            
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    // 创建Intent跳转到MainActivity
                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                    startActivity(intent);
                    
                    // 不使用默认的转场动画
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    
                    // 结束当前Activity
                    finish();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
        }, SPLASH_DELAY);
    }
    
    /**
     * 加载随机图片
     */
    private void loadRandomImage(ImageView splashImage) {
        // 随机选择一张图片
        Random random = new Random();
        String randomImage = SPLASH_IMAGES[random.nextInt(SPLASH_IMAGES.length)];
        
        try {
            // 从assets加载图片
            InputStream is = getAssets().open(randomImage);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            splashImage.setImageBitmap(bitmap);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 
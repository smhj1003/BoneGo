package kr.co.jjnet.bonego;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Copyright (C) 2017 JJNET Co., Ltd
 * 모든 권리 보유.
 * Developed by JJNET Co., Ltd.
 **/

public class jjSplashActivity extends Activity{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        try {
            Thread.sleep(3000); //대기 초 설정
            startActivity(new Intent(jjSplashActivity.this, MainActivity.class));
            finish();
        } catch (Exception e) {
           Log.e("Error", "SplashActivity ERROR", e);
        }
//        setContentView(R.layout.activity_splash);



//        Handler handler = new Handler() {
//            @Override
//            public void handleMessage(Message msg) {
//                finish();
//            }
//        };
//
//        handler.sendEmptyMessageDelayed(0, 1500);
    }
}

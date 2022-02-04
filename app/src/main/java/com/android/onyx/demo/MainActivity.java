package com.android.onyx.demo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.android.onyx.demo.scribble.ScribbleCanvasActivity;
import com.onyx.android.demo.R;
import com.onyx.android.sdk.api.device.epd.EpdController;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private final int STORAGE_ACCESS_REQUEST_CODE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        final View view = findViewById(android.R.id.content);
        EpdController.enablePost(view, 1);
        //if(!Environment.isExternalStorageManager()){
        //    requestPermissions(MainActivity.this, STORAGE_ACCESS_REQUEST_CODE);
        //}
//        String path = Environment.getExternalStorageDirectory().toString();
//        Log.d("Files", "Path: " + path);
//        File directory = new File(path);
//        File[] files = directory.listFiles();
//        Log.d("Files", "Size: "+ files.length);
//        for (File file : files) {
//            Log.d("Files", "FileName:" + file.getName());
//        }
    }

    @OnClick({R.id.button_canvas_activity})
    public void button_canvas() {
        go(ScribbleCanvasActivity.class);
    }


    private void go(Class<?> activityClass){
        startActivity(new Intent(this, activityClass));
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    public static void requestPermissions(Activity activity, int requestCode) {

                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", activity.getPackageName())));
                activity.startActivityForResult(intent, requestCode);

    }
}

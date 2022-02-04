package com.android.onyx.demo.scribble;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.onyx.android.demo.R;
import com.android.onyx.demo.utils.TouchUtils;
import com.android.onyx.demo.scribble.request.PartialRefreshRequest;
import com.onyx.android.sdk.api.device.epd.EpdController;
import com.onyx.android.sdk.api.device.epd.UpdateOption;
import com.onyx.android.sdk.data.PenConstant;
import com.onyx.android.sdk.device.Device;
import com.onyx.android.sdk.pen.NeoFountainPen;
import com.onyx.android.sdk.pen.RawInputCallback;
import com.onyx.android.sdk.pen.TouchHelper;
import com.onyx.android.sdk.pen.data.TouchPoint;
import com.onyx.android.sdk.pen.data.TouchPointList;
import com.onyx.android.sdk.rx.RxCallback;
import com.onyx.android.sdk.rx.RxManager;
import com.onyx.android.sdk.utils.NumberUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class ScribbleCanvasActivity extends AppCompatActivity {
    @Bind(R.id.surfaceView)
    SurfaceView surfaceView;
    @Bind(R.id.enable_pen_up_refresh)
    CheckBox cbPenUpRefreshEnable;
    @Bind(R.id.eraser)
    CheckBox cbEraser;

    private final Context context = this;
    private TouchHelper touchHelper;
    private RawInputCallback rawInputCallback;
    private RxManager rxManager;

    public Canvas canvas;
    public Bitmap bitmap;
    public Paint paint = new Paint();

    private static final String TAG = ScribbleCanvasActivity.class.getSimpleName();
    public final float STROKE_WIDTH = 3.0f;
    public final float PAINT_STROKE_WIDTH = 3.0f;
    public final int REFRESH_DELAY_TIME_MS = PenConstant.DEFAULT_PEN_UP_REFRESH_TIME_MS * 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scribble_canvas);
        ButterKnife.bind(this);
        initSurfaceView(surfaceView);
        initPaint();
    }

    @Override
    protected void onResume() {
        if (touchHelper != null) {
            touchHelper.setRawDrawingEnabled(true);
        }
        super.onResume();
    }
    @Override
    protected void onPause() {
        touchHelper.setRawDrawingEnabled(false);
        super.onPause();
    }
    @Override
    protected void onDestroy() {
        touchHelper.closeRawDrawing();
        bitmapRecycle();
        super.onDestroy();
    }

    public void storeSurface(){}
    public void loadSurface(){}

    private void initPaint() {
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(PAINT_STROKE_WIDTH);
    }
    private void initSurfaceView(final SurfaceView surfaceView) {
        final SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Rect limit = new Rect();
                surfaceView.getLocalVisibleRect(limit);
                touchHelper = TouchHelper.create(surfaceView, getRawInputCallback());
                touchHelper.setLimitRect(limit, new ArrayList<Rect>())
                        .setStrokeWidth(STROKE_WIDTH)
                        .openRawDrawing();
                touchHelper.setStrokeStyle(TouchHelper.STROKE_STYLE_PENCIL);
                initPenUpRefreshConfig();
                cleanSurfaceView(surfaceView);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                holder.removeCallback(this);
            }
        };
        surfaceView.getHolder().addCallback(surfaceCallback);
    }

    private void cleanSurfaceView(SurfaceView surfaceView) {
        if (surfaceView.getHolder() == null) {
            return;
        }
        Canvas canvas = surfaceView.getHolder().lockCanvas();
        if (canvas == null) {
            return;
        }
        canvas.drawColor(Color.WHITE);
        surfaceView.getHolder().unlockCanvasAndPost(canvas);
    }

    public RawInputCallback getRawInputCallback() {
        if (rawInputCallback == null) {
            rawInputCallback = new RawInputCallback() {
                @Override
                public void onBeginRawDrawing(boolean b, TouchPoint touchPoint) {
                    TouchUtils.disableFingerTouch(context);
                }

                @Override
                public void onEndRawDrawing(boolean b, TouchPoint touchPoint) {
                }

                @Override
                public void onRawDrawingTouchPointMoveReceived(TouchPoint touchPoint) {

                }

                @Override
                public void onRawDrawingTouchPointListReceived(TouchPointList touchPointList) {
                    Log.d(TAG, "onRawDrawingTouchPointListReceived");
                    List<TouchPoint> tp_list = touchPointList.getPoints();
                    drawScribbleToBitmap(tp_list);
                }

                @Override
                public void onBeginRawErasing(boolean b, TouchPoint touchPoint) {
                }

                @Override
                public void onEndRawErasing(boolean b, TouchPoint touchPoint) {
                }

                @Override
                public void onRawErasingTouchPointMoveReceived(TouchPoint touchPoint) {
                }


                @Override
                public void onRawErasingTouchPointListReceived(TouchPointList touchPointList) {
                }

                @Override
                public void onPenUpRefresh(RectF refreshRect) {
                    Log.d(TAG, "entered refresh");
                    getRxManager().enqueue(new PartialRefreshRequest(ScribbleCanvasActivity.this, surfaceView, refreshRect)
                                    .setBitmap(bitmap),
                            new RxCallback<PartialRefreshRequest>() {
                                @Override
                                public void onNext(@NonNull PartialRefreshRequest partialRefreshRequest) {
                                }
                            });
                    Log.d(TAG, "exited refresh");
                }
            };
        }
        return rawInputCallback;
    }

    private RxManager getRxManager() {
        if (rxManager == null) {
            rxManager = RxManager.Builder.sharedSingleThreadManager();
        }
        return rxManager;
    }
    private void bitmapRecycle() {
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
    }
    private void initPenUpRefreshConfig() {
        cbPenUpRefreshEnable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                touchHelper.setRawDrawingEnabled(false);
                touchHelper.setPenUpRefreshEnabled(isChecked);
                touchHelper.setRawDrawingEnabled(true);
            }
        });
        touchHelper.setPenUpRefreshTimeMs(REFRESH_DELAY_TIME_MS);
        cbPenUpRefreshEnable.setChecked(true);
        touchHelper.setRawDrawingEnabled(true);
    }
    @OnClick(R.id.button_clear2)
    public void clear_surf(){
        touchHelper.setRawDrawingEnabled(false);
        bitmapRecycle();
        cleanSurfaceView(surfaceView);
        touchHelper.setRawDrawingEnabled(true);
    }

    private void drawScribbleToBitmap(List<TouchPoint> list) {
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(surfaceView.getWidth(), surfaceView.getHeight(), Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmap);
        }
        float maxPressure = EpdController.getMaxTouchPressure();
        NeoFountainPen.drawStroke(canvas, paint, list, NumberUtils.FLOAT_ONE, STROKE_WIDTH, maxPressure, cbEraser.isChecked());

        // Refresh the screen (maybe not the best way of doing so)
        if (cbPenUpRefreshEnable.isChecked()) {
            touchHelper.setRawDrawingEnabled(false);
            touchHelper.setRawDrawingEnabled(true);
        }
        TouchUtils.enableFingerTouch(context);
    }
}

//} else {
//            Path path = new Path();
//            PointF prePoint = new PointF(list.get(0).x, list.get(0).y);
//            path.moveTo(prePoint.x, prePoint.y);
//            for (TouchPoint point : list) {
//                path.quadTo(prePoint.x, prePoint.y, point.x, point.y);
//                prePoint.x = point.x;
//                prePoint.y = point.y;
//            }
//            canvas.drawPath(path, paint);
//        }

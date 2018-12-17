package com.example.recorder.recorder.view.wave;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.recorder.recorder.view.wave.impl.DefaultAmplitudeHelper;

import java.util.List;


public abstract class RenderView extends SurfaceView implements SurfaceHolder.Callback {

    private AbsAmplitudeHelper amplitudeHelper;

    public RenderView(Context context) {
        this(context, null);

        init();
    }

    public RenderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

        init();
    }

    public RenderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getHolder().addCallback(this);

        init();
    }

    private void init() {
        setZOrderOnTop(true);//把SurfaceView置于Activity显示窗口的最顶层
        getHolder().setFormat(PixelFormat.TRANSLUCENT);//使窗口支持透明度
    }

    public void setAmplitudeHelper(AbsAmplitudeHelper amplitudeHelper) {
        this.amplitudeHelper = amplitudeHelper;
    }

    public AbsAmplitudeHelper getAmplitudeHelper() {
        return amplitudeHelper;
    }

    /*回调/线程*/

    private class RenderThread extends Thread {

        private static final long SLEEP_TIME = 70;

        private SurfaceHolder surfaceHolder;
        private boolean running = true;

        private boolean suspended = false;

        private long lastTime = 0l;
        private long used = 0l;

        public RenderThread(SurfaceHolder holder) {
            super("RenderThread");
            surfaceHolder = holder;
            lastTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            while (true) {
//                waitWhileSuspended();

                synchronized (surfaceLock) {
                    if (!running) {
                        Canvas canvas = surfaceHolder.lockCanvas();
                        if (canvas != null) {
                            surfaceHolder.unlockCanvasAndPost(canvas);
                        }
                        return;
                    }
                    Canvas canvas = surfaceHolder.lockCanvas();
                    if (canvas != null) {

                        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);//绘制透明色

                        lastTime = System.currentTimeMillis();
                        render(canvas, new float[]{amplitudeHelper.getAmplitude()});  //这里做真正绘制的事情
                        surfaceHolder.unlockCanvasAndPost(canvas);
                        used = System.currentTimeMillis() - lastTime;
                    }
                }

                try {
                    Thread.sleep(Math.max(0, SLEEP_TIME - used));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void setRun(boolean isRun) {
            this.running = isRun;

			if (!isRun) {
				surfaceHolder.getSurface().release();
			}
            
        }

        public void pauseSuspended() {
            suspended = true;
        }

        public void resumeSuspended() {
            suspended = false;
        }

        private void waitWhileSuspended() {
            while (suspended) {
                try {
                    Canvas canvas = surfaceHolder.lockCanvas();
                    if (canvas != null) {
                        onPause(canvas);
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private final Object surfaceLock = new Object();
    private RenderThread renderThread;

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        renderer = onCreateRenderer();
        if (renderer != null && renderer.isEmpty()) {
            throw new IllegalStateException();
        }

        setAmplitudeHelper(new DefaultAmplitudeHelper());

        renderThread = new RenderThread(holder);
        renderThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //这里可以获取SurfaceView的宽高等信息
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        synchronized (surfaceLock) {  //这里需要加锁，否则doDraw中有可能会crash
            renderThread.setRun(false);
            amplitudeHelper.setRun(false);
        }
    }

    /*绘图*/
    public interface IRenderer {

        void onRender(Canvas canvas, float[] millisPassed);
    }

    private List<IRenderer> renderer;

    protected List<IRenderer> onCreateRenderer() {
        return null;
    }

    private void render(Canvas canvas, float[] amplitude) {
//            onRender(canvas, amplitude);
//            onRender1(canvas, amplitude);
        onRender2(canvas, amplitude);
    }

    /**
     * 暂停动画清屏
     */
    public void pause() {
        if (renderThread != null) {
            renderThread.pauseSuspended();
        }
    }

    /**
     * 重新绘制动画
     */
    public void resume() {
        if (renderThread != null) {
            renderThread.resumeSuspended();
        }
    }

    /**
     * 渲染surfaceView的回调方法。
     *
     * @param canvas 画布
     */
    protected abstract void onRender(Canvas canvas, float[] amplitude);

    protected abstract void onRender1(Canvas canvas, float[] amplitude);

    protected abstract void onRender2(Canvas canvas, float[] amplitude);

    protected abstract void onPause(Canvas canvas);
}

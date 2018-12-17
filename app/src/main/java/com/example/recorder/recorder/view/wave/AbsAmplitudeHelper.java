package com.example.recorder.recorder.view.wave;

import com.example.recorder.recorder.recorders.IRecorderListener;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by Administrator on 2017/12/26.
 */

public abstract class AbsAmplitudeHelper implements IRecorderListener {
    private static final long CALC_TIME = 25;
    public static final float VOICE_RANGE = 1000f;

    public static final int MODE_DEFAULT = 0;//默认振幅状态
    public static final int MODE_VOICE = 1;//有声音传过来状态

    public static final int LINE_NUM = 5;
    /**
     * 当前画线的振幅
     */
    protected float amplitude = 0f;
    protected float[] amplitudeArray = new float[LINE_NUM];

    /**
     * 默认振幅
     */
    protected float defaultAmplitude = 0f;
    protected float[] defaultAmplitudeArray = new float[LINE_NUM];

    /**
     * 要到达的振幅
     */
    protected float targetAmplitude = 0f;

    /**
     * 扩张/收缩速度
     */
    protected float speed = 0f;
    protected float[] speedArray = new float[LINE_NUM];

    /**
     * 上次要到达的振幅
     * @param targetAmplitude
     */
    protected float lastTargetAmplitude = 0f;

    protected int mode = MODE_DEFAULT;

    /**
     * 循环定时器， 产生随机值
     */
    private Timer timer;
    private TimerTask timerTask;
    private Random random;

    private int randomInt;
    private int max = 350;
    private int min = 200;

    private boolean running = true;

    /**
     * 生成随机振幅
     */
    private Thread defaultThread;

    public AbsAmplitudeHelper() {
        random = new Random();

//        startRandomTask();
        defaultThread = new Thread(new DefaultRunnable());
        Thread thread = new Thread(new CaculateRunnable(), "CaculateThread");
        defaultThread.start();
        thread.start();

//        new Thread() {
//            @Override
//            public void run() {
//                super.run();
//                while (running) {
//                    float f = random.nextFloat();
//                    onVolumeChanged(f % 1f);
//                    try {
//                        Thread.sleep(500);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }.start();

//        SREventDispatcher.getInstance().registerSREventListener(this);
    }

    private void initTimerTask() {
        timerTask = new TimerTask() {
            @Override
            public void run() {
                onDefaultMod();
                randomInt = random.nextInt(max) % (max - min + 1) + min;
                setTargetAmplitude(randomInt);
            }
        };
    }

    private void releaseTimerTask() {
        if (timerTask != null) {
            timerTask.cancel();
        }
    }

    protected void startRandomTask() {
        if (null == timer) {
            timer = new Timer();
            initTimerTask();
        } else {
            releaseTimerTask();
            timer.purge();
            initTimerTask();
        }

        timer.scheduleAtFixedRate(timerTask, 300, 500);
    }

    /**
     * 在这之前首先要把声音大小转换成振幅
     * @param targetAmplitude
     */
    public synchronized void setTargetAmplitude(float targetAmplitude) {
        this.lastTargetAmplitude = this.targetAmplitude;
        this.targetAmplitude = targetAmplitude;

        calculateSpeed();
    }

    public void setDefaultAmplitude(float defaultAmplitude) {
        this.defaultAmplitude = defaultAmplitude;
    }

    public float getAmplitude() {
        return amplitude / VOICE_RANGE;
    }

    public float[] getAmplitudeArray() {
        float[] returnArray = new float[LINE_NUM];
        for (int i = 0; i < LINE_NUM; i++) {
            returnArray[i] = Math.abs(amplitudeArray[i]) / VOICE_RANGE;
        }
        return returnArray;
    }

    public void setRun(boolean running) {
        this.running = running;

        if (!running) {
            if (timer != null) {
                timer.cancel();
            }
//            SREventDispatcher.getInstance().unregisterSREventListener(this);
        }
    }

    /**
     * 计算出每次的振幅
     */
    class CaculateRunnable implements Runnable {
        public CaculateRunnable() {

        }

        @Override
        public void run() {
            while (true) {
                if (!running) {
                    break;
                }

                calculateAmplitude(CALC_TIME);

                if (isReached()) {
                    onRewindMod();
                    setTargetAmplitude(defaultAmplitude);

                    synchronized (defaultThread) {
                        defaultThread.notify();
                    }
                }

                try {
                    Thread.sleep(CALC_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    /**
     * 产生随机振幅
     */
    class DefaultRunnable implements Runnable {
        public DefaultRunnable() {
        }

        @Override
        public void run() {
            while (running) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                onDefaultMod();
                randomInt = random.nextInt(max) % (max - min + 1) + min;
                setTargetAmplitude(randomInt);
            }
        }
    }

    @Override
    public void onVolumeChanged(float v) {
        float val = v;
        if (val < 0f) {
            val = 0f;
        } else if (val > 1.0f) {
            val = 1.0f;
        }

        synchronized (defaultThread) {
            try {
                defaultThread.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        onVoiceMod();
        setTargetAmplitude(val * VOICE_RANGE);
//        startRandomTask();//每次
    }

    public abstract void calculateSpeed();

    public abstract void calculateAmplitude(Long calcTime);

    public abstract boolean isReached();

    /**
     * 固定时间之后哦没有声音传进来的状态
     */
    public abstract void onDefaultMod();

    /**
     * 有声音传进来的状态
     */
    public abstract void onVoiceMod();

    public abstract void onRewindMod();
}

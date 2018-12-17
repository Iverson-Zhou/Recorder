package com.example.recorder.recorder.view.wave.impl;


import com.example.recorder.recorder.view.wave.AbsAmplitudeHelper;

/**
 * Created by Administrator on 2017/12/26.
 */

public class DefaultAmplitudeHelper extends AbsAmplitudeHelper {

    private long waveTime = 300l;

    private float[] accelerateArray = {2.0f, 1.5f, 1.0f, 0.5f, 0.2f};

    public DefaultAmplitudeHelper() {
        super();
    }

    @Override
    public synchronized void calculateSpeed() {
        speed = (targetAmplitude - amplitude) / waveTime;

//        for (int i = 0; i < LINE_NUM; i++) {
//            speedArray[i] = 0;
//        }
    }

    private void calc() {
        for (int i = 0; i < LINE_NUM; i++) {
//            if (Math.abs(speedArray[i]) < Math.abs(speed)) {
//                speedArray[i] += (speed > 0 ? accelerateArray[i] : (-accelerateArray[i]));
//                Log.i("zhoukai_speed", "speedArray [" + i + "]" + ": " + speedArray[i] + " speed " + speed +
//                        "targetAmplitude " + targetAmplitude + " lastTargetAmplitude " + lastTargetAmplitude);
//            }
            if (speed > 0) {
                if (speedArray[i] < speed) {
                    speedArray[i] += accelerateArray[i];
                }
            } else {
                if (speedArray[i] > speed) {
                    speedArray[i] += -accelerateArray[i];
                }
            }
        }
    }

    @Override
    public void calculateAmplitude(Long calcTime) {
        amplitude += speed * calcTime;
//        calc();
//
//        for (int i = 0; i < LINE_NUM; i++) {
//            amplitudeArray[i] += (speedArray[i] * calcTime);
//        }
    }

    @Override
    public boolean isReached() {
        if (Math.abs(targetAmplitude - amplitude) <= 2 * Math.abs(speed)) {
            return true;
        }

        return false;
    }

    @Override
    public void onDefaultMod() {
        waveTime = 300l;
        mode = MODE_DEFAULT;
    }

    @Override
    public void onVoiceMod() {
        waveTime = 100l;
        mode = MODE_VOICE;
    }

    @Override
    public void onRewindMod() {
        if (mode == MODE_VOICE) {
            waveTime = 40l;
        }
    }
}

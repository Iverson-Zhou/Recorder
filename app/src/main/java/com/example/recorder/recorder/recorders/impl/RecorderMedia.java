package com.example.recorder.recorder.recorders.impl;

import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.example.recorder.recorder.recorders.IRecorder;
import com.example.recorder.recorder.recorders.IRecorderListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author THINK
 * @time 2018/12/12 17:53
 */
public class RecorderMedia implements IRecorder {
    private static final String TAG = RecorderMedia.class.getSimpleName();
    private static final int MAX_LENGTH = 1000 * 60 * 10;
    private static double BASE = 1;
    private static double SPACE = 100;
    private static double MAXAMPLITUDE = 32767d;

    private IRecorderListener mListener;
    private MediaRecorder mMediaRecorder;
    private String filePath;

    private long startTime;
    private long endTime;

    private Handler handler;

    private Thread thread = null;

    public RecorderMedia() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssmmm");
        Date date = new Date();

        this.filePath = BASE_FILE_PATH + PATH + "/" + sdf.format(date);
    }

    public RecorderMedia(String name) {
        this.filePath = BASE_FILE_PATH + PATH + "/" + filePath;
    }

    private void initThread() {
        if (null == thread) {
            thread = new Thread(new StatusRunnable());
            thread.start();
        }
    }

    private void stopThread() {
        if (null != thread) {
            if (null != handler) {
                handler.getLooper().quitSafely();
            }
            try {
                thread.interrupt();
            }catch (SecurityException e) {
                e.printStackTrace();
            } finally {
                thread = null;
            }
        }
    }
    @Override
    public void startRecorde() {
        if (mMediaRecorder == null)
            mMediaRecorder = new MediaRecorder();
        try {
            /* ②setAudioSource/setVedioSource */
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);// 设置麦克风
            /* ②设置音频文件的编码：AAC/AMR_NB/AMR_MB/Default 声音的（波形）的采样 */
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            /*
             * ②设置输出文件的格式：THREE_GPP/MPEG-4/RAW_AMR/Default THREE_GPP(3gp格式
             * ，H263视频/ARM音频编码)、MPEG-4、RAW_AMR(只支持音频且音频编码要求为AMR_NB)
             */
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            /* ③准备 */
            mMediaRecorder.setOutputFile(filePath);
            mMediaRecorder.setMaxDuration(MAX_LENGTH);
            mMediaRecorder.prepare();
            /* ④开始 */
            mMediaRecorder.start();
            // AudioRecord audioRecord.
            /* 获取开始时间* */
            startTime = System.currentTimeMillis();
            initThread();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopRecorde() {
        if (mMediaRecorder != null) {
            endTime = System.currentTimeMillis();
            Log.i("ACTION_END", "endTime" + endTime);
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            stopThread();
            Log.i("ACTION_LENGTH", "Time" + (endTime - startTime));
            return ;
        }
    }

    @Override
    public void setRecorderListener(IRecorderListener listener) {
        this.mListener = listener;
    }

    class StatusRunnable implements Runnable {
        @Override
        public void run() {
            Looper.prepare();
            handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);

                    updateMicStatus();
                }
            };
            updateMicStatus();
            Looper.loop();
        }
    }

    private void updateMicStatus() {
        if (mMediaRecorder != null) {
            double ratio = (double)mMediaRecorder.getMaxAmplitude();

//            double db = 0;// 分贝
//            if (ratio > 1)
//                db = 20 * Math.log10(ratio);
//            Log.d(TAG,"分贝值："+ratio);
            Log.i(TAG, "updateMicStatus: " + ratio / MAXAMPLITUDE);
            mListener.onVolumeChanged((float) (ratio / MAXAMPLITUDE));
            handler.sendEmptyMessageDelayed(1, 20);
        }
    }
}

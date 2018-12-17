package com.example.recorder.recorder.recorders.impl;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.example.recorder.recorder.recorders.IRecorder;
import com.example.recorder.recorder.recorders.IRecorderListener;

/**
 * @author THINK
 * @time 2018/12/13 17:21
 */
public class RecorderAudio implements IRecorder {

    private static final String TAG = "AudioRecord";
    private static final double MAX_DB = 100;
    private static final int SAMPLE_RATE_IN_HZ = 8000;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ,
            AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
    private AudioRecord audioRecord;

    private short[] buffer = new short[BUFFER_SIZE];

    private boolean isRecording = false;

    private Thread recordingThread = null;

    private IRecorderListener recorderListener;

    public RecorderAudio() {
        this.audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_IN_HZ,
                AudioFormat.CHANNEL_IN_DEFAULT , AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
    }

    @Override
    public void startRecorde() {
        if (null != audioRecord) {
            if (null == recordingThread) {
                isRecording = true;
                recordingThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        audioRecord.startRecording();

                        while (isRecording) {
                            int read = audioRecord.read(buffer, 0, BUFFER_SIZE);
                            if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                                long v = 0;
                                for (int i = 0; i < buffer.length; i++) {
                                    v += buffer[i] * buffer[i];
                                }

                                if (null != recorderListener) {
                                    Log.i("zhoukai", "v: " + v + " " + read);
                                    double amplitude = (double)v / (double)read;
//                                    amplitude = Math.sqrt(amplitude);
                                    double db = 0;// 分贝
                                    db = 10 * Math.log10(amplitude);
                                    Log.i(TAG, "run: " + db / MAX_DB);
                                    recorderListener.onVolumeChanged((float) (db / MAX_DB));
                                }
                            }
                        }

                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
                recordingThread.start();
            }
        }
    }

    @Override
    public void stopRecorde() {
        if (null != audioRecord) {
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            recordingThread = null;
        }
    }

    @Override
    public void setRecorderListener(IRecorderListener listener) {
        this.recorderListener = listener;
    }
}

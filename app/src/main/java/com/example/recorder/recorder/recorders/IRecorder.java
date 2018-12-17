package com.example.recorder.recorder.recorders;

/**
 * @author THINK
 * @time 2018/12/12 17:42
 */
public interface IRecorder {
    String BASE_FILE_PATH = "/storage/emulated/0";
    String PATH = "/recorder";

    void startRecorde();

    void stopRecorde();

    void setRecorderListener(IRecorderListener listener);
}

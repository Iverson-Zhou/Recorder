package com.example.recorder.recorder.view.wave;

import android.graphics.Canvas;
import android.graphics.EmbossMaskFilter;
import android.graphics.LinearGradient;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;

/**
 * Created by Administrator on 2017/12/26.
 */

public class WavePath extends Path {
    public static final int MAX_TIMEDELAY = 10;

    public static final int SHADER_POOL_SIZE = 20;

    /**
     * 缩放时，左边终点不动
     */
    public static final int HORIZONTAL_ZOOM_MODE_LEFT = 0;

    /**
     * 缩放时，中间点不动
     */
    public static final int HORIZONTAL_ZOOM_MODE_MID = 1;

    /**
     * 缩放时，右边终点不动
     */
    public static final int HORIZONTAL_ZOOM_MODE_RIGHT = 2;

    /**
     * 波动起点
     */
    private float waveStart;

    /**
     *波动中点
     */
    private float waveMid;

    /**
     *波动终点
     */
    private float waveEnd;

    /**
     *波动宽度
     */
    private float waveWidth;

    /**
     * 初始波动宽度
     */
    private float orignalWaveWidth;

    /**
     * 画布中心的高度
     */
    private float centerHeight;

    /**
     * 采样点的X
     */
    private float[] samplingX;

    /**
     * 采样点位置均匀映射到[-2,2]的X
     */
    private float[] mapX;

    /**
     * 最大振幅
     */
    private float maxAmplitude;

    /**
     * 时延
     */
    private int timeDelay;

    /**
     * 当前存入的位置
     */
    private int lineSavePosition;

    /**
     * 第一次进入
     */
    private boolean first = true;

    /**
     * 振幅比例
     */
    private float amplitudeProportion = 1.0f;

    /**
     * 起点
     */
    private float offset = 0f;

    /**
     * 渐变
     */
    private Shader shader;

    /**渐变颜色数组
     *
     */
    private int[] colors;

    /**
     * 渐变位置数组
     */
    private float[] positions;

    /**
     * 边缘模糊效果
     */
    private MaskFilter maskFilter;

    /**
     * 衰减函数衰减速度
     */
    private double exponent;

    private Paint paint = new Paint();

    /**
     * 缓存延迟线段
     */
    private Point[][] delayArray = new Point[MAX_TIMEDELAY][WaveView.SAMPLING_SIZE + 2];

    /**
     * 缓存延迟线段宽度
     */
    private WaveRange[] waveRanges = new WaveRange[MAX_TIMEDELAY];

    /**
     * shader池，避免次画线时new Shader
     */
    private Shader[] shaderPool = new Shader[SHADER_POOL_SIZE];

    /**
     * 0..1
     */
    private float horizontalZoomRange;

    private int horizontalZoomMode = HORIZONTAL_ZOOM_MODE_LEFT;

    float horizontalZoomSplit;

    /**
     * @param timeDelay 延迟几次canvas刷新
     */
    public WavePath(int timeDelay) {
        setTimeDelay(timeDelay);
    }

    public WavePath(Path src, int timeDelay) {
        super(src);
        setTimeDelay(timeDelay);
    }

    public void setTimeDelay(int timeDelay) {
        if (timeDelay < 0) {
            this.timeDelay = 0;
            return;
        }
        if (timeDelay > MAX_TIMEDELAY) {
            this.timeDelay = MAX_TIMEDELAY;
            return;
        }

        this.timeDelay = timeDelay;
    }

    /**
     *
     * @param waveStart
     * @param waveWidth
     * @param maxAmplitude
     * @param amplitudeProportion
     * @param colors
     * @param positions
     * @param centerHeight
     * @param lineWidth
     * @param blurWidth
     * @param exponent 衰减函数参数，越靠近0衰减度越小
     */
    public void init(float waveStart, float waveWidth, float maxAmplitude, float amplitudeProportion,
                     int[] colors, float[] positions, float centerHeight, float lineWidth, float blurWidth,
                     double exponent, float horizonTalZoomRange) {
        this.waveStart = waveStart;
        this.waveEnd = waveStart + waveWidth;
        this.waveMid = waveStart + (waveWidth / 2f);
        this.waveWidth = waveWidth;
        this.orignalWaveWidth = waveWidth;
        this.maxAmplitude = maxAmplitude;
        this.centerHeight = centerHeight;
        this.amplitudeProportion = amplitudeProportion;
        this.colors = colors;
        this.positions = positions;
        this.exponent = exponent;
        this.horizontalZoomRange = horizonTalZoomRange;

        shader = new LinearGradient(waveStart, maxAmplitude,
                waveStart + waveWidth / 2, maxAmplitude, colors, positions, Shader.TileMode.MIRROR);
//        maskFilter = new BlurMaskFilter(blurWidth, BlurMaskFilter.Blur.NORMAL);

        float[] direction = new float[]{1, 1, 1};
        float light = 0.8f;
        float specular = 6.0f;
        float blur = 2.5f;
        maskFilter = new EmbossMaskFilter(direction, light, specular, blur);

        paint.setShader(shader);
        paint.setMaskFilter(maskFilter);
        paint.setStrokeWidth(lineWidth);
        paint.setStyle(Paint.Style.STROKE);

        //初始化采样点和映射
        samplingX = new float[WaveView.SAMPLING_SIZE + 1];//因为包括起点和终点所以需要+1个位置
        mapX = new float[WaveView.SAMPLING_SIZE + 1];//同上
        float gap = waveWidth / (float) WaveView.SAMPLING_SIZE;//确定采样点之间的间距
        float x;
        for (int i = 0; i <= WaveView.SAMPLING_SIZE; i++) {
            x = (i + 1) * gap;
            samplingX[i] = x;
            mapX[i] = (x / waveWidth) * 4 - 2;//将x映射到[-2,2]的区间上
        }

        initShaderPool();
    }


    public void setHorizontalZoomMode(int horizontalZoomMode) {
        this.horizontalZoomMode = horizontalZoomMode;
    }

    /**
     * 初始化shaderpool
     */
    private void initShaderPool() {
        float horizontalZoom = orignalWaveWidth * horizontalZoomRange;
        horizontalZoomSplit = horizontalZoom / (float) SHADER_POOL_SIZE;
        float xstart = waveStart;
        float xend = waveEnd;

        for (int i = 0; i < SHADER_POOL_SIZE; i++) {
            switch (horizontalZoomMode) {
                case HORIZONTAL_ZOOM_MODE_LEFT:
                    xstart = waveStart;
                    xend = waveStart + (1.0f - horizontalZoomRange) * orignalWaveWidth + (i) * horizontalZoomSplit;
                    break;
                case HORIZONTAL_ZOOM_MODE_MID:
                    xstart = waveMid - (((1.0f - horizontalZoomRange) * orignalWaveWidth) / 2.0f) - ((i) * (horizontalZoomSplit / 2.0f));
                    xend = waveMid + (((1.0f - horizontalZoomRange) * orignalWaveWidth) / 2.0f) + ((i) * (horizontalZoomSplit / 2.0f));
                    break;
                case HORIZONTAL_ZOOM_MODE_RIGHT:
                    xstart = waveEnd - (1.0f - horizontalZoomRange) * orignalWaveWidth - (i) * horizontalZoomSplit;
                    xend = waveEnd;
                    break;
            }

            shaderPool[i] = new LinearGradient(xstart, centerHeight, (xstart + (xend - xstart) / 2.0f),
                    centerHeight, colors, positions, Shader.TileMode.MIRROR);
        }
    }

    public void setWaveWidth(float waveWidth) {
        this.waveWidth = waveWidth;

        switch (horizontalZoomMode) {
            case HORIZONTAL_ZOOM_MODE_LEFT:
                this.waveEnd = waveStart + waveWidth;
                break;
            case HORIZONTAL_ZOOM_MODE_MID:
                this.waveStart = waveMid - (waveWidth / 2.0f);
                this.waveEnd = waveMid + (waveWidth / 2.0f);
                break;
            case HORIZONTAL_ZOOM_MODE_RIGHT:
                this.waveStart = this.waveEnd - waveWidth;
                break;
        }

        //横向缩放
        if (null == waveRanges[lineSavePosition]) {
            waveRanges[lineSavePosition] = new WaveRange(waveWidth);
        } else {
            waveRanges[lineSavePosition].waveWidth = waveWidth;
        }

        float gap = waveWidth / (float) WaveView.SAMPLING_SIZE;//确定采样点之间的间距
        float x;
        for (int i = 0; i <= WaveView.SAMPLING_SIZE; i++) {
            x = (i + 1) * gap;
            samplingX[i] = x;
            mapX[i] = (x / waveWidth) * 4 - 2;//将x映射到[-2,2]的区间上
        }


    }

    /**
     *
     * @param voiceAmplitude
     */
    public void generateLine(float voiceAmplitude) {
        float withProportion = Math.abs(voiceAmplitude) % horizontalZoomRange + (1.0f - horizontalZoomRange);
        setWaveWidth((orignalWaveWidth * withProportion));
        //提前申明各种临时参数
        float x;

        //波形函数的值，包括上一点，当前点和下一点
        float curV = 0, nextV = (float) (maxAmplitude * calcValue(mapX[0], 0.5f));

        //遍历所有采样点
        for (int i = 0; i <= WaveView.SAMPLING_SIZE; i++) {
            //计算采样点的位置
            x = samplingX[i];
            curV = nextV;
            //提前算出下一采样点的值
            nextV = i < WaveView.SAMPLING_SIZE ? (float) (voiceAmplitude * maxAmplitude * calcValue(mapX[i + 1], 0.5f)) : 0;

            //连接路径
            lineToDelay(waveStart + x, centerHeight + curV * amplitudeProportion, i);

        }
        //连接所有路径到终点
        lineToDelay(waveEnd, centerHeight, WaveView.SAMPLING_SIZE + 1);
    }

    /**
     * 缓存到数组中
     * @param x
     * @param y
     * @param pointPosition
     */
    public void lineToDelay(float x, float y, int pointPosition) {
        if (first && lineSavePosition <= timeDelay) {
            if (null == delayArray[lineSavePosition][pointPosition]) {
                delayArray[lineSavePosition][pointPosition] = new Point(x, y);
            }
        } else {
            delayArray[lineSavePosition][pointPosition].x = x;
            delayArray[lineSavePosition][pointPosition].y = y;
        }

        if (pointPosition == delayArray[lineSavePosition].length - 1) {
            if (0 == timeDelay) {
                first = false;
            } else {
                lineSavePosition += 1;

                if (lineSavePosition >= timeDelay) {
                    first = false;
                    lineSavePosition = lineSavePosition % timeDelay;
                }
            }

        }
    }

    /**
     * 根据缓存的帧数画线， 使用path中自带的画笔
     * @param canvas
     */
    public void drawDelay(Canvas canvas) {
        //横向缩放
        if (waveRanges[lineSavePosition] != null) {
            int shaderPosition = (int) ((this.orignalWaveWidth - waveRanges[lineSavePosition].waveWidth) / horizontalZoomSplit);
            shaderPosition = SHADER_POOL_SIZE - shaderPosition - 1;

            if (shaderPosition < 0) {
                shaderPosition = 0;
            } else if ( shaderPosition >= SHADER_POOL_SIZE) {
                shaderPosition = SHADER_POOL_SIZE - 1;
            }

            if (null != shaderPool[shaderPosition]) {
                paint.setShader(shaderPool[shaderPosition]);
            }
        }

        drawDelay(canvas, paint);
    }

    /**
     * 根据缓存的帧数画线
     * @param canvas
     * @param paint
     */
    public void drawDelay(Canvas canvas, Paint paint) {
        if (first && lineSavePosition < timeDelay) {
            return;
        }

        rewind();
        for (int i = 0; i < delayArray[lineSavePosition].length; i++) {
            if (0 == i) {
                moveTo(delayArray[lineSavePosition][i].x, delayArray[lineSavePosition][i].y);
            }
            lineTo(delayArray[lineSavePosition][i].x, delayArray[lineSavePosition][i].y);
        }

        canvas.drawPath(this, paint);
    }

    /**
     * 计算波形函数中x对应的y值
     *
     * @param mapX   换算到[-2,2]之间的x值
     * @param offset 偏移量
     * @return
     */
    private double calcValue(float mapX, float offset) {
//        int keyX = (int) (mapX * 1000);
//        offset %= 2;
        double sinFunc = Math.sin(1.0 * Math.PI * mapX - offset * Math.PI);
        double recessionFunc;
//        if (recessionFuncs.indexOfKey(keyX) >= 0) {
//            recessionFunc = recessionFuncs.get(keyX);
//        } else {
//            recessionFunc = Math.pow(4 / (4 + Math.pow(mapX, 4)), 2.0);
//            recessionFuncs.put(keyX, recessionFunc);
//        }

        recessionFunc = Math.pow(4 / (4 + Math.pow(mapX, 4)), exponent);
        return sinFunc * recessionFunc;
    }

    class Point {
        public float x = 0;
        public float y = 0;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    class WaveRange {
        public float waveWidth = 0f;

        public WaveRange(float waveWidth) {
            this.waveWidth = waveWidth;
        }
    }
}

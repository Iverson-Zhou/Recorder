package com.example.recorder.recorder.view.wave;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.LinearGradient;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Xfermode;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.SparseArray;

import com.example.recorder.recorder.R;


public class WaveView extends RenderView {

    private static final String TAG = "WaveView";

    private static final float STORKE_WIDTH = 4.0f;

    private static final float HORIZONTAL_ZOOM_RANGE_FIRSTLINE = 0.4f;//线条横向缩放的范围
    private static final float HORIZONTAL_ZOOM_RANGE_OTHERLINE = 0.5f;//线条横向缩放的范围

    public WaveView(Context context) {
        this(context, null);
    }

    public WaveView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaveView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private final Paint paint = new Paint();

    private final Paint clearScreenPaint = new Paint();

    private int regionStartColor = getResources().getColor(R.color.regionStartColor);
    private int regionCenterColor = getResources().getColor(R.color.regionCenterColor);
    private int regionEndColor = getResources().getColor(R.color.regionEndColor);

    {
        paint.setDither(true);
        paint.setAntiAlias(true);
    }

    private final WavePath firstPath = new WavePath(0);
    private final WavePath secondPath = new WavePath(3);

    private final WavePath secondRightPath = new WavePath(5);

    private final WavePath centerLeftPath = new WavePath(6);
    private final WavePath centerRightPath = new WavePath(6);

    /**
     * 采样点的数量，越高越精细，
     * 但高于一定限度后人眼察觉不出。
     */
    public static final int SAMPLING_SIZE = 64;
    /**
     * 采样点的X
     */
    private float[] samplingX;
    /**
     * 采样点位置均匀映射到[-2,2]的X
     */
    private float[] mapX;

    /**
     * 画布宽高
     */
    private float width, height;
    /**
     * 画布中心的高度
     */
    private float centerHeight;
    /**
     * 振幅
     */
    private int amplitude;

    /**
     * 最大振幅
     */
    private float maxAmplitude;

    /**
     * 波动起点
     */
    private float waveStart;

    private float lineLength;

    /**
     *波动终点
     */
    private float waveEnd;

    /**
     *波动宽度
     */
    private float waveWidth;

    /**
     *波动高度
     */
    private int waveHeigth;

    private MaskFilter blurMaskFilter;//边缘模糊效果

    private Shader shader; //渐变效果

    private Shader shaderLeft; //渐变效果

    private Shader shaderRight; //渐变效果

    private boolean pathInited = false;//WavePath是否初始化过

    /**
     * 左边线
     */
    private Path left = new Path();

    /**
     * 右边线
     */
    private Path right = new Path();

    /**
     * 缓冲区
     */
    private Canvas bufferCanvas;
    Bitmap bufferBm;

    /**
     * 用于处理矩形的rectF
     */
    private final RectF rectF = new RectF();

    /**
     * 绘图交叉模式。放在成员变量避免每次重复创建。
     */
    private final Xfermode clipXfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);

    private final Xfermode clearXfermode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);

    private final Xfermode srcXfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC);

    private final int backGroundColor = Color.rgb(24, 33, 41);

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onRender(Canvas canvas, float[] amplitudeProportion) {
        //清屏
        clear(canvas);

        if (samplingX == null) {//首次初始化
            GradientColor.init(getResources());

            //赋值基本参数
            width = canvas.getWidth();
            height = canvas.getHeight();
            centerHeight = height * 0.5f;

            waveWidth = (int) (width * 0.6);//参数化，剧中，宽度
            waveStart = (int) ((width - waveWidth) / 2f);
            waveEnd = waveWidth + waveStart;

            maxAmplitude = waveWidth * 0.125f;//振幅为宽度的1/8

            blurMaskFilter = new BlurMaskFilter(2.0f, BlurMaskFilter.Blur.NORMAL);
            shader = new LinearGradient(waveStart, centerHeight,
                    waveStart + waveWidth / 2, centerHeight, GradientColor.colorsMainLine, GradientColor.positionsMainLine, Shader.TileMode.MIRROR);

            //初始化采样点和映射
            samplingX = new float[SAMPLING_SIZE + 1];//因为包括起点和终点所以需要+1个位置
            mapX = new float[SAMPLING_SIZE + 1];//同上
            float gap = waveWidth / (float) SAMPLING_SIZE;//确定采样点之间的间距
            float x;
            for (int i = 0; i <= SAMPLING_SIZE; i++) {
                x = (i + 1) * gap;
                samplingX[i] = x;
                mapX[i] = (x / (float) waveWidth) * 4 - 2;//将x映射到[-2,2]的区间上
            }

        }
        //左右淡入淡出
        getPath2(canvas, 0, shader);
        getPath2(canvas, waveEnd - waveStart, shader);

        //重置所有path并移动到起点
        firstPath.rewind();
        secondPath.rewind();
        centerLeftPath.rewind();
        firstPath.moveTo(waveStart, centerHeight);
        secondPath.moveTo(waveStart, centerHeight);
        centerLeftPath.moveTo(waveStart, centerHeight);

        //当前时间的偏移量，通过该偏移量使得每次绘图都向右偏移，让画面动起来
        //如果希望速度快一点，可以调小分母
//        float offset = millisPassed / 550F;
        float offset = 50f;
        amplitude = (int) (amplitudeProportion[0] * maxAmplitude);

        //提前申明各种临时参数
        float x;
        float[] xy;

        //波形函数的值，包括上一点，当前点和下一点
        float lastV, curV = 0, nextV = (float) (amplitude * calcValue(mapX[0], 0.5f));
        float lastVSecond, curvSecond = 0, nextVSecond = (float) (amplitude * calcValue(mapX[0], 0f));

        //遍历所有采样点
        for (int i = 0; i <= SAMPLING_SIZE; i++) {
            //计算采样点的位置
            x = samplingX[i];
            lastV = curV;
            curV = nextV;
            //提前算出下一采样点的值
            nextV = i < SAMPLING_SIZE ? (float) (amplitudeProportion[0] * maxAmplitude * calcValue(mapX[i + 1], 0.5f)) : 0;

            lastVSecond = curvSecond;
            curvSecond = nextVSecond;
            nextVSecond = i < SAMPLING_SIZE ? (float) (amplitudeProportion[3] * maxAmplitude * calcValue(mapX[i + 1], 0.8f)) : 0;

            //连接路径
            firstPath.lineTo(waveStart + x, centerHeight + curV);
            secondPath.lineTo(waveStart + x, centerHeight - curvSecond);
            //中间那条路径的振幅是上下的1/5
            centerLeftPath.lineTo(waveStart + x, centerHeight + curV / 5F);

        }
        //连接所有路径到终点
        firstPath.lineTo(waveEnd, centerHeight);
        secondPath.lineTo(waveEnd, centerHeight);
        centerLeftPath.lineTo(waveEnd, centerHeight);

//        float[] direction = new float[]{ 1, 1, 1 };
        //设置环境光亮度
//        float light = 1.0f;
        // 选择要应用的反射等级
//        float specular = 6;
        // 向mask应用一定级别的模糊
//        float blur = 3.5f;
//        EmbossMaskFilter embossMaskFilter = new EmbossMaskFilter(direction, light, specular, blur);

//        BlurMaskFilter blurMaskFilter = new BlurMaskFilter(2.0f, BlurMaskFilter.Blur.NORMAL);//边缘模糊效果
        paint.setMaskFilter(blurMaskFilter);
//        paint.setMaskFilter(embossMaskFilter);

//        Xfermode xfermode = new PorterDuffXfermode(PorterDuff.Mode.SCREEN);
//        paint.setXfermode(xfermode);

//        Shader shader = new LinearGradient(waveStart, centerHeight,
//                waveStart + waveWidth / 2, centerHeight,
//                getResources().getColor(R.color.regionStartColor), getResources().getColor(R.color.regionEndColor1), Shader.TileMode.MIRROR);
//        Shader shader = new LinearGradient(waveStart, centerHeight,
//                waveStart + waveWidth / 2, centerHeight, colors, positions, Shader.TileMode.MIRROR);
        paint.setShader(shader);

        //绘制上弦线
        paint.setStrokeWidth(STORKE_WIDTH);
        paint.setStyle(Paint.Style.STROKE);
//        paint.setColor(regionStartColor);
        canvas.drawPath(firstPath, paint);

        //绘制下弦线
//        paint.setColor(regionEndColor);
        canvas.drawPath(secondPath, paint);

        //绘制中间线
        paint.setColor(regionCenterColor);
        canvas.drawPath(centerLeftPath, paint);

        paint.setShader(null);

    }

    @Override
    protected void onRender1(Canvas canvas, float[] amplitudeProportion) {
        //清屏
        clear(canvas);

        if (samplingX == null) {//首次初始化
            GradientColor.init(getResources());

            //赋值基本参数
            width = canvas.getWidth();
            height = canvas.getHeight();
            centerHeight = height * 0.5f;

            waveWidth = (int) (width * 0.6);//参数化，剧中，宽度
            waveStart = (int) ((width - waveWidth) / 2f);
            waveEnd = waveWidth + waveStart;

            maxAmplitude = waveWidth * 0.125f;//振幅为宽度的1/8

            blurMaskFilter = new BlurMaskFilter(2.0f, BlurMaskFilter.Blur.NORMAL);
            shader = new LinearGradient(waveStart, centerHeight,
                    waveStart + waveWidth / 2, centerHeight, GradientColor.colorsMainLine, GradientColor.positionsMainLine, Shader.TileMode.MIRROR);

            //初始化采样点和映射
            samplingX = new float[SAMPLING_SIZE + 1];//因为包括起点和终点所以需要+1个位置
            mapX = new float[SAMPLING_SIZE + 1];//同上
            float gap = waveWidth / (float) SAMPLING_SIZE;//确定采样点之间的间距
            float x;
            for (int i = 0; i <= SAMPLING_SIZE; i++) {
                x = (i + 1) * gap;
                samplingX[i] = x;
                mapX[i] = (x / (float) waveWidth) * 4 - 2;//将x映射到[-2,2]的区间上
            }
        }

        getPath2(canvas, 0, shader/*, paint*/);
        getPath2(canvas, waveEnd - waveStart, shader/*, paintRight*/);

        //重置所有path并移动到起点
        firstPath.rewind();
        secondPath.rewind();
        centerLeftPath.rewind();
        firstPath.moveTo(waveStart, centerHeight);
        secondPath.moveTo(waveStart, centerHeight);
        centerLeftPath.moveTo(waveStart, centerHeight);

        float offset = 50f;

        //提前申明各种临时参数
        float x;
        float[] xy;

        //波形函数的值，包括上一点，当前点和下一点
        float lastV, curV = 0, nextV = (float) (amplitude * calcValue(mapX[0], 0.5f));
        float lastVSecond, curvSecond = 0, nextVSecond = (float) (amplitude * calcValue(mapX[0], 0f));

        //遍历所有采样点
        for (int i = 0; i <= SAMPLING_SIZE; i++) {
            //计算采样点的位置
            x = samplingX[i];
            lastV = curV;
            curV = nextV;
            //提前算出下一采样点的值
            nextV = i < SAMPLING_SIZE ? (float) (amplitudeProportion[0] * maxAmplitude * calcValue(mapX[i + 1], 0.5f)) : 0;

            lastVSecond = curvSecond;
            curvSecond = nextVSecond;
            nextVSecond = i < SAMPLING_SIZE ? (float) (amplitudeProportion[0] * maxAmplitude * calcValue(mapX[i + 1], 0.8f)) : 0;

            //连接路径
            firstPath.lineToDelay(waveStart + x, centerHeight + curV, i);
            secondPath.lineToDelay(waveStart + x, centerHeight - curvSecond/ 2F, i);
            //中间那条路径的振幅是上下的1/5
            centerLeftPath.lineToDelay(waveStart + x, centerHeight + curV / 5F, i);

        }
        //连接所有路径到终点
        firstPath.lineToDelay(waveEnd, centerHeight, SAMPLING_SIZE + 1);
        secondPath.lineToDelay(waveEnd, centerHeight, SAMPLING_SIZE + 1);
        centerLeftPath.lineToDelay(waveEnd, centerHeight, SAMPLING_SIZE + 1);

        paint.setMaskFilter(blurMaskFilter);

        paint.setShader(shader);

        //绘制上弦线
        paint.setStrokeWidth(STORKE_WIDTH);
        paint.setStyle(Paint.Style.STROKE);
        firstPath.drawDelay(canvas, paint);

        //绘制下弦线
        secondPath.drawDelay(canvas, paint);

        //绘制中间线
        centerLeftPath.drawDelay(canvas, paint);

        paint.setShader(null);

    }

    @Override
    protected void onRender2(Canvas canvas, float[] amplitudeProportion) {
        //清屏
        clear(canvas);

        if (!pathInited) {//首次初始化
            pathInited = true;
            GradientColor.init(getResources());

            //赋值基本参数
            width = canvas.getWidth();
            height = canvas.getHeight();
            centerHeight = height * 0.5f;

            waveWidth = width * 0.7f;//参数化，剧中，宽度
            waveStart = (width - waveWidth) * 0.5f;
            waveEnd = waveWidth + waveStart;

            lineLength = waveStart * 1.3f;

            maxAmplitude = Math.min(height * 0.5f - (int) STORKE_WIDTH, width * 0.125f);//振幅为高度的1/2和宽度的1/8的最小值

            firstPath.setHorizontalZoomMode(WavePath.HORIZONTAL_ZOOM_MODE_MID);
            firstPath.init(waveStart, waveWidth, maxAmplitude, 1.0f,
                    GradientColor.colorsMainLine, GradientColor.positionsMainLine,
                    centerHeight, STORKE_WIDTH, 0.8f, 1.7d, HORIZONTAL_ZOOM_RANGE_FIRSTLINE);

            secondPath.setHorizontalZoomMode(WavePath.HORIZONTAL_ZOOM_MODE_RIGHT);
            secondPath.init(waveStart + (waveWidth * 0.0153f), (int) (waveWidth / 2f), maxAmplitude, 0.5f,
                    GradientColor.colorsSecondLeftLine, GradientColor.positionsSecondLeftLine,
                    centerHeight, STORKE_WIDTH - 0.8f, 0.8f, 2.0d, HORIZONTAL_ZOOM_RANGE_OTHERLINE);

            secondRightPath.setHorizontalZoomMode(WavePath.HORIZONTAL_ZOOM_MODE_LEFT);
            secondRightPath.init(waveStart + (waveWidth / 2.0f) - (waveWidth * 0.0305f), waveWidth / 1.8f, maxAmplitude,  0.5f,
                    GradientColor.colorsSecondRightLine, GradientColor.positionsSecondRightLine,
                    centerHeight, STORKE_WIDTH - 0.8f, 0.8f, 1.0d, HORIZONTAL_ZOOM_RANGE_OTHERLINE);

            centerLeftPath.setHorizontalZoomMode(WavePath.HORIZONTAL_ZOOM_MODE_RIGHT);
            centerLeftPath.init(waveStart + (int) (waveWidth / 2f - waveWidth / 2.5f) + (waveWidth * 0.0190f),  waveWidth / 2.5f, maxAmplitude, 0.2f,
                    GradientColor.colorsCenterLeftLine, GradientColor.positionsCenterLeftLine,
                    centerHeight, STORKE_WIDTH - 1.2f, 0.8f, 1.0d, HORIZONTAL_ZOOM_RANGE_OTHERLINE);

            centerRightPath.setHorizontalZoomMode(WavePath.HORIZONTAL_ZOOM_MODE_LEFT);
            centerRightPath.init(waveStart +  waveWidth / 2f - (waveWidth * 0.0156f), waveWidth / 2.5f, maxAmplitude, 0.2f,
                    GradientColor.colorsCenterRigthLine, GradientColor.positionsCenterRigthLine,
                    centerHeight, STORKE_WIDTH - 1.2f, 0.8f, 1.0d, HORIZONTAL_ZOOM_RANGE_OTHERLINE);

            float[] direction = new float[]{ 1, 1, 1 };
            float light = 0.5f;
            float specular = 6;
            float blur = 1.5f;
            blurMaskFilter = new EmbossMaskFilter(direction, light, specular, blur);

            shaderLeft = new LinearGradient(0, centerHeight - 5,
                    2 * lineLength, centerHeight + 5, GradientColor.colorsleftLine, GradientColor.positionsLeftLine, Shader.TileMode.REPEAT);
            shaderRight = new LinearGradient(width - (2 * lineLength), centerHeight - 5,
                    width, centerHeight + 5, GradientColor.colorsrightLine, GradientColor.positionsRightLine, Shader.TileMode.REPEAT);

            paint.setStrokeWidth(STORKE_WIDTH + 2);
            paint.setStyle(Paint.Style.STROKE);
            paint.setMaskFilter(blurMaskFilter);

            bufferBm = Bitmap.createBitmap((int) width, (int) height, Bitmap.Config.ARGB_8888);
            bufferCanvas = new Canvas(bufferBm);
            getPath3(bufferCanvas, 0, shaderLeft, left);
            getPath3(bufferCanvas, width - 2 * lineLength, shaderRight, right);
        }

        canvas.drawBitmap(bufferBm, 0, 0, null);//画缓冲好的左右两条线段

        firstPath.generateLine(amplitudeProportion[0]);
        centerLeftPath.generateLine(amplitudeProportion[0]);
        centerRightPath.generateLine(amplitudeProportion[0]);
        secondPath.generateLine(-amplitudeProportion[0]);//第二条左边的振幅相反
        secondRightPath.generateLine(amplitudeProportion[0]);

        centerLeftPath.drawDelay(canvas);
        centerRightPath.drawDelay(canvas);
        secondPath.drawDelay(canvas);
        secondRightPath.drawDelay(canvas);
        firstPath.drawDelay(canvas);

    }

    /**
     * 计算波形函数中x对应的y值
     *
     * @param mapX   换算到[-2,2]之间的x值
     * @param offset 偏移量
     * @return
     */
    private double calcValue(float mapX, float offset) {
        int keyX = (int) (mapX * 1000);
//        offset %= 2;
        double sinFunc = Math.sin(1 * Math.PI * mapX - offset * Math.PI);
        double recessionFunc;
        if (recessionFuncs.indexOfKey(keyX) >= 0) {
            recessionFunc = recessionFuncs.get(keyX);
        } else {
            recessionFunc = Math.pow(4 / (4 + Math.pow(mapX, 4)), 2.5);
            recessionFuncs.put(keyX, recessionFunc);
        }
        return sinFunc * recessionFunc;
    }

    SparseArray<Double> recessionFuncs = new SparseArray<>();

    private final float DEFAULT_SEGMENT_LENGTH = 10.0f;

    /**
     * 切分线段，显示由细到粗
     * @param path
     * @param canvas
     */
    private void getPaths(Path path, Canvas canvas){
        PathMeasure pm = new PathMeasure(path, false);
        float length = pm.getLength();
        float segmentSize = (float) Math.ceil(length / DEFAULT_SEGMENT_LENGTH);
        Path ps = null;

        for (int i = 1; i <= segmentSize; i++) {
            ps = new Path();

            if (i == 0) {
                pm.getSegment(0, length, ps, true);
                paint.setStrokeWidth(0);
                //draw()
            } else {
                pm.getSegment((i - 1) * DEFAULT_SEGMENT_LENGTH, Math.min(i * DEFAULT_SEGMENT_LENGTH, length), ps,  true);
                paint.setStrokeWidth((float) i / segmentSize * STORKE_WIDTH);//线段宽度
            }
            paint.setColor(getResources().getColor(R.color.colorpurple));
            canvas.drawPath(ps, paint);
        }
    }

    private final int DEFAULT_SEGMENT_NUM = 15;

    /**
     * 切分线段，显示由细到粗
     * @param path
     * @param canvas
     */
    private void getPaths1(Path path, Canvas canvas) {
        PathMeasure pm = new PathMeasure(path, false);
        float length = pm.getLength();

        float itemLength = length / DEFAULT_SEGMENT_NUM;
        Path ps = null;

        for (int i = 1; i <= DEFAULT_SEGMENT_NUM; i++) {
            ps = new Path();

            if (i == 0) {
                pm.getSegment(0, length, ps, true);
                paint.setStrokeWidth(0);
                //draw()
            } else {
                pm.getSegment((i - 1) * itemLength, Math.min(i * itemLength, length), ps,  true);
                paint.setStrokeWidth((float) i / DEFAULT_SEGMENT_NUM * STORKE_WIDTH);//线段宽度
            }
            paint.setColor(getResources().getColor(R.color.regionStartColor));
            canvas.drawPath(ps, paint);
        }
    }

    /**
     *画一个菱形，模仿两边尖头线段
     * @param canvas
     */
    private void getPath2(Canvas canvas, float pathStart, Shader shader) {
        Path path = new Path();

        path.moveTo(pathStart, centerHeight);
        path.lineTo(pathStart + lineLength, centerHeight - STORKE_WIDTH);
        path.lineTo(pathStart + 2 * lineLength, centerHeight);
        path.lineTo(pathStart + lineLength, centerHeight + STORKE_WIDTH);
        path.close();

        paint.setShader(shader);

        canvas.drawPath(path, paint);
    }

    /**
     * 使用透明度渐变效果实现两边线段
     * @param canvas
     * @param pathStart
     * @param shader
     */
    private void getPath3(Canvas canvas, float pathStart, Shader shader, Path path) {
        path.moveTo(pathStart, centerHeight);
        path.lineTo(pathStart + 2 * lineLength, centerHeight);

        paint.setShader(shader);

        canvas.drawPath(path, paint);
    }

    private void clear(Canvas canvas) {
        clearScreenPaint.setXfermode(clearXfermode);
        canvas.drawPaint(clearScreenPaint);
        clearScreenPaint.setXfermode(srcXfermode);
    }

    @Override
    protected void onPause(Canvas canvas) {
        clear(canvas);
    }
}

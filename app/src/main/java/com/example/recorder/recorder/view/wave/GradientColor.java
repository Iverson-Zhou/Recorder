package com.example.recorder.recorder.view.wave;

import android.content.res.Resources;

import com.example.recorder.recorder.R;

/**
 * Created by Administrator on 2017/12/28.
 */

public class GradientColor {
    public static int[] colorsMainLine;//主线
    public static float[] positionsMainLine;//主线颜色在path上的显示比例

    public static int[] colorsSecondLeftLine;//第二条左边线
    public static float[] positionsSecondLeftLine;

    public static  int[] colorsSecondRightLine;//第二条右边线
    public static float[] positionsSecondRightLine;

    public static int[] colorsCenterLeftLine;//第三条左边线
    public static float[] positionsCenterLeftLine;

    public static int[] colorsCenterRigthLine;//第三条右边线
    public static float[] positionsCenterRigthLine;

    public static int[] colorsleftLine;
    public static float[] positionsLeftLine;

    public static int[] colorsrightLine;
    public static float[] positionsRightLine;

    public static void init(Resources resources) {
//        myColor(resources);
        uiColor(resources);

        colorsleftLine = new int[]{resources.getColor(R.color.transparent),
                resources.getColor(R.color.colordarkblueAlpha),
                resources.getColor(R.color.colordarkblue),
                resources.getColor(R.color.colorpurple),
                resources.getColor(R.color.colorpurpleAlpha),
                resources.getColor(R.color.transparent)};
        positionsLeftLine = new float[] {0.08f, 0.12f, 0.4f, 0.6f, 0.95f, 1.0f};

        colorsrightLine = new int[]{resources.getColor(R.color.transparent),
                resources.getColor(R.color.colorpurpleAlpha),
                resources.getColor(R.color.colorpurple),
                resources.getColor(R.color.colordarkblue),
                resources.getColor(R.color.colordarkblueAlpha),
                resources.getColor(R.color.transparent)};
        positionsRightLine = new float[] {0.05f, 0.12f, 0.5f, 0.7f, 0.95f, 1.0f};

    }

    private static void myColor(Resources resources) {
        colorsMainLine = new int[]{resources.getColor(R.color.transparent),
                resources.getColor(R.color.colorredAlpha),
                resources.getColor(R.color.colorred),
                resources.getColor(R.color.colorpurple),
                resources.getColor(R.color.colordarkblue1),
                resources.getColor(R.color.colorbrightblue)};
        positionsMainLine = new float[]{0.1f, 0.25f, 0.3f, 0.5f, 0.8f, 1.0f};

        colorsSecondLeftLine = new int[]{resources.getColor(R.color.transparent),
                resources.getColor(R.color.colorpurpleAlpha),
                resources.getColor(R.color.colorpurple),
                resources.getColor(R.color.colorbrightblue)};
        positionsSecondLeftLine = new float[]{0.08f, 0.18f, 0.6f, 1.0f};

        colorsSecondRightLine = new int[]{resources.getColor(R.color.transparent),
                resources.getColor(R.color.colorredAlpha),
                resources.getColor(R.color.colorred),
                resources.getColor(R.color.colorpurple),
                resources.getColor(R.color.colorbrightblue)};
        positionsSecondRightLine = new float[]{0.08f, 0.15f, 0.3f, 0.5f, 1.0f};

        colorsCenterLeftLine = new int[]{resources.getColor(R.color.transparent),
                resources.getColor(R.color.colorpurpleAlpha),
                resources.getColor(R.color.colorpurple),
                resources.getColor(R.color.regionEndColor1)};
        positionsCenterLeftLine = new float[]{0.13f, 0.16f, 0.3f, 1.0f};

        colorsCenterRigthLine = new int[]{resources.getColor(R.color.transparent),
                resources.getColor(R.color.colorpurpleAlpha),
                resources.getColor(R.color.colorpurple),
                resources.getColor(R.color.regionEndColor1)};
        positionsCenterRigthLine = new float[]{0.08f, 0.1f, 0.3f, 1.0f};
    }

    private static void uiColor(Resources resources) {
        colorsMainLine = new int[] {resources.getColor(R.color.transparent),
                resources.getColor(R.color.uicolor_darkbluealpha),
                resources.getColor(R.color.uicolor_redalpha),
                resources.getColor(R.color.uicolor_red),
                resources.getColor(R.color.uicolor_purple),
                resources.getColor(R.color.uicolor_brightblue)};
        positionsMainLine = new float[] {0.04f, 0.08f, 0.25f, 0.45f, 0.6f, 1.0f};

        colorsSecondLeftLine = colorsMainLine;
        positionsSecondLeftLine = positionsMainLine;

        colorsSecondRightLine = colorsMainLine;
        positionsSecondRightLine = positionsMainLine;

        colorsCenterLeftLine = colorsMainLine;
        positionsCenterLeftLine = positionsMainLine;

        colorsCenterRigthLine = colorsMainLine;
        positionsCenterRigthLine = positionsMainLine;
    }
}

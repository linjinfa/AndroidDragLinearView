package com.aaron.dragview.demo.util;

/**
 * 用于Toouch的工具类，能够计算两次 {@link #setCurrent(float, float)} 或者
 * {@link #setDown(float, float)} 和最近一次 {@link #setCurrent(float, float)} 之间的距离。
 */
public final class FloatRoute {

    private float downX;
    private float downY;

    private float lastX;
    private float lastY;

    private float currentX;
    private float currentY;

    public FloatRoute() {
    }

    /**
     * 设置手势开始时（或者使用处想要作为初始值的地方）
     */
    public void setDown(float x, float y) {
        downX = x;
        downY = y;

        lastX = x;
        lastY = y;

        currentX = x;
        currentY = y;
    }

    public void reset() {
        setDown(0, 0);
    }

    public void setCurrent(float x, float y) {
        lastX = currentX;
        lastY = currentY;

        currentX = x;
        currentY = y;
    }

    public float getDownX() {
        return downX;
    }

    public float getDownY() {
        return downY;
    }

    public float getLastX() {
        return lastX;
    }

    public float getLastY() {
        return lastY;
    }

    public float getLatestX() {
        return currentX;
    }

    public float getLatestY() {
        return currentY;
    }

    /**
     * 一般在setCurrent之后调用，获取相邻两个setCurrent或者setCurrent和setDown之间的变化值
     *
     * @return x轴的变化值
     */
    public float getDeltaX() {
        return currentX - lastX;
    }

    /**
     * 一般在setCurrent之后调用，获取相邻两个setCurrent或者setCurrent和setDown之间的变化值
     *
     * @return y轴的变化值
     */
    public float getDeltaY() {
        return currentY - lastY;
    }

    /**
     * 获取从setDown开始到最近一次setCurrent之间的变化值
     *
     * @return x轴的变化值
     */
    public float getTotalDeltaX() {
        return currentX - downX;
    }

    /**
     * 获取从setDown开始到最近一次setCurrent之间的变化值
     *
     * @return y轴的变化值
     */
    public float getTotalDeltaY() {
        return currentY - downY;
    }

    @Override
    public String toString() {
        return "FloatRoute [downX=" + downX + ", downY=" + downY + ", lastX="
                + lastX + ", lastY=" + lastY + ", currentX=" + currentX
                + ", currentY=" + currentY + "]";
    }

}

package com.huobi.utils;

public interface Indicatrix {

    /**
     * 计算MACD指标数据
     *
     * @param closePrice--收盘价要从第一天开始
     * @param fastPeriod--日快线移动平均，标准为12
     * @param slowPeriod--日慢线移动平均，标准为26
     * @param signalPeriod--日移动平均，标准为9
     * @param macd--顺序记录macd指标
     *            new double[]
     * @param dea--顺序记录dea指标
     *            new double[]
     * @param diff--顺序记录diff指标
     *            new double[]
     */
    void MACD(Double[] closePrice, Integer fast, Integer slow, Integer signal, Double[] macd, Double[] dea, Double[] diff);

    /**
     * 计算KDJ指标数据
     *
     * @param maxPrice--最高价要从第一天开始
     * @param minPrice--最低价要从第一天开始
     * @param closePrice--收盘价要从第一天开始
     * @param fastK_Period--标准值：9
     * @param slowK_Period--标准值：3
     * @param slowD_Period--标准值：3
     * @param K--顺序记录K指标
     *            new double[]
     * @param D--顺序记录D指标
     *            new double[]
     * @param J--顺序记录J指标
     *            new double[]
     */
    void KDJ(double[] maxPrice, double[] minPrice, double[] closePrice, int fastK_Period, int slowK_Period,
             int slowD_Period, double[] K, double[] D, double[] J);

    /**
     * 计算RSI指标数据
     *
     * @param closePrice--收盘价要从第一天开始
     * @param rsi1_n--计算rsi1指标，标准为：6
     * @param rsi2_n--计算rsi2指标，标准为：12
     * @param rsi3_n--计算rsi3指标，标准为：24
     * @param rsi1--顺序记录rsi1指标
     *            new double[]
     * @param rsi2--顺序记录rsi2指标
     *            new double[]
     * @param rsi3--顺序记录rsi3指标
     *            new double[]
     */
    void RSI(double[] closePrice, int rsi1_n, int rsi2_n, int rsi3_n, double[] rsi1, double[] rsi2, double[] rsi3);

}

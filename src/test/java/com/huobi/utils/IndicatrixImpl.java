package com.huobi.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class IndicatrixImpl implements Indicatrix{
    @Override
    public void MACD(Double[] closePrice, Integer fast, Integer slow, Integer signal, Double[] macd, Double[] dea, Double[] diff) {
        double preEma_12 = 0;
        double preEma_26 = 0;
        double preDEA = 0;

        double ema_12 = 0;
        double ema_26 = 0;

        double fastPeriod = Double.valueOf(new Integer(fast));// 日快线移动平均，标准为12，按照标准即可
        double slowPeriod = Double.valueOf(new Integer(slow));// 日慢线移动平均，标准为26，可理解为天数
        double signalPeriod = Double.valueOf(new Integer(signal));// 日移动平均，标准为9，按照标准即可

        double DEA = 0;
        double DIFF = 0;
        double MACD = 0;
        for (int i = 0; i < closePrice.length; i++) {
            ema_12 = i == 0 ? closePrice[i]
                    : MathCaclateUtil.add(
                    MathCaclateUtil.divide(
                            MathCaclateUtil.multiply(preEma_12, fastPeriod - 1, BigDecimal.ROUND_HALF_UP),
                            fastPeriod + 1, BigDecimal.ROUND_UNNECESSARY),
                    MathCaclateUtil.divide(
                            MathCaclateUtil.multiply(closePrice[i], 2D, BigDecimal.ROUND_HALF_UP),
                            fastPeriod + 1, BigDecimal.ROUND_UNNECESSARY),
                    BigDecimal.ROUND_HALF_UP);// ema_12=preEma_12*(fastPeriod-1)/(fastPeriod+1)+closePrice*2/(fastPeriod+1)

            ema_26 = i == 0 ? closePrice[i]
                    : MathCaclateUtil.add(
                    MathCaclateUtil.divide(
                            MathCaclateUtil.multiply(preEma_26, slowPeriod - 1, BigDecimal.ROUND_HALF_UP),
                            slowPeriod + 1, BigDecimal.ROUND_UNNECESSARY),
                    MathCaclateUtil.divide(
                            MathCaclateUtil.multiply(closePrice[i], 2D, BigDecimal.ROUND_HALF_UP),
                            slowPeriod + 1, BigDecimal.ROUND_UNNECESSARY),
                    BigDecimal.ROUND_HALF_UP);// ema_26=preEma_26*(slowPeriod-1)/(slowPeriod+1)+closePrice*2/(slowPeriod+1)

            DIFF = i == 0 ? 0 : MathCaclateUtil.subtract(ema_12, ema_26, BigDecimal.ROUND_HALF_UP);// Diff=ema_12-ema_26

            DEA = i == 0 ? 0
                    : MathCaclateUtil.add(
                    MathCaclateUtil.divide(
                            MathCaclateUtil.multiply(preDEA, signalPeriod - 1, BigDecimal.ROUND_HALF_UP),
                            signalPeriod + 1, BigDecimal.ROUND_UNNECESSARY),
                    MathCaclateUtil.divide(MathCaclateUtil.multiply(DIFF, 2D, BigDecimal.ROUND_HALF_UP),
                            signalPeriod + 1, BigDecimal.ROUND_UNNECESSARY),
                    BigDecimal.ROUND_HALF_UP);// DEA=preDEA*(signalPeriod-1)/(signalPeriod+1)+Diff*2/(signalPeriod+1)

            MACD = i == 0 ? 0
                    : MathCaclateUtil.multiply(2D, MathCaclateUtil.subtract(DIFF, DEA, BigDecimal.ROUND_HALF_UP),
                    BigDecimal.ROUND_HALF_UP);// MACD=2×(Diff－DEA)

            preEma_12 = ema_12;
            preEma_26 = ema_26;
            preDEA = DEA;

            macd[i] = MACD;
            dea[i] = DEA;
            diff[i] = DIFF;
        }
    }

    @Override
    public void KDJ(double[] maxPrice, double[] minPrice, double[] closePrice, int fastK, int slowK, int slowD,
                    double[] K_R, double[] D_R, double[] J_R) {
        List<Double> highestPriceList = new ArrayList<>();
        List<Double> lowestPriceList = new ArrayList<>();

        List<Double> highestPriceList_temp = new ArrayList<>();
        List<Double> lowestPriceList_temp = new ArrayList<>();

        double RSV = 0;

        double fastK_Period = fastK;// 标准值为9
        double slowK_Period = Double.valueOf(new Integer(slowK));// 标准值为3
        double slowD_Period = Double.valueOf(new Integer(slowD));// 标准值为3

        double preK = 0;
        double preD = 0;

        double K = 0;
        double D = 0;
        double J = 0;
        for (int i = 0; i < closePrice.length; i++) {
            if (highestPriceList.size() == fastK_Period) {
                highestPriceList.remove(0);
                lowestPriceList.remove(0);
            } else if (highestPriceList.size() == 0) {
                highestPriceList.add(maxPrice[i]);
                lowestPriceList.add(minPrice[i]);
            }
            highestPriceList.add(maxPrice[i]);
            lowestPriceList.add(minPrice[i]);

            highestPriceList_temp = new ArrayList<>();
            lowestPriceList_temp = new ArrayList<>();

            highestPriceList_temp.addAll(highestPriceList);
            lowestPriceList_temp.addAll(lowestPriceList);

            highestPriceList_temp.sort(Double::compareTo);
            lowestPriceList_temp.sort(Double::compareTo);

            RSV = MathCaclateUtil.multiply(MathCaclateUtil.divide(
                    MathCaclateUtil.subtract(closePrice[i], lowestPriceList_temp.get(0), BigDecimal.ROUND_HALF_UP),
                    MathCaclateUtil.subtract(highestPriceList_temp.get(highestPriceList_temp.size() - 1),
                            lowestPriceList_temp.get(0), BigDecimal.ROUND_HALF_UP),
                    BigDecimal.ROUND_UNNECESSARY), 100D, BigDecimal.ROUND_HALF_UP);// （今日收盘价－9日内最低价）÷（9日内最高价－9日内最低价）×100

            // 如果无前一日的K、D值
            K = MathCaclateUtil.divide(MathCaclateUtil.add(RSV,
                    MathCaclateUtil.multiply(2D, preK, BigDecimal.ROUND_HALF_UP), BigDecimal.ROUND_HALF_UP),
                    slowK_Period, BigDecimal.ROUND_UNNECESSARY);// （当日RSV值+2×前一日K值）÷3

            D = MathCaclateUtil.divide(MathCaclateUtil.add(K,
                    MathCaclateUtil.multiply(2D, preD, BigDecimal.ROUND_HALF_UP), BigDecimal.ROUND_HALF_UP),
                    slowD_Period, BigDecimal.ROUND_UNNECESSARY);// （当日K值+2×前一日D值）÷3

            J = MathCaclateUtil.subtract(MathCaclateUtil.multiply(3D, K, BigDecimal.ROUND_HALF_UP),
                    MathCaclateUtil.multiply(2D, D, BigDecimal.ROUND_HALF_UP), BigDecimal.ROUND_HALF_UP);// 3K－2D

            preK = K;
            preD = D;

            K_R[i] = K;
            D_R[i] = D;
            J_R[i] = J;
        }
    }

    @Override
    public void RSI(double[] closePrice, int rsi1_n, int rsi2_n, int rsi3_n, double[] rsi1, double[] rsi2,
                    double[] rsi3) {
        double pp_6;// 用于计算rsi数据
        double np_6;
        double pp_12;
        double np_12;
        double pp_24;
        double np_24;
        double prepp_6 = 0;// 用于计算rsi数据
        double prenp_6 = 0;
        double prepp_12 = 0;
        double prenp_12 = 0;
        double prepp_24 = 0;
        double prenp_24 = 0;

        double upsanddowns;
        double n1 = Double.valueOf(new Integer(rsi1_n));// 标准值为6
        double n2 = Double.valueOf(new Integer(rsi2_n));// 标准值为12
        double n3 = Double.valueOf(new Integer(rsi3_n));// 标准值为24
        double num_100 = 100D;

        double RSI1 = 0;
        double RSI2 = 0;
        double RSI3 = 0;
        for (int i = 0; i < closePrice.length; i++) {
            if (i == 0) {
                continue;
            }
            upsanddowns = closePrice[i] - closePrice[i - 1];

            pp_6 = MathCaclateUtil.add(
                    MathCaclateUtil.divide(MathCaclateUtil.multiply(prepp_6, n1 - 1, BigDecimal.ROUND_HALF_UP), n1,
                            BigDecimal.ROUND_UNNECESSARY),
                    MathCaclateUtil.divide(upsanddowns >= 0 ? upsanddowns : 0, n1, BigDecimal.ROUND_UNNECESSARY),
                    BigDecimal.ROUND_HALF_UP);// prepp_6*(6-1)/6+(upsanddowns>=0?upsanddowns:0)/6
            np_6 = MathCaclateUtil.add(
                    MathCaclateUtil.divide(MathCaclateUtil.multiply(prenp_6, n1 - 1, BigDecimal.ROUND_HALF_UP), n1,
                            BigDecimal.ROUND_UNNECESSARY),
                    MathCaclateUtil.divide(upsanddowns >= 0 ? 0 : upsanddowns, n1, BigDecimal.ROUND_UNNECESSARY),
                    BigDecimal.ROUND_HALF_UP);// prenp_6*(6-1)/6+(upsanddowns>=0?0:upsanddowns)/6
            RSI1 = MathCaclateUtil.divide(MathCaclateUtil.multiply(num_100, pp_6, BigDecimal.ROUND_HALF_UP),
                    MathCaclateUtil.add(pp_6, -np_6, BigDecimal.ROUND_HALF_UP), BigDecimal.ROUND_UNNECESSARY);// 100*pp_6/(pp_6-np_6)

            rsi1[i] = RSI1;
            prepp_6 = pp_6;
            prenp_6 = np_6;

            pp_12 = MathCaclateUtil.add(
                    MathCaclateUtil.divide(MathCaclateUtil.multiply(prepp_12, n2 - 1, BigDecimal.ROUND_HALF_UP), n2,
                            BigDecimal.ROUND_UNNECESSARY),
                    MathCaclateUtil.divide(upsanddowns >= 0 ? upsanddowns : 0, n2, BigDecimal.ROUND_UNNECESSARY),
                    BigDecimal.ROUND_HALF_UP);// prepp_12*(12-1)/12+(upsanddowns>=0?upsanddowns:0)/12;
            np_12 = MathCaclateUtil.add(
                    MathCaclateUtil.divide(MathCaclateUtil.multiply(prenp_12, n2 - 1, BigDecimal.ROUND_HALF_UP), n2,
                            BigDecimal.ROUND_UNNECESSARY),
                    MathCaclateUtil.divide(upsanddowns >= 0 ? 0 : upsanddowns, n2, BigDecimal.ROUND_UNNECESSARY),
                    BigDecimal.ROUND_HALF_UP);// prenp_12*(12-1)/12+(upsanddowns>=0?0:upsanddowns)/12;
            RSI2 = MathCaclateUtil.divide(MathCaclateUtil.multiply(num_100, pp_12, BigDecimal.ROUND_HALF_UP),
                    MathCaclateUtil.add(pp_12, -np_12, BigDecimal.ROUND_HALF_UP), BigDecimal.ROUND_UNNECESSARY);// 100*pp_12/(pp_12-np_12);

            rsi2[i] = RSI2;
            prepp_12 = pp_12;
            prenp_12 = np_12;

            pp_24 = MathCaclateUtil.add(
                    MathCaclateUtil.divide(MathCaclateUtil.multiply(prepp_24, n3 - 1, BigDecimal.ROUND_HALF_UP), n3,
                            BigDecimal.ROUND_UNNECESSARY),
                    MathCaclateUtil.divide(upsanddowns >= 0 ? upsanddowns : 0, n3, BigDecimal.ROUND_UNNECESSARY),
                    BigDecimal.ROUND_HALF_UP);// prepp_24*(24-1)/24+(upsanddowns>=0?upsanddowns:0)/24;
            np_24 = MathCaclateUtil.add(
                    MathCaclateUtil.divide(MathCaclateUtil.multiply(prenp_24, n3 - 1, BigDecimal.ROUND_HALF_UP), n3,
                            BigDecimal.ROUND_UNNECESSARY),
                    MathCaclateUtil.divide(upsanddowns >= 0 ? 0 : upsanddowns, n3, BigDecimal.ROUND_UNNECESSARY),
                    BigDecimal.ROUND_HALF_UP);// prenp_24*(24-1)/24+(upsanddowns>=0?0:upsanddowns)/24;
            RSI3 = MathCaclateUtil.divide(MathCaclateUtil.multiply(num_100, pp_24, BigDecimal.ROUND_HALF_UP),
                    MathCaclateUtil.add(pp_24, -np_24, BigDecimal.ROUND_HALF_UP), BigDecimal.ROUND_UNNECESSARY);// 100*pp_24/(pp_24-np_24);

            rsi3[i] = RSI3;
            prepp_24 = pp_24;
            prenp_24 = np_24;
        }
    }
}

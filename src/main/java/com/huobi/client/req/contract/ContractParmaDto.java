package com.huobi.client.req.contract;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractParmaDto {
    private List<Double> closeList;
    private List<Double> highList;
    private List<Double> lowList;
    private List<Double> volumeList;

    //主程序循环时间
    private Long sleepMillis;
    //当前KLine周期
    private String periodTime;
    // 自定义-判断是否持仓（有持仓则不开仓）
    private Boolean haveOrder;
    // 自定义-当前持仓方向 "open":开 "close":平
    private String havaOrderDirection;
    // 自定义-当前持仓张数(只做日志输出，不做交易)
    private Long volume;
    // 自定义-容错系数
    private Double dynamicNum;
    // 自定义-本次是否下单
    private Boolean currentTakeOrder = false;
    // 自定义 - 短周期最高价
    private double high5Price;
    // 自定义 - 短周期最高最低价
    private double low5Price;
    // 自定义 - 30日最高最低价
    double high30Price;
    // 自定义 - 30日最高最低价
    double low30Price;
    //true:当前为趋势 ， false:当前为波段
    private Boolean trendType;
    //当前收盘价
    private Double currentClosePrice;
    //上一次收盘价
    private Double lastClosePrice;
    //当前最高价
    private Double currentHighPrice;
    //上一次最高价
    private Double lastHighPrice;
    //当前最低价
    private Double currentLowPrice;
    //上一次最低价
    private Double lastLowPrice;
    //kline长周期定义
    private Integer longLineCycle;
    //kline短周期定义
    private Integer shortLineCycle;
    //当前实时价格(用于实时查询价格)
    private BigDecimal currRealPrice;
    //上次实时价格(用于实时查询价格)
    private BigDecimal lastRealPrice;
    //以下为macd指标
    private Double[] difArr;
    private Double[] deaArr;
    private Double[] macdArr;
    //波段止损点（upBoll差值）
    private Double upStopLossPoint;
    //波段止损点（lowBoll差值）
    private Double lowStopLossPoint;
    //K线回测起止时间(时间戳)
    private Long startTime;
    private Long endTime;
    //OBV
    private List<BigDecimal> obvList;
    //Boll
    private double[][] bollArr;
    //EMA
    private Integer EmaValue;
    private double[] emaArr;
}

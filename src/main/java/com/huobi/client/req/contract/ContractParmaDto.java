package com.huobi.client.req.contract;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractParmaDto {
    private List<Double> closeList;
    private List<Double> highList;
    private List<Double> lowList;

    //当前KLine周期
    private String periodTime;
    // 自定义-判断是否持仓（有持仓则不开仓）
    private Boolean haveOrder;
    // 自定义-当前持仓方向 "open":开 "close":平
    private String havaOrderDirection;
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
}

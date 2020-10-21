package com.huobi.client.req.contract;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ContractPlaceOrderRequest {
    // 	string 	true 	合约代码,支持大小写,"BTC-USD"
    private String contractCode;
    // 	long 	false 	客户自己填写和维护，必须为数字, 请注意必须小于等于9223372036854775807
    private Long clientOrderId;
    // 	decimal 	false 	价格
    private BigDecimal price;
    // 	long 	true 	委托数量(张)
    private Long volume;
    // 	string 	true 	"buy":买 "sell":卖
    private String direction;
    // 	string 	true 	"open":开 "close":平
    private String offset;
    // 	int 	true 	杠杆倍数[“开仓”若有10倍多单，就不能再下20倍多单;首次使用高倍杠杆(>20倍)，请使用主账号登录web端同意高倍杠杆协议后，才能使用接口下高倍杠杆(>20倍)]
    private Integer leverRate;

    //  订单报价类型 "limit":限价 "opponent":对手价 "post_only":只做maker单,post only下单只受用户持仓数量限制,
    // 	optimal_5：最优5档、optimal_10：最优10档、optimal_20：最优20档，"fok":FOK订单，"ioc":IOC订单, opponent_ioc"： 对手价-IOC下单，
    // 	"optimal_5_ioc"：最优5档-IOC下单，"optimal_10_ioc"：最优10档-IOC下单，"optimal_20_ioc"：最优20档-IOC下单,
    // 	"opponent_fok"：对手价-FOK下单，"optimal_5_fok"：最优5档-FOK下单，"optimal_10_fok"：最优10档-FOK下单，"optimal_20_fok"：最优20档-FOK下单
    private String orderPriceType;

    // 自定义-判断是否持仓（有持仓则不开仓）
    private Boolean haveOrder;
    // 自定义-当前持仓方向 "open":开 "close":平
    private String havaOrderDirection;
    // 自定义-容错系数
    private Double dynamicNum;
    // 自定义-本次是否下单
    private Boolean currentTakeOrder = false;
    // 自定义 - 5日最高最低价
    private double high5Price;
    // 自定义 - 5日最高最低价
    private double low5Price;
    // 自定义 - 30日最高最低价
    double high30Price;
    // 自定义 - 30日最高最低价
    double low30Price;
    //true:当前为趋势 ， false:当前为波段
    private Boolean trendType;
}

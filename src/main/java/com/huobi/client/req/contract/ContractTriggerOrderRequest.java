package com.huobi.client.req.contract;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ContractTriggerOrderRequest {
    private String contractCode;// 	true 	string 	合约代码 	BTC-USD
    private String triggerType;// 	true 	string 	触发类型： ge大于等于(触发价比最新价大)；le小于(触发价比最新价小)
    private BigDecimal triggerPrice;// 	true 	decimal 	触发价，精度超过最小变动单位会报错
    private BigDecimal orderPrice;// 	false 	decimal 	委托价，精度超过最小变动单位会报错
    private String orderPriceType;// 	false 	string 	委托类型： 不填默认为limit; 限价：limit ，最优5档：optimal_5，最优10档：optimal_10，最优20档：optimal_20
    private Long volume;// 	true 	decimal 	委托数量(张)
    private String direction;// 	true 	string 	buy:买 sell:卖
    private String offset;// 	true 	string 	open:开 close:平
    private Integer leverRate;// 	false 	int 	开仓必须填写，平仓可以不填。杠杆倍数[开仓若有10倍多单，就不能再下20倍多单]
}

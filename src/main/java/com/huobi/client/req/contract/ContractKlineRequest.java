package com.huobi.client.req.contract;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ContractKlineRequest {
    //合约代码 仅支持大写， "BTC-USD" ...
    private String contractCode;
    //k线类型
    private String period;
    //K线数量
    private Integer size;
    //开始时间戳
    private Long from;
    //结束时间戳
    private Long to;

}

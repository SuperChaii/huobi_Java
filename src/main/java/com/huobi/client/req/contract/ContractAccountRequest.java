package com.huobi.client.req.contract;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ContractAccountRequest {
    //合约代码 仅支持大写， "BTC-USD" ...
    private String contractCode;

}

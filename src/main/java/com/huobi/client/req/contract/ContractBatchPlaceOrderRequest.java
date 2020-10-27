package com.huobi.client.req.contract;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ContractBatchPlaceOrderRequest {
    private List<ContractPlaceOrderRequest> orders_data;
}

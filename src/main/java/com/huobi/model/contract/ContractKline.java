package com.huobi.model.contract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractKline {
    //      "id": K线id,
    private Long id;
    //      "vol": 成交量(张)，买卖双边成交量之和,
    private BigDecimal vol;
    //      "count": 成交笔数,
    private BigDecimal count;
    //      "open": 开盘价,
    private BigDecimal open;
    //      "close": 收盘价,当K线为最晚的一根时，是最新成交价
    private BigDecimal close;
    //      "low": 最低价,
    private BigDecimal low;
    //      "high": 最高价,
    private BigDecimal high;
    //      "amount": 成交量(币), 即 sum(每一笔成交量(张)*单张合约面值/该笔成交价)
    private BigDecimal amount;
}

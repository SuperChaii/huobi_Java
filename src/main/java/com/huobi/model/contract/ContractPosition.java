package com.huobi.model.contract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractPosition {
    private String symbol; // 	true 	string 	品种代码 	"BTC","ETH"...
    private String contract_code; // 	true 	string 	合约代码 	"BTC-USD" ...
    private BigDecimal volume; // 	true 	decimal 	持仓量
    private BigDecimal available; // 	true 	decimal 	可平仓数量
    private BigDecimal frozen; // 	true 	decimal 	冻结数量
    private BigDecimal cost_open; // 	true 	decimal 	开仓均价
    private BigDecimal cost_hold; // 	true 	decimal 	持仓均价
    private BigDecimal profit_unreal; // 	true 	decimal 	未实现盈亏
    private BigDecimal profit_rate;// 	true 	decimal 	收益率
    private BigDecimal profit;// 	true 	decimal 	收益
    private BigDecimal position_margin;// 	true 	decimal 	持仓保证金
    private BigDecimal lever_rate;// 	true 	int 	杠杠倍数
    private String direction;// 	true 	string 	"buy":买 "sell":卖
    private BigDecimal last_price;// 	true 	decimal 	最新价
}

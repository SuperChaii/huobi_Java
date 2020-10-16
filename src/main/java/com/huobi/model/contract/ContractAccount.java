package com.huobi.model.contract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractAccount {
    // 	true 	string 	品种代码 	"BTC","ETH"...
    private String symbol;
    // 	true 	string 	合约代码 	"BTC-USD" ...
    private String contract_code;
    // 	true 	decimal 	账户权益
    private BigDecimal margin_balance;
    // 	true 	decimal 	静态权益
    private BigDecimal margin_static;
    // 	true 	decimal 	持仓保证金（当前持有仓位所占用的保证金）
    private BigDecimal margin_position;
    // 	true 	decimal 	冻结保证金
    private BigDecimal margin_frozen;
    // 	true 	decimal 	可用保证金
    private BigDecimal margin_available;
    // 	true 	decimal 	已实现盈亏
    private BigDecimal profit_real;
    // 	true 	decimal 	未实现盈亏
    private BigDecimal profit_unreal;
    // 	true 	decimal 	保证金率
    private BigDecimal risk_rate;
    // 	true 	decimal 	预估强平价
    private BigDecimal liquidation_price;
    // 	true 	decimal 	可划转数量
    private BigDecimal withdraw_available;
    // 	true 	decimal 	杠杠倍数
    private BigDecimal lever_rate;
    //  true 	decimal 	调整系数
    private BigDecimal adjust_factor;
}

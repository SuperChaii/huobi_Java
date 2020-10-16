package com.huobi.model.account;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ContractAccount {

  private String symbol;
  private String contract_code;
  private BigDecimal contract_size;
  private BigDecimal price_tick;
  private String settlement_date;
  private String create_date;
  private Integer contract_status;
}

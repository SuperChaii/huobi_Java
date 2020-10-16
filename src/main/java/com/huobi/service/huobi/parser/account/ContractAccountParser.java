package com.huobi.service.huobi.parser.account;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.huobi.model.account.Account;
import com.huobi.model.account.ContractAccount;
import com.huobi.service.huobi.parser.HuobiModelParser;

import java.util.ArrayList;
import java.util.List;

public class ContractAccountParser implements HuobiModelParser<ContractAccount> {

  @Override
  public ContractAccount parse(JSONObject json) {
    ContractAccount account = json.toJavaObject(ContractAccount.class);
    account.setSymbol(json.getString("symbol"));
    account.setContract_code(json.getString("contract_code"));
    account.setContract_size(json.getBigDecimal("contract_size"));
    account.setPrice_tick(json.getBigDecimal("price_tick"));
    account.setCreate_date(json.getString("create_date"));
    account.setContract_status(json.getInteger("contract_status"));
    account.setSettlement_date(json.getString("settlement_date"));

    return account;
  }

  @Override
  public ContractAccount parse(JSONArray json) {
    return null;
  }

  @Override
  public List<ContractAccount> parseArray(JSONArray jsonArray) {
    List<ContractAccount> accountList = new ArrayList<>(jsonArray.size());
    for (int i=0;i<jsonArray.size();i++) {
      JSONObject jsonObject = jsonArray.getJSONObject(i);
      accountList.add(parse(jsonObject));
    }

    return accountList;
  }
}

package com.huobi.client;

import com.alibaba.fastjson.JSONObject;
import com.huobi.client.req.contract.ContractAccountRequest;
import com.huobi.client.req.contract.ContractKlineRequest;
import com.huobi.client.req.contract.ContractPlaceOrderRequest;
import com.huobi.constant.Options;
import com.huobi.constant.enums.ExchangeEnum;
import com.huobi.exception.SDKException;
import com.huobi.model.contract.ContractKline;
import com.huobi.service.huobi.HuobiContractService;

import java.util.List;

public interface ContractClient {

  List<ContractKline> getContractKline(ContractKlineRequest request);

  String getContractInfo();

  JSONObject getContractAccountInfo(ContractAccountRequest request);

  static ContractClient create(Options options) {

    if (options.getExchange().equals(ExchangeEnum.HUOBI)) {
      return new HuobiContractService(options);
    }
    throw new SDKException(SDKException.INPUT_ERROR, "Unsupport Exchange.");
  }

  JSONObject getContractPosition(ContractAccountRequest build);

  JSONObject placeOrder(ContractPlaceOrderRequest build);
}

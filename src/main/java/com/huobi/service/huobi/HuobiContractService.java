package com.huobi.service.huobi;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.huobi.client.ContractClient;
import com.huobi.client.req.contract.ContractAccountRequest;
import com.huobi.client.req.contract.ContractKlineRequest;
import com.huobi.client.req.contract.ContractPlaceOrderRequest;
import com.huobi.constant.Options;
import com.huobi.constant.enums.AccountTypeEnum;
import com.huobi.model.account.Account;
import com.huobi.model.contract.ContractKline;
import com.huobi.service.huobi.connection.HuobiRestConnection;
import com.huobi.service.huobi.signature.UrlParamsBuilder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HuobiContractService implements ContractClient {

    //获取K线数据
    public static final String GET_SWAP_MARKET_KLINE_PATH = "/swap-ex/market/history/kline";
    //获取合约详情
    public static final String GET_CONTRACT_INFO_PATH = "/swap-api/v1/swap_contract_info";
    //获取合约账户详情
    public static final String POST_CONTRACT_ACCOUNT_INFO_PATH = "/swap-api/v1/swap_account_info";
    //获取合约持仓信息
    public static final String POST_CONTRACT_POSITION_PATH = "/swap-api/v1/swap_position_info";
    //合约下单
    public static final String POST_CONTRACT_ORDER = "/swap-api/v1/swap_order";

    private Map<AccountTypeEnum, Account> accountMap = new ConcurrentHashMap<>();
    private Map<String, Account> marginAccountMap = new ConcurrentHashMap<>();

    private Options options;

    private HuobiRestConnection restConnection;

    public HuobiContractService(Options options) {
        this.options = options;
        this.restConnection = new HuobiRestConnection(options);
    }


    @Override
    public List<ContractKline> getContractKline(ContractKlineRequest request) {
        JSONObject json = restConnection.executeGetWithSignature(GET_SWAP_MARKET_KLINE_PATH,
                UrlParamsBuilder.build()
                        .putToUrl("contract_code", request.getContractCode())
                        .putToUrl("period", request.getPeriod())
                        .putToUrl("size", request.getSize())
        );
        String dataJson = json.get("data").toString();
        return JSON.parseArray(dataJson, ContractKline.class);
    }

    @Override
    public String getContractInfo() {
        return restConnection.executeGetString(options.getRestHost().concat(GET_CONTRACT_INFO_PATH), UrlParamsBuilder.build());
    }

    @Override
    public JSONObject getContractAccountInfo(ContractAccountRequest request) {
        return restConnection.executePostWithSignature(POST_CONTRACT_ACCOUNT_INFO_PATH,
                UrlParamsBuilder.build().putToPost("contract_code", request.getContractCode()));
    }

    @Override
    public JSONObject getContractPosition(ContractAccountRequest request) {
        return restConnection.executePostWithSignature(POST_CONTRACT_POSITION_PATH,
                UrlParamsBuilder.build().putToPost("contract_code", request.getContractCode()));
    }

    @Override
    public JSONObject placeOrder(ContractPlaceOrderRequest request) {
        return restConnection.executePostWithSignature(POST_CONTRACT_ORDER,
                UrlParamsBuilder.build()
                        .putToPost("contract_code",request.getContractCode())
                        .putToPost("volume",request.getVolume())
                        .putToPost("direction",request.getDirection())
                        .putToPost("offset",request.getOffset())
                        .putToPost("lever_rate",request.getLeverRate())
                        .putToPost("order_price_type",request.getOrderPriceType())
        );
    }
}

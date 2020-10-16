package com.huobi;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.huobi.client.ContractClient;
import com.huobi.client.req.contract.ContractAccountRequest;
import com.huobi.client.req.contract.ContractKlineRequest;
import com.huobi.client.req.contract.ContractTakeOrderRequest;
import com.huobi.constant.HuobiOptions;
import com.huobi.constant.enums.CandlestickIntervalEnum;
import com.huobi.model.contract.ContractAccount;
import com.huobi.model.contract.ContractKline;
import com.huobi.model.contract.ContractPosition;
import com.huobi.utils.IndicatrixImpl;

import java.util.ArrayList;
import java.util.List;

public class Test {

    public static void main(String[] args) {
        final String CONTRACTCODE = "BTC-USD";
        //每次交易量(张)
        Long volume = 1L;
        //方向 buy":买 "sell":卖
        String direction = "buy";
        //"open":开 "close":平
        String offset = "open";
        //杠杆倍数
        final Integer leverRate = 5;
        //订单报价类型
        final String orderPriceType = "opponent";

        //趋势多头为1， 空头为0
        Boolean trendFlag = false;
        Double lastDif = null;
        Double lastDea = null;
        Double lastMacd = null;
        List<Double> closeList;
        IndicatrixImpl impl = new IndicatrixImpl();
        ContractAccount contractAccount;
        ContractPosition contractPosition;


        //获取合约客户端
        ContractClient contractService = ContractClient.create(HuobiOptions.builder()
                .restHost("https://api.hbdm.com")
                .apiKey(Constants.API_KEY)
                .secretKey(Constants.SECRET_KEY)
                .build());

        //获取账户余额
//        contractAccount = getContractAccount(CONTRACTCODE, contractService);
//        if (contractAccount != null) {
//            System.out.println(contractAccount);
//            System.out.println("当前可用保证金：" + contractAccount.getMargin_available());
//        }

        //获取目前持仓
//        contractPosition = getContractPosition(CONTRACTCODE, contractService);
//        if (contractPosition != null) {
//            System.out.println(contractPosition);
//            System.out.println("当前持仓量：" + contractPosition.getVolume() + "持仓均价:" + contractPosition.getCost_hold());
//        }

        //合约下单
        takeOrder(CONTRACTCODE, contractService, volume, direction, offset, leverRate, orderPriceType);


        //获取收盘价，根据K线图
        //closeList = getCloseListByKLine(contractService, CONTRACTCODE, CandlestickIntervalEnum.MIN1, 50);

//        Double[] macdArr = new Double[closeList.size()];
//        Double[] deaArr = new Double[closeList.size()];
//        Double[] difArr = new Double[closeList.size()];
        //获取MACD数组
        //impl.MACD(closeList.toArray(new Double[closeList.size()]), 12, 26, 9, macdArr, deaArr, difArr);
        //getMacdArray(trendFlag, lastDif, lastDea, lastMacd, closeList, macdArr, deaArr, difArr);

    }

    private static void takeOrder(String contractcode, ContractClient contractService, Long volume,
                                  String direction, String offset, Integer leverRate, String orderPriceType) {
        JSONObject json = contractService.placeOrder(ContractTakeOrderRequest.builder()
                .contractCode(contractcode)
                .volume(volume)
                .direction(direction)
                .offset(offset)
                .leverRate(leverRate)
                .orderPriceType(orderPriceType)
                .build());
        System.out.println("已下单：" + json);
    }

    private static ContractPosition getContractPosition(String CONTRACTCODE, ContractClient contractService) {
        JSONObject positionJson = contractService.getContractPosition(ContractAccountRequest.builder().contractCode(CONTRACTCODE).build());
        String positionDataJson = positionJson.getString("data");
        List<ContractPosition> positionList = JSONArray.parseArray(positionDataJson, ContractPosition.class);
        for (ContractPosition position : positionList) {
            if (CONTRACTCODE.equals(position.getContract_code())) {
                return position;
            }
        }
        return null;
    }

    private static ContractAccount getContractAccount(String CONTRACTCODE, ContractClient contractService) {
        JSONObject accountJson = contractService.getContractAccountInfo(ContractAccountRequest.builder().contractCode(CONTRACTCODE).build());
        // - 遍历找到指定contractCode
        String accountDataJson = accountJson.getString("data");
        List<ContractAccount> contractAccountList = JSONArray.parseArray(accountDataJson, ContractAccount.class);
        for (ContractAccount acc : contractAccountList) {
            if (CONTRACTCODE.equals(acc.getContract_code())) {
                return acc;
            }
        }
        return null;
    }

    private static void getMacdArray(Boolean trendFlag, Double lastDif, Double lastDea, Double lastMacd, List<Double> closeList, Double[] macdArr, Double[] deaArr, Double[] difArr) {
        for (int i = 0; i < closeList.size(); i++) {
            double macd = macdArr[i];
            double dif = difArr[i];
            double dea = deaArr[i];

            //判断上一次是否有值
            if (lastDif != null && lastDea != null && lastMacd != null) {
                //判断 是否交叉
                if (dif > dea && lastDif < lastDea) {
                    trendFlag = true;
                    System.out.println("第" + i + "条KLine出线金叉，转为多头！！！");
                } else if (dif < dea && lastDif > lastDea) {
                    trendFlag = false;
                    System.out.println("第" + i + "条KLine出线死叉，转为空头！！！");
                }
                //判断是否出现背离
                if (dif > lastDif && dea > lastDea && macd < lastMacd) {
                    System.out.println("出现顶部背离，可能转空头，建议平多仓！");
                } else if (dif < lastDif && dea < lastDea && macd > lastMacd) {
                    System.out.println("出现底部背离，可能转多头，建议平空仓！");
                }
            }
            if (trendFlag) {
                System.out.println("---" + i + "目前为---多头！");
            } else {
                System.out.println("---" + i + "目前为---空头！");
            }
            lastMacd = macd;
            lastDif = dif;
            lastDea = dea;
        }
    }

    private static List<Double> getCloseListByKLine(ContractClient contractService, String contractCode, CandlestickIntervalEnum periodEnum, Integer size) {
        ArrayList<Double> list = new ArrayList<>();
        List<ContractKline> contractAccountList = contractService.getContractKline(ContractKlineRequest.builder()
                .contractCode(contractCode)
                .period(periodEnum.getCode())
                .size(size)
                .build());
        contractAccountList.forEach(contract -> {
            //System.out.println("---获取K线图---".concat(contract.toString()));
            list.add(contract.getClose().doubleValue());
        });
        return list;
    }
}

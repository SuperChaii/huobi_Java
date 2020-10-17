package com.huobi;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.huobi.client.ContractClient;
import com.huobi.client.req.contract.ContractAccountRequest;
import com.huobi.client.req.contract.ContractKlineRequest;
import com.huobi.client.req.contract.ContractPlaceOrderRequest;
import com.huobi.constant.HuobiOptions;
import com.huobi.constant.enums.CandlestickIntervalEnum;
import com.huobi.model.contract.ContractAccount;
import com.huobi.model.contract.ContractKline;
import com.huobi.model.contract.ContractPosition;
import com.huobi.utils.IndicatrixImpl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Test {

    public static void main(String[] args) {
        final String CONTRACTCODE = "BTC-USD";
        final Long sleepMillis = 1000L;

        ContractPlaceOrderRequest poRequest = new ContractPlaceOrderRequest();
        poRequest.setVolume(1L);
        poRequest.setDirection("buy");
        poRequest.setOffset("open");
        poRequest.setLeverRate(5);
        poRequest.setOrderPriceType("opponent");
        poRequest.setDynamicNum(0.1);

        //当前可开最大张数
        Integer maxOrders;

        List<Double> closeList;
        IndicatrixImpl impl = new IndicatrixImpl();
        ContractAccount contractAccount;
        ContractPosition contractPosition = null;


        //获取合约客户端
        ContractClient contractService = ContractClient.create(HuobiOptions.builder()
                .restHost("https://api.hbdm.com")
                .apiKey(Constants.API_KEY)
                .secretKey(Constants.SECRET_KEY)
                .build());


        while (true) {
            try {
                //获取收盘价，根据K线图
                closeList = getCloseListByKLine(contractService, CONTRACTCODE, CandlestickIntervalEnum.MIN1, 50);
                //获取目前持仓
                contractPosition = getContractPosition(CONTRACTCODE, contractService);
                if (contractPosition != null) {
                    System.out.println("当前持仓量：" + contractPosition.getVolume().doubleValue() + "---当前持仓方向：" + contractPosition.getDirection());
                    if (!contractPosition.getVolume().equals(BigDecimal.ZERO)) {
                        poRequest.setHaveOrder(true);
                        poRequest.setHavaOrderDirection(contractPosition.getDirection());
                        poRequest.setVolume(contractPosition.getVolume().longValue());
                    } else {
                        poRequest.setHaveOrder(false);
                    }
                } else {
                    poRequest.setHaveOrder(false);
                    //无持仓，获取账户余额，计算开仓张数（当前杠杆下满仓）
                    contractAccount = getContractAccount(CONTRACTCODE, contractService);
//                    if (contractAccount != null) {
//                        System.out.println(CONTRACTCODE + "当前可用保证金：" + contractAccount.getMargin_available());
//                    }
                    maxOrders = contractAccount.getMargin_available()
                            .multiply(new BigDecimal(closeList.get(closeList.size() - 1)))
                            .multiply(new BigDecimal(poRequest.getLeverRate()))
                            .divide(new BigDecimal(100)).intValue() - 1;
//                    System.out.println(getTimeFormat(System.currentTimeMillis())
//                            + "---当前无持仓，正在寻找开仓机会！杠杆为" + poRequest.getLeverRate() + "倍，可开张数:" + maxOrders
//                            + "---lastClose:" + closeList.get(closeList.size() - 2)
//                            + "---currentClose:" + closeList.get(closeList.size() - 1));
                    poRequest.setVolume(maxOrders.longValue());
                }

                Double[] macdArr = new Double[closeList.size()];
                Double[] deaArr = new Double[closeList.size()];
                Double[] difArr = new Double[closeList.size()];
                //获取MACD数组
                impl.MACD(closeList.toArray(new Double[closeList.size()]), 12, 26, 9, macdArr, deaArr, difArr);
                getMacdArray(poRequest, difArr, deaArr, macdArr);
                //合约下单
                if (poRequest.getCurrentTakeOrder()) {
                    takeOrder(CONTRACTCODE, contractService, poRequest.getVolume(), poRequest.getDirection(),
                            poRequest.getOffset(), poRequest.getLeverRate(), poRequest.getOrderPriceType());
                    System.out.println("---currentMACD:" + macdArr[macdArr.length - 1]
                            + "---lastMACD:" + macdArr[macdArr.length - 2]
                            + "---currentDIF:" + difArr[difArr.length - 1]
                            + "---lastDIF:" + difArr[difArr.length - 2]
                            + "---currentDEA:" + deaArr[deaArr.length - 1]
                            + "---lastDEA:" + deaArr[deaArr.length - 2]
                    );
                    System.out.println(getTimeFormat(System.currentTimeMillis()) + "***已成功下单" + poRequest + "张！***" + poRequest.toString());
                }
                poRequest.setCurrentTakeOrder(false);
                try {
                    Thread.sleep(sleepMillis);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static String getTimeFormat(long currentTimeMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(Long.parseLong(String.valueOf(currentTimeMillis))));
    }

    private static void takeOrder(String contractcode, ContractClient contractService, Long volume,
                                  String direction, String offset, Integer leverRate, String orderPriceType) {
        JSONObject json = contractService.placeOrder(ContractPlaceOrderRequest.builder()
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

    private static void getMacdArray(ContractPlaceOrderRequest poRequest, Double[] difArr, Double[] deaArr, Double[] macdArr) {
        double dynamicNum = poRequest.getDynamicNum();
        Double currentDif = null;
        Double currentDea = null;
        Double currentMacd = null;
        Double lastDif = null;
        Double lastDea = null;
        Double lastMacd = null;
        //判断后5根K线有没有交叉
        for (int i = 1; i < 3; i++) {
            currentDif = difArr[difArr.length - i];
            currentDea = deaArr[deaArr.length - i];
            currentMacd = macdArr[macdArr.length - i];
            lastDif = difArr[difArr.length - i - 1];
            lastDea = deaArr[deaArr.length - i - 1];
            lastMacd = macdArr[macdArr.length - i - 1];


            //判断上一次是否有值
            if (lastDif != null && lastDea != null && lastMacd != null) {
                if (poRequest.getHaveOrder()) {
                    //有持仓则只考虑平仓，判断是否出现背离
                    if ("buy".equals(poRequest.getHavaOrderDirection())) {
                        if ((currentDif < lastDif && Math.abs(currentDif - lastDif) > dynamicNum)
                                && (currentMacd < lastMacd && Math.abs(currentMacd - lastDif) > dynamicNum)
                        ) {
                            poRequest.setOffset("close");
                            poRequest.setDirection("sell");
                            System.out.println("出现顶部背离，且容错系数超过" + dynamicNum + "，可能转空头，已将多向平仓！");
                            poRequest.setCurrentTakeOrder(true);
                            return;
                        }
                    } else {
                        if ((currentDif > lastDif && Math.abs(currentDif - lastDif) > dynamicNum)
                                && (currentMacd > lastMacd && Math.abs(currentMacd - lastDif) > dynamicNum)
                        ) {
                            poRequest.setOffset("close");
                            poRequest.setDirection("buy");
                            System.out.println("出现底部部背离 ，且容错系数超过" + dynamicNum + "，可能转多头，已将空向平仓！");
                            poRequest.setCurrentTakeOrder(true);
                            return;
                        }
                    }
                } else {
                    //无持仓只考虑开仓，判断 DIF&DEA 是否交叉
                    if (currentDif > currentDea
                            && lastDif <= lastDea
                            && Math.abs(currentDif - lastDif) > dynamicNum
                            && currentMacd > -0.3
                    ) {
                        poRequest.setOffset("open");
                        poRequest.setDirection("buy");
                        System.out.println("KLine出线金叉，转为多头，已开仓！！！");
                        poRequest.setCurrentTakeOrder(true);
                        return;
                    } else if (currentDif <= currentDea
                            && lastDif > lastDea
                            && Math.abs(currentDif - lastDif) > dynamicNum
                            && currentMacd < 0.3
                    ) {
                        poRequest.setOffset("open");
                        poRequest.setDirection("sell");
                        System.out.println("KLine出线死叉，转为空头，已开仓！！！");
                        poRequest.setCurrentTakeOrder(true);
                        return;
                    }
                }
            }
        }
    }

    private static List<Double> getCloseListByKLine(ContractClient contractService, String
            contractCode, CandlestickIntervalEnum periodEnum, Integer size) {
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

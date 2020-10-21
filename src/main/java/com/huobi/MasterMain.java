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
import com.huobi.utils.quant.QuantIndicators;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

public class MasterMain {

    public static void main(String[] args) {
        final String CONTRACTCODE = "ETH-USD";
        final Long sleepMillis = 2000L;

        ContractPlaceOrderRequest requestDTO = new ContractPlaceOrderRequest();
        QuantIndicators qiUtils = new QuantIndicators();
        requestDTO.setVolume(1L);
        requestDTO.setDirection("buy");
        requestDTO.setOffset("open");
        requestDTO.setLeverRate(100);
        requestDTO.setOrderPriceType("opponent");
        requestDTO.setDynamicNum(0.1);

        Map<String, List<Double>> marketMap;
        List<Double> closeList;
        List<Double> highList;
        List<Double> lowList;

        IndicatrixImpl impl = new IndicatrixImpl();


        //获取合约客户端
        ContractClient contractService = ContractClient.create(HuobiOptions.builder()
                .restHost("https://api.hbdm.com")
                .apiKey(Constants.API_KEY)
                .secretKey(Constants.SECRET_KEY)
                .build());
        //获取持仓情况及最大下单量
        updateHaveOrderAndVolume(CONTRACTCODE, requestDTO, contractService);

        while (true) {
            try {
                //获取收盘价，根据K线图
                marketMap = getMarketPriceListByKLine(contractService, CONTRACTCODE, CandlestickIntervalEnum.MIN60, 50);
                //获取收盘价list计算指标
                closeList = marketMap.get("closeList");
                highList = marketMap.get("highList");
                lowList = marketMap.get("lowList");
                requestDTO.setHigh5Price(highList.subList(highList.size() - 5, highList.size()).stream().filter(Objects::nonNull).max(Comparator.comparingDouble(price -> price)).get());
                requestDTO.setLow5Price(lowList.subList(lowList.size() - 5, lowList.size()).stream().filter(Objects::nonNull).min(Comparator.comparingDouble(price -> price)).get());
                requestDTO.setHigh30Price(highList.subList(highList.size() - 30, highList.size()).stream().filter(Objects::nonNull).max(Comparator.comparingDouble(price -> price)).get());
                requestDTO.setLow30Price(lowList.subList(lowList.size() - 30, lowList.size()).stream().filter(Objects::nonNull).min(Comparator.comparingDouble(price -> price)).get());


                //获取BOLL指标
                Double[] closePrice = closeList.toArray(new Double[closeList.size()]);
                double[] doubleArr = new double[closePrice.length];
                for (int i = 0; i < closePrice.length; i++) {
                    doubleArr[i] = closePrice[i];
                }
                double[][] bollArr = qiUtils.boll(doubleArr);
                //获取EMD指标
                //double[] emaArr = qiUtils.ema(doubleArr, 21);
                //获取MACD指标
                Double[] macdArr = new Double[closeList.size()];
                Double[] deaArr = new Double[closeList.size()];
                Double[] difArr = new Double[closeList.size()];
                impl.MACD(closeList.toArray(new Double[closeList.size()]), 12, 26, 9, macdArr, deaArr, difArr);
                //根据macd & boll & high & low 指标判断是否下单
                getOrderByParams(requestDTO, difArr, deaArr, macdArr, bollArr);
                //合约下单
                if (requestDTO.getCurrentTakeOrder()) {
                    takeOrder(CONTRACTCODE, contractService, requestDTO.getVolume(), requestDTO.getDirection(),
                            requestDTO.getOffset(), requestDTO.getLeverRate(), requestDTO.getOrderPriceType());
                    System.out.println("---currentMACD:" + macdArr[macdArr.length - 1]
                            + "---lastMACD:" + macdArr[macdArr.length - 2]
                            + "---currentDIF:" + difArr[difArr.length - 1]
                            + "---lastDIF:" + difArr[difArr.length - 2]
                            + "---currentDEA:" + deaArr[deaArr.length - 1]
                            + "---lastDEA:" + deaArr[deaArr.length - 2]
                    );
                    //下单后，多等一秒，防止获取持仓情况延迟
                    try {
                        Thread.sleep(sleepMillis);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //获取持仓情况及最大下单量
                    updateHaveOrderAndVolume(CONTRACTCODE, requestDTO, contractService);
                    System.out.println(getTimeFormat(System.currentTimeMillis()) + "---已成功下单" + requestDTO.getVolume() + "张！---" + requestDTO.toString());
                }
                requestDTO.setCurrentTakeOrder(false);
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

    private static void updateHaveOrderAndVolume(String CONTRACTCODE, ContractPlaceOrderRequest requestDTO, ContractClient contractService) {
        ContractPosition contractPosition;
        ContractAccount contractAccount;
        List<Double> closeList;
        //获取目前持仓，账户余额，计算最大下单张数
        contractPosition = getContractPosition(CONTRACTCODE, contractService);
        if (contractPosition != null) {
            requestDTO.setHaveOrder(true);
            requestDTO.setHavaOrderDirection(contractPosition.getDirection());
            requestDTO.setVolume(contractPosition.getVolume().longValue());
            System.out.println("当前持仓量：" + contractPosition.getVolume().doubleValue() + "---当前持仓方向：" + contractPosition.getDirection());
        } else {
            requestDTO.setHaveOrder(false);
            requestDTO.setHavaOrderDirection(null);
            //无持仓，获取账户余额，计算开仓张数（当前杠杆下满仓）
            contractAccount = getContractAccount(CONTRACTCODE, contractService);
            //获取当前价格
            closeList = getMarketPriceListByKLine(contractService, CONTRACTCODE, CandlestickIntervalEnum.MIN1, 1).get("closeList");
            requestDTO.setVolume(contractAccount.getMargin_available()
                    .multiply(new BigDecimal(closeList.get(closeList.size() - 1)))
                    .multiply(new BigDecimal(requestDTO.getLeverRate()))
                    .divide(new BigDecimal(100)).longValue() - 1);
            System.out.println("当前无持仓!!!目前最大可开张数为：" + requestDTO.getVolume() + "张,倍数为：" + requestDTO.getLeverRate());
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

    private static void getOrderByParams(ContractPlaceOrderRequest requestDTO, Double[] difArr, Double[] deaArr, Double[] macdArr, double[][] bollArr) {
        Double lastDif = null;
        Double lastDea = null;
        Double lastMacd = null;
        Double currentDif = null;
        Double currentDea = null;
        Double currentMacd = null;
        double upBoll = bollArr[0][bollArr[0].length - 1];
        double midBoll = bollArr[1][bollArr[1].length - 1];
        double lowBoll = bollArr[2][bollArr[2].length - 1];


//        for (int i = 1; i < 3; i++) {
//            currentDif = difArr[difArr.length - i];
//            currentDea = deaArr[deaArr.length - i];
//            currentMacd = macdArr[macdArr.length - i];
//            lastDif = difArr[difArr.length - i - 1];
//            lastDea = deaArr[deaArr.length - i - 1];
//            lastMacd = macdArr[macdArr.length - i - 1];
//
//            //判断上一次是否有值
//            if (lastDif != null && lastDea != null && lastMacd != null) {
//                if (requestDTO.getHaveOrder()) {
//                    //有持仓则只考虑平仓，判断是否出现背离
//                    if ("buy".equals(requestDTO.getHavaOrderDirection())) {
//                        if (currentDif < lastDif
//                                && currentDea < lastDea
//                                && currentMacd < lastMacd
//                        ) {
//                            requestDTO.setOffset("close");
//                            requestDTO.setDirection("sell");
//                            System.out.println("***行情转空头，已平仓" + requestDTO.getVolume() + "张！！");
//                            requestDTO.setCurrentTakeOrder(true);
//                            return;
//                        }
//                    } else if ("sell".equals(requestDTO.getHavaOrderDirection())) {
//                        if (currentDif > lastDif
//                                && currentDea > lastDea
//                                && currentMacd > lastMacd
//                        ) {
//                            requestDTO.setOffset("close");
//                            requestDTO.setDirection("buy");
//                            System.out.println("***行情转多头，已平仓" + requestDTO.getVolume() + "张！！");
//                            requestDTO.setCurrentTakeOrder(true);
//                            return;
//                        }
//                    }
//                } else {
//                    //无持仓只考虑开仓，判断三线同一方向则开单
//                    if (currentDif > lastDif
//                            && currentDea > lastDea
//                            && currentMacd > lastMacd
//                    ) {
//                        requestDTO.setOffset("open");
//                        requestDTO.setDirection("buy");
//                        System.out.println("***满足开多仓条件，已开：" + requestDTO.getVolume() + "张！！***");
//                        requestDTO.setCurrentTakeOrder(true);
//                        return;
//                    } else if (currentDif < lastDif
//                            && currentDea < lastDea
//                            && currentMacd < lastMacd
//                    ) {
//                        requestDTO.setOffset("open");
//                        requestDTO.setDirection("sell");
//                        System.out.println("***满足开空仓条件，已开：" + requestDTO.getVolume() + "张！！***");
//                        requestDTO.setCurrentTakeOrder(true);
//                        return;
//                    }
//                }
//            }
//        }
    }

    private static Map<String, List<Double>> getMarketPriceListByKLine(ContractClient contractService, String
            contractCode, CandlestickIntervalEnum periodEnum, Integer size) {
        Map<String, List<Double>> resultMap = new HashMap<>();
        ArrayList<Double> closeList = new ArrayList<>();
        ArrayList<Double> highList = new ArrayList<>();
        ArrayList<Double> lowList = new ArrayList<>();
        List<ContractKline> contractAccountList = contractService.getContractKline(ContractKlineRequest.builder()
                .contractCode(contractCode)
                .period(periodEnum.getCode())
                .size(size)
                .build());
        //获取周期内收盘价
        contractAccountList.forEach(contract -> {
            closeList.add(contract.getClose().doubleValue());
        });
        //获取周期内最高价
        contractAccountList.forEach(contract -> {
            highList.add(contract.getHigh().doubleValue());
        });
        contractAccountList.forEach(contract -> {
            lowList.add(contract.getLow().doubleValue());
        });
        resultMap.put("closeList", closeList);
        resultMap.put("highList", highList);
        resultMap.put("lowList", lowList);
        return resultMap;
    }
}

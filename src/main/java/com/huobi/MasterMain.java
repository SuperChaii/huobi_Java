package com.huobi;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.huobi.client.ContractClient;
import com.huobi.client.req.contract.ContractAccountRequest;
import com.huobi.client.req.contract.ContractKlineRequest;
import com.huobi.client.req.contract.ContractParmaDto;
import com.huobi.client.req.contract.ContractPlaceOrderRequest;
import com.huobi.constant.HuobiOptions;
import com.huobi.constant.enums.CandlestickIntervalEnum;
import com.huobi.model.contract.ContractAccount;
import com.huobi.model.contract.ContractKline;
import com.huobi.model.contract.ContractPosition;
import com.huobi.utils.IndicatrixImpl;
import com.huobi.utils.quant.QuantIndicators;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

public class MasterMain {

    public static void main(String[] args) {
        final String CONTRACTCODE = "BTC-USD";
        final Long sleepMillis = 2000L;

        ContractPlaceOrderRequest request = new ContractPlaceOrderRequest();
        ContractParmaDto dto = new ContractParmaDto();
        QuantIndicators qiUtils = new QuantIndicators();
        request.setVolume(1L);
        request.setDirection("buy");
        request.setOffset("open");
        request.setLeverRate(20);
        request.setOrderPriceType("opponent");
        dto.setDynamicNum(0.1);

        Map<String, List<Double>> marketMap;

        IndicatrixImpl impl = new IndicatrixImpl();


        //获取合约客户端
        ContractClient contractService = ContractClient.create(HuobiOptions.builder()
                .restHost("https://api.hbdm.com")
                .apiKey(Constants.API_KEY)
                .secretKey(Constants.SECRET_KEY)
                .build());
        //获取持仓情况及最大下单量
        updateHaveOrderAndVolume(CONTRACTCODE, request, dto, contractService);

        while (true) {
            try {
                //获取收盘价，根据K线图
                marketMap = getMarketPriceListByKLine(contractService, CONTRACTCODE, CandlestickIntervalEnum.MIN15, 50);
                dto = updateMarketPriceByResult(dto, marketMap);

                //获取BOLL指标
                Double[] closePrice = dto.getCloseList().toArray(new Double[dto.getCloseList().size()]);
                double[] doubleArr = new double[closePrice.length];
                for (int i = 0; i < closePrice.length; i++) {
                    doubleArr[i] = closePrice[i];
                }
                double[][] bollArr = qiUtils.boll(doubleArr);
                //获取EMD指标
                //double[] emaArr = qiUtils.ema(doubleArr, 21);
                //获取MACD指标
                Double[] macdArr = new Double[dto.getCloseList().size()];
                Double[] deaArr = new Double[dto.getCloseList().size()];
                Double[] difArr = new Double[dto.getCloseList().size()];
                impl.MACD(dto.getCloseList().toArray(new Double[dto.getCloseList().size()]), 12, 26, 9, macdArr, deaArr, difArr);

                //根据macd & boll & high & low 指标判断是否下单
                getOrderByParams(request, dto, difArr, deaArr, macdArr, bollArr);
                //合约下单
                if (dto.getCurrentTakeOrder()) {
                    takeOrder(CONTRACTCODE, contractService, request.getVolume(), request.getDirection(),
                            request.getOffset(), request.getLeverRate(), request.getOrderPriceType());
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
                    updateHaveOrderAndVolume(CONTRACTCODE, request, dto, contractService);
                    System.out.println(getTimeFormat(System.currentTimeMillis()) + "---已成功下单" + request.getVolume() + "张！---" + request.toString());
                }
                dto.setCurrentTakeOrder(false);
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

    @NotNull
    private static ContractParmaDto updateMarketPriceByResult(ContractParmaDto dto, Map<String, List<Double>> marketMap) {
        List<Double> closeList = marketMap.get("closeList");
        List<Double> highList = marketMap.get("highList");
        List<Double> lowList = marketMap.get("lowList");
        dto.setCloseList(closeList);
        dto.setHighList(highList);
        dto.setLowList(lowList);

        dto.setHigh5Price(highList.subList(highList.size() - 5, highList.size()).stream().filter(Objects::nonNull).max(Comparator.comparingDouble(price -> price)).get());
        dto.setLow5Price(lowList.subList(lowList.size() - 5, lowList.size()).stream().filter(Objects::nonNull).min(Comparator.comparingDouble(price -> price)).get());
        dto.setHigh30Price(highList.subList(highList.size() - 30, highList.size()).stream().filter(Objects::nonNull).max(Comparator.comparingDouble(price -> price)).get());
        dto.setLow30Price(lowList.subList(lowList.size() - 30, lowList.size()).stream().filter(Objects::nonNull).min(Comparator.comparingDouble(price -> price)).get());
        dto.setCurrentClosePrice(closeList.get(closeList.size() - 1));

        dto.setCurrentClosePrice(closeList.get(closeList.size() - 1));
        dto.setLastClosePrice(closeList.get(closeList.size() - 2));
        dto.setCurrentHighPrice(highList.get(highList.size() - 1));
        dto.setLastHighPrice(highList.get(highList.size() - 2));
        return dto;
    }

    private static void getOrderByParams(ContractPlaceOrderRequest request, ContractParmaDto dto, Double[] difArr, Double[] deaArr, Double[] macdArr, double[][] bollArr) {
        Double lastDif = null;
        Double lastDea = null;
        Double lastMacd = null;
        Double currentDif = null;
        Double currentDea = null;
        Double currentMacd = null;
        double upBoll = bollArr[0][bollArr[0].length - 1];
        double midBoll = bollArr[1][bollArr[1].length - 1];
        double lowBoll = bollArr[2][bollArr[2].length - 1];
        Double currentPrice = dto.getCurrentClosePrice();

        //以下为开仓逻辑：
        if (!dto.getHaveOrder()) {
            //判断 当前价格是否连续突破30日前高或前低，突破则转换为趋势行情，否则为波段
            if (dto.getCurrentHighPrice() >= dto.getHigh30Price() ||
                    dto.getCurrentHighPrice() <= dto.getHigh30Price()
            ) {
                //趋势行情
                dto.setTrendType(true);
                //当价格突破highPrice 做多 / 突破lowPrice做空
                if (currentPrice > upBoll) {
                    request.setOffset("open");
                    request.setDirection("buy");
                    dto.setCurrentTakeOrder(true);
                    System.out.println("***当前为【趋势】行情（高买低卖），突破上轨道，已【开多】仓" + request.getVolume() + "张！！");
                } else if (currentPrice < lowBoll) {
                    request.setOffset("open");
                    request.setDirection("sell");
                    dto.setCurrentTakeOrder(true);
                    System.out.println("***当前为【趋势】行情（高买低卖），跌破下轨道，已【开空】仓" + request.getVolume() + "张！！");
                }
            } else {
                //波段行情
                dto.setTrendType(false);
                //当价格突破highPrice 做空 / 突破lowPrice做多
                if (currentPrice > upBoll) {
                    request.setOffset("open");
                    request.setDirection("sell");
                    dto.setCurrentTakeOrder(true);
                    System.out.println("***当前为【波段】行情(低买高卖)，突破上轨，已【开空】仓" + request.getVolume() + "张！！");
                } else if (currentPrice < lowBoll) {
                    request.setOffset("open");
                    request.setDirection("buy");
                    dto.setCurrentTakeOrder(true);
                    System.out.println("***当前为【波段】行情(低买高卖)，跌破下轨，已【开多】仓" + request.getVolume() + "张！！");
                }
            }
        } else {
            //平仓逻辑：判断开仓时是趋势还是波段
            //如果是趋势行情：当跌/突破5日k线平仓
            if (dto.getTrendType()) {
                //趋势无止盈，只有止损:当跌破5日k线最低价 / 跌破lowBoll价格平仓
                if ("buy".equals(dto.getHavaOrderDirection())
                        && (currentPrice <= dto.getLow5Price() || currentPrice < lowBoll)

                ) {
                    //当趋势跌破5日k最低价时平仓
                    request.setOffset("close");
                    request.setDirection("sell");
                    dto.setCurrentTakeOrder(true);
                    System.out.println("***当前为【趋势】行情做多，跌破5日k线，已【平多】仓" + request.getVolume() + "张！！");
                } else if ("sell".equals(dto.getHavaOrderDirection())
                        && (currentPrice >= dto.getHigh5Price() || currentPrice > upBoll)
                ) {
                    //当趋势突破5日k最高价时平仓
                    request.setOffset("close");
                    request.setDirection("buy");
                    dto.setCurrentTakeOrder(true);
                    System.out.println("***当前为【趋势】行情做空，突破上轨，已【平空】仓" + request.getVolume() + "张！！");
                }
            } else {
                //波段行情 -> 止盈:突破upboll，止损：跌破5日k最低价
                if ("buy".equals(dto.getHavaOrderDirection())
                        && (currentPrice >= upBoll || currentPrice <= dto.getLow5Price())
                ) {
                    request.setOffset("close");
                    request.setDirection("sell");
                    dto.setCurrentTakeOrder(true);
                    System.out.println("***当前为【波段】行情做多 -> 止盈:突破upboll / 止损：跌破5日k最低价，已【平多】仓" + request.getVolume() + "张！！");
                }else if("sell".equals(dto.getHavaOrderDirection())
                    && (currentPrice <= lowBoll || currentPrice >= dto.getHigh5Price())
                ){
                    //波段行情 -> 止盈:跌破lowBoll，止损：突破5k最高价
                    request.setOffset("close");
                    request.setDirection("buy");
                    dto.setCurrentTakeOrder(true);
                    System.out.println("***当前为【波段】行情做空 -> 已止盈:跌破lowBoll/止损：突破5k最高价，已【平空】仓" + request.getVolume() + "张！！");
                }
            }
        }


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
//                if (request.getHaveOrder()) {
//                    //有持仓则只考虑平仓，判断是否出现背离
//                    if ("buy".equals(request.getHavaOrderDirection())) {
//                        if (currentDif < lastDif
//                                && currentDea < lastDea
//                                && currentMacd < lastMacd
//                        ) {
//                            request.setOffset("close");
//                            request.setDirection("sell");
//                            System.out.println("***行情转空头，已平仓" + request.getVolume() + "张！！");
//                            request.setCurrentTakeOrder(true);
//                            return;
//                        }
//                    } else if ("sell".equals(request.getHavaOrderDirection())) {
//                        if (currentDif > lastDif
//                                && currentDea > lastDea
//                                && currentMacd > lastMacd
//                        ) {
//                            request.setOffset("close");
//                            request.setDirection("buy");
//                            System.out.println("***行情转多头，已平仓" + request.getVolume() + "张！！");
//                            request.setCurrentTakeOrder(true);
//                            return;
//                        }
//                    }
//                } else {
//                    //无持仓只考虑开仓，判断三线同一方向则开单
//                    if (currentDif > lastDif
//                            && currentDea > lastDea
//                            && currentMacd > lastMacd
//                    ) {
//                        request.setOffset("open");
//                        request.setDirection("buy");
//                        System.out.println("***满足开多仓条件，已开：" + request.getVolume() + "张！！***");
//                        request.setCurrentTakeOrder(true);
//                        return;
//                    } else if (currentDif < lastDif
//                            && currentDea < lastDea
//                            && currentMacd < lastMacd
//                    ) {
//                        request.setOffset("open");
//                        request.setDirection("sell");
//                        System.out.println("***满足开空仓条件，已开：" + request.getVolume() + "张！！***");
//                        request.setCurrentTakeOrder(true);
//                        return;
//                    }
//                }
//            }
//        }
    }


    private static void updateHaveOrderAndVolume(String CONTRACTCODE, ContractPlaceOrderRequest request, ContractParmaDto dto, ContractClient contractService) {
        ContractPosition contractPosition;
        ContractAccount contractAccount;
        List<Double> closeList;
        //获取目前持仓，账户余额，计算最大下单张数
        contractPosition = getContractPosition(CONTRACTCODE, contractService);
        if (contractPosition != null) {
            dto.setHaveOrder(true);
            dto.setHavaOrderDirection(contractPosition.getDirection());
            request.setVolume(contractPosition.getVolume().longValue());
            System.out.println("当前持仓量：" + contractPosition.getVolume().doubleValue() + "---当前持仓方向：" + contractPosition.getDirection());
        } else {
            dto.setHaveOrder(false);
            dto.setHavaOrderDirection(null);
            //无持仓，获取账户余额，计算开仓张数（当前杠杆下满仓）
            contractAccount = getContractAccount(CONTRACTCODE, contractService);
            //获取当前价格
            closeList = getMarketPriceListByKLine(contractService, CONTRACTCODE, CandlestickIntervalEnum.MIN1, 1).get("closeList");
            request.setVolume(contractAccount.getMargin_available()
                    .multiply(new BigDecimal(closeList.get(closeList.size() - 1)))
                    .multiply(new BigDecimal(request.getLeverRate()))
                    .divide(new BigDecimal(100)).longValue() - 1);
            System.out.println("当前无持仓!!!目前最大可开张数为：" + request.getVolume() + "张,倍数为：" + request.getLeverRate());
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

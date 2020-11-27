package com.huobi;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.huobi.client.ContractClient;
import com.huobi.client.req.contract.*;
import com.huobi.model.contract.ContractAccount;
import com.huobi.model.contract.ContractKline;
import com.huobi.model.contract.ContractPosition;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class CoreMethods {
    protected static void validParams(String[] args, ContractUniversalRequest request, ContractParmaDto dto) {
        String bname = args[0];
        Integer beishu = Integer.valueOf(args[1]);
        String kLineCycle = args[2];
        Integer longLineCycle = Integer.valueOf(args[3]);
        Integer shortLineCycle = Integer.valueOf(args[4]);
        String trendType = args[5];
        Integer emaValue = Integer.valueOf(args[6]);
        //校验入参格式:币种名称，默认BTC-USD
        if (Objects.nonNull(bname)) {
            request.setContractCode(bname);
        } else {
            request.setContractCode("BTC-USD");
        }
        //校验入参格式，并赋值(默认20X)
        if (Objects.nonNull(beishu)) {
            request.setLeverRate(beishu);
        } else {
            request.setLeverRate(20);
        }
        //校验入参:K线周期,默认15分钟
        if (Objects.nonNull(kLineCycle)
        ) {
            dto.setPeriodTime(kLineCycle);
        } else {
            dto.setPeriodTime("15min");
        }
        //校验入参:长k线次数，默认30
        if (Objects.nonNull(longLineCycle)) {
            dto.setLongLineCycle(longLineCycle);
        } else {
            dto.setLongLineCycle(30);
        }
        //校验入参：短k线次数，默认5
        if (Objects.nonNull(shortLineCycle)) {
            dto.setShortLineCycle(shortLineCycle);
        } else {
            dto.setShortLineCycle(5);
        }
        //校验入参：趋势
        if (Objects.nonNull(trendType) && "1".equals(trendType)) {
            dto.setTrendType(true);
        } else {
            dto.setTrendType(false);
        }
        //校验入参：ema
        if (Objects.nonNull(emaValue)) {
            dto.setEmaValue(emaValue);
        } else {
            dto.setEmaValue(120);
        }
    }

    protected static String getTimeFormat(long currentTimeMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(Long.parseLong(String.valueOf(currentTimeMillis))));
    }

    protected static void takeOrder(String contractcode, ContractClient contractService, Long volume,
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

    protected static void triggerOrder(ContractClient contractService, ContractUniversalRequest request) {
        JSONObject json = contractService.triggerOrder(ContractTriggerOrderRequest.builder()
                .contractCode(request.getContractCode())
                .volume(request.getVolume())
                .direction(request.getDirection())
                .leverRate(request.getLeverRate())
                .offset(request.getOffset())
                .orderPrice(request.getOrderPrice())
                .orderPriceType(request.getOrderPriceType())
                .triggerPrice(request.getTriggerPrice())
                .triggerType(request.getTriggerType())
                .build());
    }

    protected static ContractPosition getContractPosition(String CONTRACTCODE, ContractClient contractService) {
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

    protected static ContractAccount getContractAccount(String CONTRACTCODE, ContractClient contractService) {
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


    protected static void getMarketInfoListByKLine(ContractClient contractService, String
            contractCode, ContractParmaDto dto) throws InterruptedException {
        List<ContractKline> contractAccountList;
        String currentTime = getTimeFormat(System.currentTimeMillis());
        DecimalFormat df = new DecimalFormat("#.00");
        while (true) {
            contractAccountList = contractService.getContractKline(ContractKlineRequest.builder()
                    .contractCode(contractCode)
                    .period(dto.getPeriodTime())
                    .size(dto.getLongLineCycle() + 50)
                    .from(dto.getStartTime())
                    .to(dto.getEndTime())
                    .build());
            if (Objects.isNull(dto.getLastRealPrice())) {
                dto.setLastRealPrice(contractAccountList.get(contractAccountList.size() - 1).getClose());
            } else {
                dto.setCurrRealPrice(contractAccountList.get(contractAccountList.size() - 1).getClose());
                if (dto.getCurrRealPrice().compareTo(dto.getLastRealPrice()) != 0) {
                    updatePriceByMarket(dto, contractAccountList);
                    System.out.println(currentTime + ">" +
                            "hv【" + dto.getHaveOrder() + "】" +
                            "hod【" + dto.getHavaOrderDirection() + "】" +
                            "tr【" + dto.getTrendType() + "】" +
                            "vo【" + dto.getVolume() + "】" +
                            "pr【" + dto.getCurrRealPrice() + "】" +
                            "30h【" + dto.getHigh30Price() + "】" +
                            "30l【" + dto.getLow30Price() + "】" +
                            "5h【" + dto.getHigh5Price() + "】" +
                            "5l【" + dto.getLow5Price() + "】" +
                            "uplp【" + df.format(dto.getUpStopLossPoint() != null ? dto.getUpStopLossPoint() : 0) + "】" +
                            "lowlp【" + df.format(dto.getLowStopLossPoint() != null ? dto.getLowStopLossPoint() : 0) + "】"
                    );
                    dto.setLastRealPrice(contractAccountList.get(contractAccountList.size() - 1).getClose());
                    break;
                }
            }
            Thread.sleep(dto.getSleepMillis());
        }
    }

    protected static void updatePriceByMarket(ContractParmaDto dto, List<ContractKline> contractAccountList) {
        ArrayList<Double> closeList = new ArrayList<>();
        ArrayList<Double> highList = new ArrayList<>();
        ArrayList<Double> lowList = new ArrayList<>();
        ArrayList<Double> volumeList = new ArrayList<>();
        //获取周期内收盘价
        contractAccountList.forEach(contract -> {
            closeList.add(contract.getClose().doubleValue());
        });
        //获取周期内最高价
        contractAccountList.forEach(contract -> {
            highList.add(contract.getHigh().doubleValue());
        });
        //获取周期内最低价
        contractAccountList.forEach(contract -> {
            lowList.add(contract.getLow().doubleValue());
        });
        contractAccountList.forEach(contract ->{
            volumeList.add(contract.getVol().doubleValue());
        });

        dto.setCloseList(closeList);
        dto.setHighList(highList);
        dto.setLowList(lowList);
        dto.setVolumeList(volumeList);

        dto.setHigh5Price(highList.subList(highList.size() - dto.getShortLineCycle(), highList.size())
                .stream().filter(Objects::nonNull).max(Comparator.comparingDouble(price -> price)).get());
        dto.setLow5Price(lowList.subList(lowList.size() - dto.getShortLineCycle(), lowList.size())
                .stream().filter(Objects::nonNull).min(Comparator.comparingDouble(price -> price)).get());
        dto.setHigh30Price(highList.subList(highList.size() - dto.getLongLineCycle(), highList.size())
                .stream().filter(Objects::nonNull).max(Comparator.comparingDouble(price -> price)).get());
        dto.setLow30Price(lowList.subList(lowList.size() - dto.getLongLineCycle(), lowList.size())
                .stream().filter(Objects::nonNull).min(Comparator.comparingDouble(price -> price)).get());
        dto.setCurrentClosePrice(closeList.get(closeList.size() - 1));

        dto.setCurrentClosePrice(closeList.get(closeList.size() - 1));
        dto.setLastClosePrice(closeList.get(closeList.size() - 2));
        dto.setCurrentHighPrice(highList.get(highList.size() - 1));
        dto.setLastHighPrice(highList.get(highList.size() - 2));
        dto.setCurrentLowPrice(lowList.get(lowList.size() - 1));
        dto.setLastLowPrice(lowList.get(lowList.size() - 2));

    }
}

package com.huobi;

import com.huobi.client.ContractClient;
import com.huobi.client.req.contract.ContractParmaDto;
import com.huobi.client.req.contract.ContractUniversalRequest;
import com.huobi.model.contract.ContractAccount;
import com.huobi.model.contract.ContractPosition;

import java.math.BigDecimal;
import java.util.Objects;

import static com.huobi.CoreMethods.*;

public class CoreLogic {
    protected static void getOrderByParams(ContractUniversalRequest request, ContractParmaDto dto, double[][] bollArr) {
        double upBoll = bollArr[0][bollArr[0].length - 1];
        double midBoll = bollArr[1][bollArr[1].length - 1];
        double lowBoll = bollArr[2][bollArr[2].length - 1];
        Double currentHighPrice = dto.getCurrentHighPrice();
        Double lastHighPrice = dto.getLastHighPrice();
        Double currentLowPrice = dto.getCurrentLowPrice();
        Double lastLowPrice = dto.getLastLowPrice();
        Double currentPrice = dto.getCurrentClosePrice();

        Double currMacd = dto.getMacdArr()[dto.getMacdArr().length - 1];
        Double lastMacd = dto.getMacdArr()[dto.getMacdArr().length - 2];
        Double currDif = dto.getDifArr()[dto.getDifArr().length - 1];
        Double lastDIf = dto.getDifArr()[dto.getDifArr().length - 2];
        Double currDea = dto.getDeaArr()[dto.getDeaArr().length - 1];
        Double lastDea = dto.getDeaArr()[dto.getDeaArr().length - 2];
        String currentTime = getTimeFormat(System.currentTimeMillis());

        //默认为吃单开仓（对手价20）
        request.setOrderPriceType("optimal_20");
        //***开仓逻辑***
        if (!dto.getHaveOrder()) {
            //判断 当前价格是否连续突破长周期（自定义）日前高或前低，突破则转换为趋势行情，否则为波段
            if (dto.getCurrentHighPrice() >= dto.getHigh30Price()
                    || dto.getLastHighPrice() >= dto.getHigh30Price()
                    || dto.getCurrentLowPrice() <= dto.getLow30Price()
                    || dto.getLastLowPrice() <= dto.getLow30Price()
            ) {
                //当价格突破highPrice 做多 / 突破lowPrice做空,且做多时DIF大于DEA线，做空时同理
                if (currentPrice > upBoll && currDif > currDea) {
                    //趋势行情
                    dto.setTrendType(true);
                    request.setOffset("open");
                    request.setDirection("buy");
                    dto.setCurrentTakeOrder(true);
                    System.out.println("***" + currentTime + "当前为【趋势】行情（高买低卖），突破上轨道，已【开多】仓" + request.getVolume() + "张！！" +
                            "currentPrice > upBoll：" + currentPrice + ">" + upBoll);
                } else if (currentPrice < lowBoll && currDif < currDea) {
                    //趋势行情
                    dto.setTrendType(true);
                    request.setOffset("open");
                    request.setDirection("sell");
                    dto.setCurrentTakeOrder(true);
                    System.out.println("***" + currentTime + "当前为【趋势】行情（高买低卖），跌破下轨道，已【开空】仓" + request.getVolume() + "张！！" +
                            "currentPrice < lowBoll:" + currentPrice + "<" + lowBoll);
                }
            } else {
                //波段行情
                dto.setTrendType(false);
                //当highPrice突破后会回落 做空 / 当lowPrice跌破反弹时 做多 ，且当前MACD柱小于前一次MACD柱做空，做多同理
                if (currentHighPrice > upBoll && currentPrice < upBoll && currMacd < lastMacd) {
                    request.setOffset("open");
                    request.setDirection("sell");
                    dto.setCurrentTakeOrder(true);
                    System.out.println("***" + currentTime + "当前为【波段】行情(低买高卖)，突破上轨，已【开空】仓" + request.getVolume() + "张！！" +
                            "currentPrice > upBoll:" + currentPrice + ">" + upBoll);
                } else if (currentLowPrice < lowBoll && currentPrice > lowBoll && currMacd > lastMacd) {
                    request.setOffset("open");
                    request.setDirection("buy");
                    dto.setCurrentTakeOrder(true);
                    System.out.println("***" + currentTime + "当前为【波段】行情(低买高卖)，跌破下轨，已【开多】仓" + request.getVolume() + "张！！" +
                            "currentPrice < lowBoll:" + currentPrice + "<" + lowBoll);
                }
            }
        } else {
            //***平仓逻辑***
            //趋势行情：当跌/突破5日k线平仓
            if (Objects.nonNull(dto.getTrendType()) && dto.getTrendType()) {
                //趋势无止盈，只有止损:当跌破5日k线最低价 / 跌破midBoll价格平仓
                if ("buy".equals(dto.getHavaOrderDirection())
                        && (currentPrice <= dto.getLow5Price() || currentPrice < midBoll)
                ) {
                    //当趋势跌破5日k最低价时平仓
                    request.setOffset("close");
                    request.setDirection("sell");
                    dto.setCurrentTakeOrder(true);
                    System.out.println("***" + currentTime + "当前为【趋势】行情做多，跌破5日k线，已【平多】仓" + request.getVolume() + "张！！" +
                            "(currentPrice <= dto.getLow5Price() || currentPrice < midBoll)"
                            + currentPrice + "<=" + dto.getLow5Price() + "||" + currentPrice + "<" + midBoll);
                } else if ("sell".equals(dto.getHavaOrderDirection())
                        && (currentPrice >= dto.getHigh5Price() || currentPrice >= midBoll)
                ) {
                    //当趋势突破5日k最高价时平仓
                    request.setOffset("close");
                    request.setDirection("buy");
                    dto.setCurrentTakeOrder(true);
                    System.out.println("***" + currentTime + "当前为【趋势】行情做空，突破上轨，已【平空】仓" + request.getVolume() + "张！！" +
                            "(currentPrice >= dto.getHigh5Price() || currentPrice >= midBoll)"
                            + currentPrice + ">=" + dto.getHigh5Price() + "||" + currentPrice + ">=" + midBoll);
                }
            } else {
                //波段行情 -> 止盈:突破upboll后回落或突破upboll后跌破5日k低价，止损：跌破30日k最低价
                if ("buy".equals(dto.getHavaOrderDirection())
                        && (currentHighPrice >= upBoll
                        || lastHighPrice >= upBoll
                        || currentPrice <= dto.getLow30Price())
                ) {
                    if ((currentPrice <= upBoll || currentPrice <= dto.getLow5Price())
                            && currMacd < lastMacd) {
                        request.setOffset("close");
                        request.setDirection("sell");
                        dto.setCurrentTakeOrder(true);
                        System.out.println("***" + currentTime + "当前为【波段】行情做多 -> 已【平多】仓" + request.getVolume() + "张！！" +
                                "【currentHighPrice >= upBoll || currentPrice <= dto.getLow30Price()】 ; 【currentPrice <= upBoll || currentPrice <= dto.getLow5Price()】 "
                                + (currentHighPrice >= upBoll) + "||" + (currentPrice <= dto.getLow30Price()) + ";" + (currentPrice <= upBoll) + "||" + (currentPrice <= dto.getLow5Price()));
                    }
                } else if ("sell".equals(dto.getHavaOrderDirection())
                        && (currentLowPrice <= lowBoll
                        || lastLowPrice <= lowBoll
                        || currentPrice >= dto.getHigh30Price())
                ) {
                    if ((currentPrice >= lowBoll || currentPrice >= dto.getHigh5Price())
                            && currMacd > lastMacd) {
                        //波段行情 -> 止盈:跌破lowBoll，止损：突破5k最高价
                        request.setOffset("close");
                        request.setDirection("buy");
                        dto.setCurrentTakeOrder(true);
                        System.out.println("***" + currentTime + "当前为【波段】行情做空 ->已【平空】仓" + request.getVolume() + "张！！" +
                                "currentLowPrice <= lowBoll ; currentPrice >= dto.getHigh30Price() ; currentPrice >= lowBoll ; currentPrice >= dto.getHigh5Price()) || "
                                + (currentLowPrice <= lowBoll) + ";" + (currentPrice >= dto.getHigh30Price()) + (currentPrice >= lowBoll) + ";" + (currentPrice >= dto.getHigh5Price()) + ";");
                    }
                }
            }
        }
    }

    protected static void updateHaveOrderAndVolume(String CONTRACTCODE, ContractUniversalRequest request, ContractParmaDto dto, ContractClient contractService) {
        ContractPosition contractPosition;
        ContractAccount contractAccount;
        //获取目前持仓，账户余额，计算最大下单张数
        contractPosition = getContractPosition(CONTRACTCODE, contractService);
        if (contractPosition != null) {
            dto.setHaveOrder(true);
            dto.setHavaOrderDirection(contractPosition.getDirection());
            request.setVolume(contractPosition.getVolume().longValue());
            System.out.println(">" + contractPosition.getVolume().doubleValue() + "，" + contractPosition.getDirection());
        } else {
            //BTC合约面值为100U,其它合约面值为10U
            int contractFaceValue;
            if ("BTC-USD".equals(CONTRACTCODE)) {
                contractFaceValue = 100;
            } else {
                contractFaceValue = 10;
            }
            dto.setHaveOrder(false);
            dto.setHavaOrderDirection(null);
            //无持仓，获取账户余额，计算开仓张数（当前杠杆下满仓）
            contractAccount = getContractAccount(CONTRACTCODE, contractService);
            //获取当前价格
            request.setVolume(contractAccount.getMargin_available()
                    .multiply(new BigDecimal(dto.getCurrentClosePrice()))
                    .multiply(new BigDecimal(request.getLeverRate()))
                    .divide(new BigDecimal(contractFaceValue)).longValue() - 1);
            System.out.println(">无持仓!可开：" + request.getVolume() + "张," + request.getLeverRate());
        }
    }
}

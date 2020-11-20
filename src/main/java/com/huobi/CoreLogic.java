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
        Double upBoll = bollArr[0][bollArr[0].length - 1];
        Double midBoll = bollArr[1][bollArr[1].length - 1];
        Double lowBoll = bollArr[2][bollArr[2].length - 1];
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
                    dto.setUpStopLossPoint(upBoll - (upBoll - midBoll) / 2);
                    dto.setLowStopLossPoint(lowBoll + (midBoll - lowBoll) / 2);
                    System.out.println("***" + currentTime + "当前为【趋势】行情，已【开多】仓" + request.getVolume() + "张！！");
                } else if (currentPrice < lowBoll && currDif < currDea) {
                    //趋势行情
                    dto.setTrendType(true);
                    request.setOffset("open");
                    request.setDirection("sell");
                    dto.setCurrentTakeOrder(true);
                    dto.setUpStopLossPoint(upBoll - (upBoll - midBoll) / 2);
                    dto.setLowStopLossPoint(lowBoll + (midBoll - lowBoll) / 2);
                    System.out.println("***" + currentTime + "当前为【趋势】行情，【开空】仓" + request.getVolume() + "张！！");
                }
            } else {
                //波段行情
                dto.setTrendType(false);
                //当highPrice突破后会回落 做空 / 当lowPrice跌破反弹时 做多 ，且当前MACD柱小于前一次MACD柱做空，做多同理
                if ((currentHighPrice > upBoll || lastHighPrice > upBoll)
                        && currentPrice < upBoll
                        && currentPrice > midBoll
                        && currMacd < lastMacd
                ) {
                    request.setOffset("open");
                    request.setDirection("sell");
                    dto.setCurrentTakeOrder(true);
                    dto.setUpStopLossPoint(upBoll + (upBoll - midBoll) / 2);
                    dto.setLowStopLossPoint(lowBoll - (midBoll - lowBoll) / 2);
                    System.out.println("***" + currentTime + "当前为【波段】行情，已【开空】仓" + request.getVolume() + "张！！");
                } else if ((currentLowPrice < lowBoll || lastLowPrice < lowBoll)
                        && currentPrice > lowBoll
                        && currentPrice < midBoll
                        && currMacd > lastMacd
                ) {
                    request.setOffset("open");
                    request.setDirection("buy");
                    dto.setCurrentTakeOrder(true);
                    dto.setUpStopLossPoint(upBoll + (upBoll - midBoll) / 2);
                    dto.setLowStopLossPoint(lowBoll - (midBoll - lowBoll) / 2);
                    System.out.println("***" + currentTime + "当前为【波段】行情，已【开多】仓" + request.getVolume() + "张！！");
                }
            }
        } else {
            //***平仓逻辑***
            //**趋势行情**：
            if (Objects.nonNull(dto.getTrendType()) && dto.getTrendType()) {
                //为止损点赋默认值
                if(Objects.isNull(dto.getUpStopLossPoint())){
                    dto.setUpStopLossPoint(upBoll - (upBoll - midBoll) / 2);
                }
                if(Objects.isNull(dto.getLowStopLossPoint())){
                    dto.setLowStopLossPoint(lowBoll + (midBoll - lowBoll) / 2);
                }
                //趋势无止盈只有止损: 跌破5日k线最低价 / 跌破midBoll价格/ 跌破止损点 > upBoll - (upBoll - midBoll) / 2
                if ("buy".equals(dto.getHavaOrderDirection())
                        && (currentPrice <= dto.getLow5Price()
                        || currentPrice <= midBoll
                        || currentPrice <= dto.getUpStopLossPoint())
                ) {
                    //当趋势跌破5日k最低价时平仓
                    request.setOffset("close");
                    request.setDirection("sell");
                    dto.setCurrentTakeOrder(true);
                    System.out.println("***" + currentTime + "当前为【趋势】行情做多，已【平多】仓" + request.getVolume() + "张！！");
                } else if ("sell".equals(dto.getHavaOrderDirection())
                        && (currentPrice >= dto.getHigh5Price()
                        || currentPrice >= midBoll
                        || currentPrice >= dto.getLowStopLossPoint())
                ) {
                    //当趋势突破5日k最高价时平仓
                    request.setOffset("close");
                    request.setDirection("buy");
                    dto.setCurrentTakeOrder(true);
                    System.out.println("***" + currentTime + "当前为【趋势】行情做空，已【平空】仓" + request.getVolume() + "张！！");
                }
            } else {
                //**波段行情**
                // 止盈:突破upboll后回落或突破upboll后跌破5日k低价，
                // 止损：跌破30日k最低价 | 跌破 lowStopLossPoint位置
                // 为止损点赋默认值
                if(Objects.isNull(dto.getUpStopLossPoint())){
                    dto.setUpStopLossPoint(upBoll + (upBoll - midBoll) / 2);
                }
                if(Objects.isNull(dto.getLowStopLossPoint())){
                    dto.setLowStopLossPoint(lowBoll - (midBoll - lowBoll) / 2);
                }
                if ("buy".equals(dto.getHavaOrderDirection())
                        && (currentHighPrice >= upBoll
                        || lastHighPrice >= upBoll
                        || currentPrice <= dto.getLow30Price())
                        || currentPrice <= dto.getLowStopLossPoint()
                ) {
                    if ((currentPrice <= upBoll || currentPrice <= dto.getLow5Price())
                            && currMacd < lastMacd) {
                        request.setOffset("close");
                        request.setDirection("sell");
                        dto.setCurrentTakeOrder(true);
                        System.out.println("***" + currentTime + "当前为【波段】，已【平多】仓" + request.getVolume() + "张！！");
                    } else if (currentPrice <= dto.getLowStopLossPoint()) {
                        //止损：lowBoll * 2 - midBoll
                        request.setOffset("close");
                        request.setDirection("sell");
                        dto.setCurrentTakeOrder(true);
                        System.out.println("***" + currentTime + "当前为【波段】行情，止损点【平多】仓，" + request.getVolume() + "张！！");
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
                        System.out.println("***" + currentTime + "当前为【波段】行情，已【平空】仓" + request.getVolume() + "张！！");
                    } else if (currentPrice >= dto.getUpStopLossPoint()) {
                        //止损：upBoll+(upBoll-midBoll)/2)
                        request.setOffset("close");
                        request.setDirection("buy");
                        dto.setCurrentTakeOrder(true);
                        System.out.println("***" + currentTime + "当前为【波段】行情，止损点【平空】仓" + request.getVolume() + "张！！");

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
            request.setVolume(contractPosition.getVolume().longValue());
            dto.setHaveOrder(true);
            dto.setHavaOrderDirection(contractPosition.getDirection());
            dto.setVolume(request.getVolume());
        } else {
            //BTC合约面值为100U,其它合约面值为10U
            int contractFaceValue;
            if ("BTC-USD".equals(CONTRACTCODE)) {
                contractFaceValue = 100;
            } else {
                contractFaceValue = 10;
            }
            //无持仓，获取账户余额，计算开仓张数（当前杠杆下满仓）
            contractAccount = getContractAccount(CONTRACTCODE, contractService);
            //获取当前价格
            request.setVolume(contractAccount.getMargin_available()
                    .multiply(new BigDecimal(dto.getCurrentClosePrice()))
                    .multiply(new BigDecimal(request.getLeverRate()))
                    .divide(new BigDecimal(contractFaceValue)).longValue() - 1);
            dto.setHaveOrder(false);
            dto.setHavaOrderDirection(null);
            dto.setVolume(request.getVolume());
        }
    }
}

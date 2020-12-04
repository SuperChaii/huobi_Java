package com.huobi;

import com.huobi.client.ContractClient;
import com.huobi.client.req.contract.ContractParmaDto;
import com.huobi.client.req.contract.ContractUniversalRequest;
import com.huobi.model.contract.ContractAccount;
import com.huobi.model.contract.ContractPosition;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static com.huobi.CoreMethods.*;

public class CoreLogic {
    protected static void getOrderByParams(ContractUniversalRequest request, ContractParmaDto dto) {
        //Market
        Double currentHighPrice = dto.getCurrentHighPrice();
        Double lastHighPrice = dto.getLastHighPrice();
        Double currentLowPrice = dto.getCurrentLowPrice();
        Double lastLowPrice = dto.getLastLowPrice();
        Double currentPrice = dto.getCurrentClosePrice();
        //BOLL
        Double upBoll = dto.getBollArr()[0][dto.getBollArr()[0].length - 1];
        Double midBoll = dto.getBollArr()[1][dto.getBollArr()[1].length - 1];
        Double lowBoll = dto.getBollArr()[2][dto.getBollArr()[2].length - 1];
        //MACD
        Double currMacd = dto.getMacdArr()[dto.getMacdArr().length - 1];
        Double lastMacd = dto.getMacdArr()[dto.getMacdArr().length - 2];
        Double currDif = dto.getDifArr()[dto.getDifArr().length - 1];
        Double currDea = dto.getDeaArr()[dto.getDeaArr().length - 1];
        //OBV
        BigDecimal currObv = dto.getObvList().get(dto.getObvList().size() - 1);
        BigDecimal longHighObv = dto.getObvList().stream().max((x, y) -> x.compareTo(y)).get();
        BigDecimal longLowObv = dto.getObvList().stream().min((x, y) -> x.compareTo(y)).get();
        //EMA
        //Double currEma = dto.getEmaArr()[dto.getEmaArr().length - 1];
        //时间
        String currentTime = getTimeFormat(System.currentTimeMillis());

        //默认为吃单开仓（对手价20）
        request.setOrderPriceType("optimal_20");
        //***开仓逻辑***
        if (!dto.getHaveOrder()) {
            //趋势行情-量价突破 > 判断当前价格是否连续突破长周期（自定义）日前高或前低，且OBV量能同时突破 则转换为趋势行情，否则为波段
            if ((currObv.compareTo(longHighObv) >= 0
                    || currObv.compareTo(longLowObv) <= 0)
                    && (dto.getCurrentHighPrice() >= dto.getHigh30Price()
                    || dto.getLastHighPrice() >= dto.getHigh30Price()
                    || dto.getCurrentLowPrice() <= dto.getLow30Price()
                    || dto.getLastLowPrice() <= dto.getLow30Price()
            )) {
                if (currentPrice > upBoll) {
                    dto.setTrendType(true);
                    request.setOffset("open");
                    request.setDirection("buy");
                    dto.setCurrentTakeOrder(true);
                    dto.setUpStopLossPoint(midBoll);
                    System.out.println("***" + currentTime + "当前为【趋势】行情，已【开多】仓" + request.getVolume() + "张！！");
                } else if (currentPrice < lowBoll) {
                    dto.setTrendType(true);
                    request.setOffset("open");
                    request.setDirection("sell");
                    dto.setCurrentTakeOrder(true);
                    dto.setLowStopLossPoint(midBoll);
                    System.out.println("***" + currentTime + "当前为【趋势】行情，【开空】仓" + request.getVolume() + "张！！");
                }
            } else {
                //波段行情-开空仓
                dto.setTrendType(false);
                //当highPrice突破后会回落 做空 / 当lowPrice跌破反弹时 做多
                if (currentHighPrice >= upBoll
                        && currentPrice < upBoll
                        && currentPrice > midBoll
                        && currentHighPrice != dto.getHigh30Price()
                ) {
                    request.setOffset("open");
                    request.setDirection("sell");
                    dto.setCurrentTakeOrder(true);
                    dto.setUpStopLossPoint(upBoll + (upBoll - midBoll));
                    System.out.println("***" + currentTime + "当前为【波段】行情，已【开空】仓" + request.getVolume() + "张！！");
                    //波段行情-开多仓
                } else if (currentLowPrice <= lowBoll
                        && currentPrice > lowBoll
                        && currentPrice < midBoll
                        && currentLowPrice != dto.getLow30Price()
                ) {
                    request.setOffset("open");
                    request.setDirection("buy");
                    dto.setCurrentTakeOrder(true);
                    dto.setLowStopLossPoint(lowBoll - (midBoll - lowBoll));
                    System.out.println("***" + currentTime + "当前为【波段】行情，已【开多】仓" + request.getVolume() + "张！！");
                }
            }
        } else {
            //***趋势行情平仓***
            if (Objects.nonNull(dto.getTrendType()) && dto.getTrendType()) {
                //为止损点赋默认值
                if (Objects.isNull(dto.getUpStopLossPoint())) {
                    dto.setUpStopLossPoint(midBoll);
                }
                if (Objects.isNull(dto.getLowStopLossPoint())) {
                    dto.setLowStopLossPoint(midBoll);
                }
                //趋势无止盈只有止损: 跌破5日k线最低价 / 跌破止损点 / mid
                if ("buy".equals(dto.getHavaOrderDirection())
                        && (currentPrice <= dto.getLow5Price()
                        || currentPrice <= dto.getUpStopLossPoint())
                ) {
                    //当趋势跌破5日k最低价时平仓
                    request.setOffset("close");
                    request.setDirection("sell");
                    dto.setCurrentTakeOrder(true);
                    System.out.println("***" + currentTime + "当前为【趋势】行情做多，已【平多】仓" + request.getVolume() + "张！！");
                } else if ("sell".equals(dto.getHavaOrderDirection())
                        && (currentPrice >= dto.getHigh5Price()
                        || currentPrice >= dto.getLowStopLossPoint())
                ) {
                    //当趋势突破5日k最高价时平仓
                    request.setOffset("close");
                    request.setDirection("buy");
                    dto.setCurrentTakeOrder(true);
                    System.out.println("***" + currentTime + "当前为【趋势】行情做空，已【平空】仓" + request.getVolume() + "张！！");
                }
            } else {
                //***波段行情平仓***
                if (Objects.isNull(dto.getUpStopLossPoint())) {
                    dto.setUpStopLossPoint(upBoll + (upBoll - midBoll));
                }
                if (Objects.isNull(dto.getLowStopLossPoint())) {
                    dto.setLowStopLossPoint(lowBoll - (midBoll - lowBoll));
                }
                // 止盈：突破upboll后回落
                // 止损：再次跌破upboll后
                // 平仓逻辑：止盈：突破boll回落 && 止损：跌破boll
                if ("buy".equals(dto.getHavaOrderDirection())
                        && (currentHighPrice >= upBoll && currentPrice <= upBoll
                        || currentPrice <= dto.getLowStopLossPoint()
                        || (currentLowPrice <= dto.getLow30Price() && currObv.compareTo(longHighObv) <= 0)
                )) {
                    request.setOffset("close");
                    request.setDirection("sell");
                    dto.setCurrentTakeOrder(true);
                    System.out.println("***" + currentTime + "当前为【波段】，已【平多】仓" + request.getVolume() + "张！！");
                    //为空头设平仓逻辑：
                } else if ("sell".equals(dto.getHavaOrderDirection())
                        && (currentLowPrice <= lowBoll && currentPrice >= lowBoll
                        || currentPrice >= dto.getUpStopLossPoint()
                        || (currentHighPrice >= dto.getHigh30Price() && currObv.compareTo(longHighObv) >= 0)
                )) {
                    request.setOffset("close");
                    request.setDirection("buy");
                    dto.setCurrentTakeOrder(true);
                    System.out.println("***" + currentTime + "当前为【波段】行情，已【平空】仓" + request.getVolume() + "张！！");
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

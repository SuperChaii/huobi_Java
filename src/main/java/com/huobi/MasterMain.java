package com.huobi;

import com.huobi.client.ContractClient;
import com.huobi.client.req.contract.*;
import com.huobi.constant.HuobiOptions;
import com.huobi.utils.IndicatrixImpl;
import com.huobi.utils.quant.QuantIndicators;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.huobi.CoreMethods.*;
import static com.huobi.CoreLogic.*;

public class MasterMain {


    public static void main(String[] args) throws InterruptedException {
        ContractUniversalRequest request = new ContractUniversalRequest();
        ContractParmaDto dto = new ContractParmaDto();
        QuantIndicators qiUtils = new QuantIndicators();
        IndicatrixImpl impl = new IndicatrixImpl();
        dto.setSleepMillis(500L);
        //args[]参数校验并为request,dto赋值
        validParams(args, request, dto);
        //获取合约客户端
        ContractClient contractService = ContractClient.create(HuobiOptions.builder()
                //.restHost("https://api.hbdm.com")
                .restHost("https://api.hbdm.vn")
                .apiKey(Constants.API_KEY)
                .secretKey(Constants.SECRET_KEY)
                .build());
        try {
            //初始化持仓情况及最大下单量
            getMarketInfoListByKLine(contractService, request.getContractCode(), dto);
            updateHaveOrderAndVolume(request.getContractCode(), request, dto, contractService);
        } catch (Exception e) {
            updateHaveOrderAndVolume(request.getContractCode(), request, dto, contractService);
            System.out.println("***updateHaveOrderAndVolume:repeat->" + e.getMessage());
            e.printStackTrace();
        }
        while (true) {
            try {
                //获取收盘价，根据K线图
                getMarketInfoListByKLine(contractService, request.getContractCode(), dto);
                try {
                    //下单前更新持仓情况及最大下单量
                    updateHaveOrderAndVolume(request.getContractCode(), request, dto, contractService);
                } catch (Exception e) {
                    Thread.sleep(dto.getSleepMillis());
                    updateHaveOrderAndVolume(request.getContractCode(), request, dto, contractService);
                    System.out.println("***updateHaveOrderAndVolume:repeat->" + e.getMessage());
                    e.printStackTrace();
                }
                //获取BOLL指标
                double[] closePriceArr = getArrayByList(dto.getCloseList());
                dto.setBollArr(qiUtils.boll(closePriceArr));
                //获取EMD指标
                //double[] emaArr = qiUtils.ema(closePriceArr, 21);
                //获取OBV指标,并转BigDecimal
                List<BigDecimal> obvList = new ArrayList<>();
                double[] obvarr =qiUtils.obv(getArrayByList(dto.getCloseList()),getArrayByList(dto.getVolumeList()));
                for (int i = 0; i < obvarr.length; i++) {
                    obvList.add(new BigDecimal(obvarr[i]));
                }
                dto.setObvList(obvList);

                //获取MACD指标
                impl.MACD(dto.getCloseList().toArray(new Double[dto.getCloseList().size()]), 12, 26, 9, dto);

                //核心算法：根据macd & boll & KLine 指标判断是否下单
                getOrderByParams(request, dto);
                //合约下单
                if (dto.getCurrentTakeOrder()) {
                    takeOrder(request.getContractCode(), contractService, request.getVolume(), request.getDirection(),
                            request.getOffset(), request.getLeverRate(), request.getOrderPriceType());
                    Thread.sleep(dto.getSleepMillis());
                }
                //重置做单标识，一定要执行否则循环下单导致空指针！
                dto.setCurrentTakeOrder(false);
                Thread.sleep(dto.getSleepMillis());
            } catch (Exception e) {
                //重置标识，一定要执行否则循环下单导致空指针！
                dto.setCurrentTakeOrder(false);
                e.printStackTrace();
                System.out.println("***" + getTimeFormat(System.currentTimeMillis()) + "-e.getMessage=" + e.getMessage());
                //8小时结算，如报错等待60秒
                Thread.sleep(60 * 1000);
            }
        }
    }

    private static double[] getArrayByList(List<Double> list) {
        Double[] tempArr = (Double[]) list.toArray(new Double[list.size()]);
        double[] arr = new double[tempArr.length];
        for (int i = 0; i < tempArr.length; i++) {
            arr[i] = tempArr[i];
        }
        return arr;
    }


}

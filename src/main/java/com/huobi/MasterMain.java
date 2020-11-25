package com.huobi;

import com.huobi.client.ContractClient;
import com.huobi.client.req.contract.*;
import com.huobi.constant.HuobiOptions;
import com.huobi.utils.IndicatrixImpl;
import com.huobi.utils.quant.QuantIndicators;


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
            getMarketPriceListByKLine(contractService, request.getContractCode(), dto);
            updateHaveOrderAndVolume(request.getContractCode(), request, dto, contractService);
        } catch (Exception e) {
            updateHaveOrderAndVolume(request.getContractCode(), request, dto, contractService);
            System.out.println("***updateHaveOrderAndVolume:repeat->" + e.getMessage());
            e.printStackTrace();
        }
        while (true) {
            try {
                //获取收盘价，根据K线图
                getMarketPriceListByKLine(contractService, request.getContractCode(), dto);
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
                Double[] closePrice = dto.getCloseList().toArray(new Double[dto.getCloseList().size()]);
                double[] doubleArr = new double[closePrice.length];
                for (int i = 0; i < closePrice.length; i++) {
                    doubleArr[i] = closePrice[i];
                }
                double[][] bollArr = qiUtils.boll(doubleArr);
                //获取EMD指标
                //double[] emaArr = qiUtils.ema(doubleArr, 21);
                //获取MACD指标
                impl.MACD(dto.getCloseList().toArray(new Double[dto.getCloseList().size()]), 12, 26, 9, dto);

                //核心算法：根据macd & boll & KLine 指标判断是否下单
                getOrderByParams(request, dto, bollArr);
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


}

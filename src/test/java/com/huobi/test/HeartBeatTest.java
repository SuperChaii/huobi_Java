package com.huobi.test;

import com.alibaba.fastjson.JSONObject;
import com.huobi.client.GenericClient;
import com.huobi.constant.HuobiOptions;

public class HeartBeatTest {
    private static String status = "0";

    public static void main(String[] args) throws InterruptedException {

        while (true) {
            //获取系统客户端 状态
            GenericClient genericService = GenericClient.create(HuobiOptions.builder().restHost("https://api.hbdm.com").build());

            JSONObject json = genericService.getHeartBeat();
            System.out.println(json);
            JSONObject dataJson = json.getJSONObject("data");
            status = dataJson.getString("swap_heartbeat");
            if (!status.equals("0")) {
                return;
            }
            Thread.sleep(5000);
        }
    }
}

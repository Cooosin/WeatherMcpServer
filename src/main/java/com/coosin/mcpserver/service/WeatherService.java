package com.coosin.mcpserver.service;


import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;

/**
 * WeatherService
 *
 * @author HeshunKang
 * @date 2025/04/27 15:28
 */
@Slf4j
@Service
public class WeatherService {

    @Value("${hefeng.weather.host:}")
    private String apiHost;

    @Value("${hefeng.api.key:}")
    private String apiKey;

    @Tool(description = "获取某个城市的实时天气")
    public WeatherFunctionResponse getWeather(WeatherFunctionRequest request) {
        String city = request.getCity();
        log.info("开始获取天气，城市：{}", city);
        // 先调用城市搜索接口，查询到该城市的locationId
        String citySearchApiUrl = apiHost + "/geo/v2/city/lookup";
        @Cleanup
        HttpResponse citySearchHttpResponse = HttpUtil.createGet(citySearchApiUrl)
                .header("Content-Type", "application/json")
                .header("X-QW-Api-Key", apiKey)
                .form("location", city)
                .execute();

        String cityResponseBody = citySearchHttpResponse.body();
        log.info("城市搜索接口返回结果：{}", cityResponseBody);

        String locationId = JSONUtil.getByPath(JSONUtil.parseObj(cityResponseBody), "$.location[0].id", null);

        log.info("城市的locationId为：{}", locationId);
        // 再调用天气接口，获取天气信息
        @Cleanup
        HttpResponse weatherHttpResponse = HttpUtil.createGet(apiHost + "/v7/weather/now")
                .header("Content-Type", "application/json")
                .header("X-QW-Api-Key", apiKey)
                .form("location", locationId)
                .form("lang", "zh")
                .execute();
        String body = weatherHttpResponse.body();
        log.info("天气接口返回结果：{}", body);
        JSONObject jsonObject = JSONUtil.parseObj(body);
        JSONObject now = jsonObject.getJSONObject("now");
        WeatherFunctionResponse response = now.toBean(WeatherFunctionResponse.class);
        return response;
    }
}


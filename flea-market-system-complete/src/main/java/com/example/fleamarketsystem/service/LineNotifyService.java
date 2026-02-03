package com.example.fleamarketsystem.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class LineNotifyService {

    @Value("${line.notify.api.url:https://notify-api.line.me/api/notify}")
    private String lineNotifyApiUrl;

    private final RestTemplate restTemplate;

    public LineNotifyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendMessage(String accessToken, String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBearerAuth(accessToken);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("message", message);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        try {
            restTemplate.postForEntity(lineNotifyApiUrl, request, String.class);
            System.out.println("LINE Notify message sent successfully.");
        } catch (Exception e) {
            System.err.println("Failed to send LINE Notify message: " + e.getMessage());
        }
    }
}

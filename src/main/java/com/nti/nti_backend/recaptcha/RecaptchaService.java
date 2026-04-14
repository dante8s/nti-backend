package com.nti.nti_backend.recaptcha;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecaptchaService {

    @Value("${recaptcha.secret}")
    private String secretKey;

    @Value("${recaptcha.url}")
    private String recaptchaUrl;

    private final RestTemplate restTemplate;

    public boolean verify(String captchaToken) {
        // Якщо токен порожній — одразу відхиляємо
        if (captchaToken == null
                || captchaToken.isBlank()) {
            return false;
        }

        // Відправляємо запит до Google
        String url = recaptchaUrl
                + "?secret=" + secretKey
                + "&response=" + captchaToken;

        Map response = restTemplate.postForObject(
                url, null, Map.class
        );

        // Google повертає {"success": true/false}
        if (response == null) return false;

        return Boolean.TRUE.equals(
                response.get("success")
        );
    }
}
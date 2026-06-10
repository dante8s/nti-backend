package com.nti.nti_backend.recaptcha;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
        if (secretKey == null || secretKey.isBlank()) {
            return true;
        }

        // If the token is empty — reject immediately
        if (captchaToken == null
                || captchaToken.isBlank()) {
            return false;
        }

        // Send request to Google
        String url = recaptchaUrl
                + "?secret=" + secretKey
                + "&response=" + captchaToken;

        Map response = restTemplate.postForObject(
                url, null, Map.class
        );

        // Google returns {"success": true/false}
        if (response == null) return false;

        return Boolean.TRUE.equals(
                response.get("success")
        );
    }
}
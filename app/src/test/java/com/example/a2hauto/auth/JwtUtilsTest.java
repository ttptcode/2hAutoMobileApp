package com.example.a2hauto.auth;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class JwtUtilsTest {

    @Test
    public void extractDisplayName_prefersFullNameClaim() {
        String token = createToken("{\"fullName\":\"Nguyen Van A\",\"role\":\"User\"}");

        assertEquals("Nguyen Van A", JwtUtils.extractDisplayName(token));
    }

    @Test
    public void extractDisplayName_supportsAspNetNameClaim() {
        String token = createToken("{\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\":\"Tran Thi B\"}");

        assertEquals("Tran Thi B", JwtUtils.extractDisplayName(token));
    }

    @Test
    public void extractDisplayName_returnsEmptyForMalformedToken() {
        assertEquals("", JwtUtils.extractDisplayName("not-a-jwt"));
    }

    private String createToken(String payloadJson) {
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64Url(payloadJson);
        return header + "." + payload + ".signature";
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}


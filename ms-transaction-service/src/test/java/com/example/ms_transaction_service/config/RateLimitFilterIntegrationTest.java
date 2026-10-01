package com.example.ms_transaction_service.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = {"rate-limit.per-ip.capacity=3", "rate-limit.global.capacity=5"})
@AutoConfigureMockMvc
class RateLimitFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private MockHttpServletRequestBuilder desdeIp(String ip) {
        return get("/api/movimientos").with(request -> {
            request.setRemoteAddr(ip);
            return request;
        });
    }

    @Test
    void superarCuotaPorIpYGlobalRetornaTooManyRequests() throws Exception {
        // Cuota por IP (3): la cuarta peticion de la misma IP se corta antes de la seguridad.
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(desdeIp("10.0.0.1")).andExpect(status().isUnauthorized());
        }
        mockMvc.perform(desdeIp("10.0.0.1"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(429));

        // Otra IP aun tiene cuota propia, pero el techo global (5) ya casi se agoto.
        mockMvc.perform(desdeIp("10.0.0.2")).andExpect(status().isUnauthorized());
        mockMvc.perform(desdeIp("10.0.0.3")).andExpect(status().isUnauthorized());
        mockMvc.perform(desdeIp("10.0.0.4")).andExpect(status().isTooManyRequests());
    }
}

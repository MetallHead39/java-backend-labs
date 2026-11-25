package com.payment.paymentservice;

import com.payment.common.PaymentDto;
import com.payment.common.PaymentStatus;
import com.payment.paymentservice.controller.PaymentHealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentServiceApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private PaymentHealthController healthController;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoadsAndControllerBeanPresent() {
        assertThat(applicationContext).isNotNull();
        assertThat(healthController).isNotNull();

        PaymentDto sample = new PaymentDto("p-100", BigDecimal.TEN, PaymentStatus.PENDING);
        assertThat(sample.status()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/api/payments/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("payment-service"));
    }
}

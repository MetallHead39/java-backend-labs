package com.payment.settlementservice;

import com.payment.common.SettlementReportDto;
import com.payment.settlementservice.controller.SettlementHealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SettlementServiceApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SettlementHealthController healthController;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoadsAndControllerBeanPresent() {
        assertThat(applicationContext).isNotNull();
        assertThat(healthController).isNotNull();

        SettlementReportDto sample = new SettlementReportDto(LocalDate.now(), 2, BigDecimal.ONE);
        assertThat(sample.totalPayments()).isEqualTo(2);
    }

    @Test
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/api/settlements/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("settlement-service"));
    }
}

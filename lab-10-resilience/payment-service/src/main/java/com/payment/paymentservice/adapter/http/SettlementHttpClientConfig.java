package com.payment.paymentservice.adapter.http;

import com.payment.paymentservice.config.SettlementIntegrationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

/**
 * Spring configuration providing the Declarative HTTP client proxy.
 */
@Configuration
@ConditionalOnProperty(name = "integration.settlement.transport", havingValue = "http", matchIfMissing = true)
public class SettlementHttpClientConfig {

    @Bean
    public SettlementHttpClient settlementHttpClient(SettlementIntegrationProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.http().connectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.http().readTimeoutMs()));

        RestClient restClient = RestClient.builder()
                .baseUrl(properties.http().baseUrl())
                .requestFactory(requestFactory)
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build();

        return factory.createClient(SettlementHttpClient.class);
    }
}

package com.payment.paymentservice.adapter.http;

import com.payment.common.dto.DailyLimitInfo;
import com.payment.common.dto.MerchantFeeInfo;
import com.payment.common.dto.SettlementDayInfo;
import com.payment.paymentservice.exception.RemoteResourceNotFoundException;
import com.payment.paymentservice.exception.RemoteServiceUnavailableException;
import com.payment.paymentservice.port.DailyLimitLookup;
import com.payment.paymentservice.port.MerchantFeeLookup;
import com.payment.paymentservice.port.SettlementDayLookup;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

/**
 * HTTP implementation of the settlement lookup ports using Declarative HTTP Client.
 */
@Component
@ConditionalOnProperty(name = "integration.settlement.transport", havingValue = "http", matchIfMissing = true)
public class SettlementHttpAdapter implements SettlementDayLookup, MerchantFeeLookup, DailyLimitLookup {

    private final SettlementHttpClient httpClient;

    public SettlementHttpAdapter(SettlementHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public SettlementDayInfo getDayStatus(String date) {
        return execute(() -> httpClient.getSettlementDay(date), "Settlement day not found: " + date);
    }

    @Override
    public MerchantFeeInfo getMerchantFee(String merchantId) {
        return execute(() -> httpClient.getMerchantFee(merchantId), "Merchant fee not found: " + merchantId);
    }

    @Override
    public DailyLimitInfo getDailyLimit(String merchantId) {
        return execute(() -> httpClient.getDailyLimit(merchantId), "Daily limit not found: " + merchantId);
    }

    private <T> T execute(Supplier<T> call, String notFoundMessage) {
        try {
            return call.get();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RemoteResourceNotFoundException(notFoundMessage, ex);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new RemoteResourceNotFoundException(notFoundMessage, ex);
            }
            throw new RemoteServiceUnavailableException("Remote HTTP error: " + ex.getStatusCode() + " - " + ex.getMessage(), ex);
        } catch (HttpServerErrorException ex) {
            throw new RemoteServiceUnavailableException("Remote settlement HTTP 5xx error: " + ex.getStatusCode(), ex);
        } catch (ResourceAccessException ex) {
            throw new RemoteServiceUnavailableException("Remote settlement service unavailable or timed out: " + ex.getMessage(), ex);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new RemoteResourceNotFoundException(notFoundMessage, ex);
            }
            throw new RemoteServiceUnavailableException("Remote settlement HTTP client error: " + ex.getStatusCode(), ex);
        } catch (RestClientException ex) {
            throw new RemoteServiceUnavailableException("Remote settlement service request failed: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new RemoteServiceUnavailableException("Unexpected error communicating with remote settlement service: " + ex.getMessage(), ex);
        }
    }
}

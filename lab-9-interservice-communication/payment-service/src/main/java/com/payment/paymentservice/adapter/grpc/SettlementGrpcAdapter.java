package com.payment.paymentservice.adapter.grpc;

import com.payment.common.dto.DailyLimitInfo;
import com.payment.common.dto.MerchantFeeInfo;
import com.payment.common.dto.SettlementDayInfo;
import com.payment.grpc.DailyLimitRequest;
import com.payment.grpc.DailyLimitResponse;
import com.payment.grpc.MerchantFeeRequest;
import com.payment.grpc.MerchantFeeResponse;
import com.payment.grpc.SettlementDayRequest;
import com.payment.grpc.SettlementDayResponse;
import com.payment.grpc.SettlementQueryServiceGrpc;
import com.payment.paymentservice.config.SettlementIntegrationProperties;
import com.payment.paymentservice.exception.RemoteResourceNotFoundException;
import com.payment.paymentservice.exception.RemoteServiceUnavailableException;
import com.payment.paymentservice.port.DailyLimitLookup;
import com.payment.paymentservice.port.MerchantFeeLookup;
import com.payment.paymentservice.port.SettlementDayLookup;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * gRPC implementation of the settlement lookup ports using SettlementQueryServiceBlockingStub.
 */
@Component
@ConditionalOnProperty(name = "integration.settlement.transport", havingValue = "grpc")
public class SettlementGrpcAdapter implements SettlementDayLookup, MerchantFeeLookup, DailyLimitLookup {

    private final ManagedChannel channel;
    private final SettlementIntegrationProperties properties;

    public SettlementGrpcAdapter(ManagedChannel channel, SettlementIntegrationProperties properties) {
        this.channel = channel;
        this.properties = properties;
    }

    private SettlementQueryServiceGrpc.SettlementQueryServiceBlockingStub createStubWithDeadline() {
        return SettlementQueryServiceGrpc.newBlockingStub(channel)
                .withDeadlineAfter(properties.grpc().deadlineMs(), TimeUnit.MILLISECONDS);
    }

    @Override
    public SettlementDayInfo getDayStatus(String date) {
        SettlementDayRequest request = SettlementDayRequest.newBuilder().setDate(date).build();
        SettlementDayResponse response = execute(stub -> stub.getSettlementDay(request), "Settlement day not found: " + date);
        return new SettlementDayInfo(response.getDate(), response.getStatus());
    }

    @Override
    public MerchantFeeInfo getMerchantFee(String merchantId) {
        MerchantFeeRequest request = MerchantFeeRequest.newBuilder().setMerchantId(merchantId).build();
        MerchantFeeResponse response = execute(stub -> stub.getMerchantFee(request), "Merchant fee not found: " + merchantId);
        return new MerchantFeeInfo(response.getMerchantId(), response.getFeePercent());
    }

    @Override
    public DailyLimitInfo getDailyLimit(String merchantId) {
        DailyLimitRequest request = DailyLimitRequest.newBuilder().setMerchantId(merchantId).build();
        DailyLimitResponse response = execute(stub -> stub.getDailyLimit(request), "Daily limit not found: " + merchantId);
        return new DailyLimitInfo(response.getMerchantId(), response.getRemainingLimit());
    }

    private <T> T execute(Function<SettlementQueryServiceGrpc.SettlementQueryServiceBlockingStub, T> call, String notFoundMessage) {
        try {
            return call.apply(createStubWithDeadline());
        } catch (StatusRuntimeException ex) {
            if (ex.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw new RemoteResourceNotFoundException(
                        ex.getStatus().getDescription() != null ? ex.getStatus().getDescription() : notFoundMessage,
                        ex
                );
            }
            throw new RemoteServiceUnavailableException(
                    "gRPC error [" + ex.getStatus().getCode() + "]: " + ex.getStatus().getDescription(),
                    ex
            );
        } catch (Exception ex) {
            throw new RemoteServiceUnavailableException("Unexpected gRPC error: " + ex.getMessage(), ex);
        }
    }
}

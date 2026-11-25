package com.payment.paymentservice;

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
import com.payment.paymentservice.adapter.grpc.SettlementGrpcAdapter;
import com.payment.paymentservice.config.SettlementIntegrationProperties;
import com.payment.paymentservice.exception.RemoteResourceNotFoundException;
import com.payment.paymentservice.exception.RemoteServiceUnavailableException;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettlementGrpcAdapterTest {

    private Server server;
    private ManagedChannel channel;
    private SettlementGrpcAdapter grpcAdapter;
    private final TestSettlementGrpcServiceImpl serviceImpl = new TestSettlementGrpcServiceImpl();

    @BeforeEach
    void setUp() throws IOException {
        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(serviceImpl)
                .build()
                .start();

        channel = InProcessChannelBuilder.forName(serverName)
                .directExecutor()
                .build();

        SettlementIntegrationProperties properties = new SettlementIntegrationProperties(
                "grpc",
                null,
                new SettlementIntegrationProperties.GrpcProperties("localhost", 9090, 500)
        );

        grpcAdapter = new SettlementGrpcAdapter(channel, properties);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (channel != null) {
            channel.shutdown();
            channel.awaitTermination(3, TimeUnit.SECONDS);
        }
        if (server != null) {
            server.shutdown();
            server.awaitTermination(3, TimeUnit.SECONDS);
        }
    }

    @Test
    void shouldSuccessfullyRetrieveSettlementDay() {
        SettlementDayInfo info = grpcAdapter.getDayStatus("2026-11-25");
        assertThat(info.date()).isEqualTo("2026-11-25");
        assertThat(info.status()).isEqualTo("OPEN");
    }

    @Test
    void shouldSuccessfullyRetrieveMerchantFee() {
        MerchantFeeInfo info = grpcAdapter.getMerchantFee("merchant-1");
        assertThat(info.merchantId()).isEqualTo("merchant-1");
        assertThat(info.feePercent()).isEqualTo(1.5);
    }

    @Test
    void shouldSuccessfullyRetrieveDailyLimit() {
        DailyLimitInfo info = grpcAdapter.getDailyLimit("merchant-1");
        assertThat(info.merchantId()).isEqualTo("merchant-1");
        assertThat(info.remainingLimit()).isEqualTo(5000.0);
    }

    @Test
    void shouldTranslateStatusNotFoundToRemoteResourceNotFoundException() {
        assertThatThrownBy(() -> grpcAdapter.getDayStatus("not-found-date"))
                .isInstanceOf(RemoteResourceNotFoundException.class)
                .hasMessageContaining("Day not found: not-found-date");
    }

    @Test
    void shouldTranslateDeadlineExceededToRemoteServiceUnavailableException() {
        assertThatThrownBy(() -> grpcAdapter.getMerchantFee("slow-merchant"))
                .isInstanceOf(RemoteServiceUnavailableException.class)
                .hasMessageContaining("DEADLINE_EXCEEDED");
    }

    @Test
    void shouldTranslateServerDownToRemoteServiceUnavailableException() {
        server.shutdownNow();
        assertThatThrownBy(() -> grpcAdapter.getDayStatus("2026-11-25"))
                .isInstanceOf(RemoteServiceUnavailableException.class);
    }

    private static class TestSettlementGrpcServiceImpl extends SettlementQueryServiceGrpc.SettlementQueryServiceImplBase {
        @Override
        public void getSettlementDay(SettlementDayRequest request, StreamObserver<SettlementDayResponse> responseObserver) {
            if ("not-found-date".equals(request.getDate())) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("Day not found: " + request.getDate()).asRuntimeException());
                return;
            }
            responseObserver.onNext(SettlementDayResponse.newBuilder()
                    .setDate(request.getDate())
                    .setStatus("OPEN")
                    .build());
            responseObserver.onCompleted();
        }

        @Override
        public void getMerchantFee(MerchantFeeRequest request, StreamObserver<MerchantFeeResponse> responseObserver) {
            if ("slow-merchant".equals(request.getMerchantId())) {
                try {
                    Thread.sleep(700);
                } catch (InterruptedException ignored) {
                }
            }
            responseObserver.onNext(MerchantFeeResponse.newBuilder()
                    .setMerchantId(request.getMerchantId())
                    .setFeePercent(1.5)
                    .build());
            responseObserver.onCompleted();
        }

        @Override
        public void getDailyLimit(DailyLimitRequest request, StreamObserver<DailyLimitResponse> responseObserver) {
            responseObserver.onNext(DailyLimitResponse.newBuilder()
                    .setMerchantId(request.getMerchantId())
                    .setRemainingLimit(5000.0)
                    .build());
            responseObserver.onCompleted();
        }
    }
}

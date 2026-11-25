package com.payment.settlementservice;

import com.payment.grpc.DailyLimitRequest;
import com.payment.grpc.DailyLimitResponse;
import com.payment.grpc.MerchantFeeRequest;
import com.payment.grpc.MerchantFeeResponse;
import com.payment.grpc.SettlementDayRequest;
import com.payment.grpc.SettlementDayResponse;
import com.payment.grpc.SettlementQueryServiceGrpc;
import com.payment.settlementservice.grpc.GrpcServerLifecycle;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "grpc.server.enabled=true",
                "grpc.server.port=0"
        }
)
class SettlementGrpcServiceTest {

    @Autowired
    private GrpcServerLifecycle grpcServerLifecycle;

    private ManagedChannel channel;
    private SettlementQueryServiceGrpc.SettlementQueryServiceBlockingStub stub;

    @BeforeEach
    void setUp() {
        int port = grpcServerLifecycle.getPort();
        channel = ManagedChannelBuilder.forAddress("localhost", port)
                .usePlaintext()
                .build();
        stub = SettlementQueryServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (channel != null) {
            channel.shutdown();
            channel.awaitTermination(3, TimeUnit.SECONDS);
        }
    }

    @Test
    void shouldReturnSettlementDayViaGrpc() {
        SettlementDayResponse response = stub.getSettlementDay(
                SettlementDayRequest.newBuilder().setDate("2026-11-25").build()
        );
        assertThat(response.getDate()).isEqualTo("2026-11-25");
        assertThat(response.getStatus()).isEqualTo("OPEN");
    }

    @Test
    void shouldReturnNotFoundWhenSettlementDayMissing() {
        assertThatThrownBy(() -> stub.getSettlementDay(
                SettlementDayRequest.newBuilder().setDate("2000-01-01").build()
        ))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(ex -> assertThat(((StatusRuntimeException) ex).getStatus().getCode())
                        .isEqualTo(Status.Code.NOT_FOUND));
    }

    @Test
    void shouldReturnMerchantFeeViaGrpc() {
        MerchantFeeResponse response = stub.getMerchantFee(
                MerchantFeeRequest.newBuilder().setMerchantId("merchant-1").build()
        );
        assertThat(response.getMerchantId()).isEqualTo("merchant-1");
        assertThat(response.getFeePercent()).isEqualTo(1.5);
    }

    @Test
    void shouldReturnNotFoundWhenMerchantFeeMissing() {
        assertThatThrownBy(() -> stub.getMerchantFee(
                MerchantFeeRequest.newBuilder().setMerchantId("unknown").build()
        ))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(ex -> assertThat(((StatusRuntimeException) ex).getStatus().getCode())
                        .isEqualTo(Status.Code.NOT_FOUND));
    }

    @Test
    void shouldReturnDailyLimitViaGrpc() {
        DailyLimitResponse response = stub.getDailyLimit(
                DailyLimitRequest.newBuilder().setMerchantId("merchant-2").build()
        );
        assertThat(response.getMerchantId()).isEqualTo("merchant-2");
        assertThat(response.getRemainingLimit()).isEqualTo(1200.0);
    }

    @Test
    void shouldReturnNotFoundWhenDailyLimitMissing() {
        assertThatThrownBy(() -> stub.getDailyLimit(
                DailyLimitRequest.newBuilder().setMerchantId("unknown").build()
        ))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(ex -> assertThat(((StatusRuntimeException) ex).getStatus().getCode())
                        .isEqualTo(Status.Code.NOT_FOUND));
    }
}

package com.payment.settlementservice.grpc;

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
import com.payment.settlementservice.service.SettlementQueryService;
import com.payment.settlementservice.service.SettlementResourceNotFoundException;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * gRPC service implementation exposing settlement lookup RPCs.
 */
@Service
public class SettlementGrpcServiceImpl extends SettlementQueryServiceGrpc.SettlementQueryServiceImplBase {

    private final SettlementQueryService settlementQueryService;

    public SettlementGrpcServiceImpl(SettlementQueryService settlementQueryService) {
        this.settlementQueryService = settlementQueryService;
    }

    @Override
    public void getSettlementDay(SettlementDayRequest request,
                                 StreamObserver<SettlementDayResponse> responseObserver) {
        executeUnary(responseObserver, () -> {
            SettlementDayInfo info = settlementQueryService.getSettlementDay(request.getDate());
            return SettlementDayResponse.newBuilder()
                    .setDate(info.date())
                    .setStatus(info.status())
                    .build();
        });
    }

    @Override
    public void getMerchantFee(MerchantFeeRequest request,
                               StreamObserver<MerchantFeeResponse> responseObserver) {
        executeUnary(responseObserver, () -> {
            MerchantFeeInfo info = settlementQueryService.getMerchantFee(request.getMerchantId());
            return MerchantFeeResponse.newBuilder()
                    .setMerchantId(info.merchantId())
                    .setFeePercent(info.feePercent())
                    .build();
        });
    }

    @Override
    public void getDailyLimit(DailyLimitRequest request,
                              StreamObserver<DailyLimitResponse> responseObserver) {
        executeUnary(responseObserver, () -> {
            DailyLimitInfo info = settlementQueryService.getDailyLimit(request.getMerchantId());
            return DailyLimitResponse.newBuilder()
                    .setMerchantId(info.merchantId())
                    .setRemainingLimit(info.remainingLimit())
                    .build();
        });
    }

    private <T> void executeUnary(StreamObserver<T> responseObserver, Supplier<T> action) {
        try {
            T response = action.get();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (SettlementResourceNotFoundException ex) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(ex.getMessage())
                    .asRuntimeException());
        }
    }
}

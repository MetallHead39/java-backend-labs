package com.payment.settlementservice.repository;

import com.payment.settlementservice.entity.MerchantFeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantFeeRepository extends JpaRepository<MerchantFeeEntity, String> {
}

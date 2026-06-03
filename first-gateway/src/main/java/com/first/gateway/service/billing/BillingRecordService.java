package com.first.gateway.service.billing;

import com.first.gateway.domain.entity.BillingRecord;
import com.first.gateway.domain.enums.BillingRecordType;
import com.first.gateway.repository.BillingRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingRecordService {

    private final BillingRecordRepository billingRecordRepository;

    public BillingRecordService(BillingRecordRepository billingRecordRepository) {
        this.billingRecordRepository = billingRecordRepository;
    }

    @Transactional
    public void record(Long tenantId,
                       Long userId,
                       BillingRecordType type,
                       long amount,
                       Long refLogId,
                       String remark) {
        BillingRecord record = new BillingRecord();
        record.setTenantId(tenantId);
        record.setUserId(userId);
        record.setType(type.name());
        record.setAmount(amount);
        record.setRefLogId(refLogId);
        record.setRemark(remark);
        billingRecordRepository.save(record);
    }
}

package com.first.gateway.service.billing;

import com.first.gateway.domain.entity.ChannelModel;
import com.first.gateway.domain.entity.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public final class BillingCostCalculator {

    private static final long DEFAULT_ESTIMATE_TOKENS = 2048L;

    private BillingCostCalculator() {}

    public static long estimateTokens(Map<String, Object> request) {
        Object maxTokens = request.get("max_tokens");
        if (maxTokens instanceof Number number && number.longValue() > 0) {
            return number.longValue();
        }
        return DEFAULT_ESTIMATE_TOKENS;
    }

    public static long computeCost(int promptTokens,
                                   int completionTokens,
                                   ChannelModel model,
                                   BigDecimal groupRatio) {
        BigDecimal inputRatio = model.getInputRatio() != null ? model.getInputRatio() : BigDecimal.ONE;
        BigDecimal outputRatio = model.getOutputRatio() != null ? model.getOutputRatio() : BigDecimal.ONE;
        BigDecimal ratio = groupRatio != null ? groupRatio : BigDecimal.ONE;

        BigDecimal base = inputRatio.multiply(BigDecimal.valueOf(Math.max(promptTokens, 0)))
            .add(outputRatio.multiply(BigDecimal.valueOf(Math.max(completionTokens, 0))));
        return base.multiply(ratio).setScale(0, RoundingMode.CEILING).longValue();
    }

    public static long estimateReserveCost(Map<String, Object> request,
                                           ChannelModel model,
                                           BigDecimal groupRatio) {
        long tokens = estimateTokens(request);
        BigDecimal inputRatio = model.getInputRatio() != null ? model.getInputRatio() : BigDecimal.ONE;
        BigDecimal outputRatio = model.getOutputRatio() != null ? model.getOutputRatio() : BigDecimal.ONE;
        BigDecimal ratio = groupRatio != null ? groupRatio : BigDecimal.ONE;
        BigDecimal maxRatio = inputRatio.max(outputRatio);
        return maxRatio.multiply(BigDecimal.valueOf(tokens)).multiply(ratio)
            .setScale(0, RoundingMode.CEILING).longValue();
    }

    public static long embeddingCost(int promptTokens, ChannelModel model, BigDecimal groupRatio) {
        return computeCost(promptTokens, 0, model, groupRatio);
    }
}

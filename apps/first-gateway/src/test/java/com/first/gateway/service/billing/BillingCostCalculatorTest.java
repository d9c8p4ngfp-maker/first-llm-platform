package com.first.gateway.service.billing;

import com.first.gateway.domain.entity.ChannelModel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BillingCostCalculatorTest {

    @Test
    void computeCost_basicFormula() {
        ChannelModel model = model(2.0, 4.0);
        long cost = BillingCostCalculator.computeCost(10, 5, model, BigDecimal.ONE);
        assertEquals(40, cost);
    }

    @Test
    void computeCost_withGroupRatio() {
        ChannelModel model = model(1.0, 2.0);
        long cost = BillingCostCalculator.computeCost(100, 50, model, new BigDecimal("1.5"));
        assertEquals(300, cost);
    }

    @Test
    void computeCost_nullRatiosFallbackToOne() {
        ChannelModel model = new ChannelModel();
        long cost = BillingCostCalculator.computeCost(10, 5, model, null);
        assertEquals(15, cost);
    }

    @Test
    void computeCost_zeroTokens() {
        ChannelModel model = model(2.0, 3.0);
        assertEquals(0, BillingCostCalculator.computeCost(0, 0, model, BigDecimal.ONE));
    }

    @Test
    void computeCost_negativeTokensClamped() {
        ChannelModel model = model(2.0, 3.0);
        assertEquals(0, BillingCostCalculator.computeCost(-5, -3, model, BigDecimal.ONE));
    }

    @Test
    void computeCost_roundingCeiling() {
        ChannelModel model = model(new BigDecimal("1.5"), BigDecimal.ONE);
        assertEquals(2, BillingCostCalculator.computeCost(1, 0, model, BigDecimal.ONE));
    }

    @Test
    void estimateTokens_usesMaxTokens() {
        assertEquals(512L, BillingCostCalculator.estimateTokens(Map.of("max_tokens", 512)));
    }

    @Test
    void estimateTokens_defaultsTo2048() {
        assertEquals(2048L, BillingCostCalculator.estimateTokens(Map.of("model", "x")));
    }

    @Test
    void estimateReserveCost_usesMaxRatio() {
        ChannelModel model = model(1.0, 3.0);
        long cost = BillingCostCalculator.estimateReserveCost(
            Map.of("max_tokens", 100), model, new BigDecimal("2"));
        assertEquals(600, cost);
    }

    @Test
    void embeddingCost_onlyPromptTokens() {
        ChannelModel model = model(2.0, 5.0);
        long cost = BillingCostCalculator.embeddingCost(100, model, new BigDecimal("1.5"));
        assertEquals(300, cost);
    }

    private static ChannelModel model(double input, double output) {
        return model(BigDecimal.valueOf(input), BigDecimal.valueOf(output));
    }

    private static ChannelModel model(BigDecimal input, BigDecimal output) {
        ChannelModel model = new ChannelModel();
        model.setInputRatio(input);
        model.setOutputRatio(output);
        return model;
    }
}

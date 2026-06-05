package com.first.gateway.domain.entity;

import com.first.gateway.domain.enums.ModelTier;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "channel_model")
@Getter
@Setter
public class ChannelModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "model_alias", length = 100)
    private String modelAlias;

    @Column(name = "input_ratio", nullable = false, precision = 10, scale = 4)
    private BigDecimal inputRatio = BigDecimal.ONE;

    @Column(name = "output_ratio", nullable = false, precision = 10, scale = 4)
    private BigDecimal outputRatio = BigDecimal.ONE;

    @Column(name = "cache_ratio", nullable = false, precision = 10, scale = 4)
    private BigDecimal cacheRatio = BigDecimal.ONE;

    @Column(name = "max_context")
    private Integer maxContext;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModelTier tier = ModelTier.STANDARD;

    @Column(name = "model_type", nullable = false, length = 20)
    private String modelType = "CHAT";

    @Column(nullable = false)
    private Short enabled = 1;
}

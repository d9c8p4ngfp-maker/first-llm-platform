package com.first.gateway.repository;

import com.first.gateway.domain.entity.ChannelModel;
import com.first.gateway.domain.enums.ModelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChannelModelRepository extends JpaRepository<ChannelModel, Long> {

    List<ChannelModel> findByEnabledOrderByModelNameAsc(Short enabled);

    List<ChannelModel> findByChannelIdAndEnabled(Long channelId, Short enabled);

    List<ChannelModel> findByModelNameAndEnabled(String modelName, Short enabled);

    @Query("""
        SELECT cm FROM ChannelModel cm
        WHERE cm.enabled = 1 AND (cm.modelName = :model OR cm.modelAlias = :model)
        """)
    List<ChannelModel> findByModelNameOrAliasAndEnabled(@Param("model") String model);

    @Query("""
        SELECT cm FROM ChannelModel cm
        WHERE (cm.modelName = :model OR cm.modelAlias = :model)
          AND cm.enabled = 1
          AND cm.modelType = :modelType
        """)
    List<ChannelModel> findByModelNameOrAliasAndEnabledAndModelType(
        @Param("model") String model, @Param("modelType") ModelType modelType);

    @Query("""
        SELECT CASE WHEN COUNT(cm) > 0 THEN true ELSE false END
        FROM ChannelModel cm
        WHERE cm.modelName = :model OR cm.modelAlias = :model
        """)
    boolean existsByModelNameOrAlias(@Param("model") String model);

    @Query("""
        SELECT cm FROM ChannelModel cm, Channel c
        WHERE cm.channelId = c.id
          AND cm.enabled = 1
          AND c.deleted = 0
          AND c.userId = :userId
        ORDER BY cm.modelName ASC
        """)
    List<ChannelModel> findEnabledByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT cm FROM ChannelModel cm
        JOIN Channel c ON cm.channelId = c.id
        WHERE cm.modelType = :modelType
          AND cm.enabled = 1
          AND c.status = 'ACTIVE'
          AND c.deleted = 0
        ORDER BY c.priority DESC, c.weight DESC
        """)
    List<ChannelModel> findByModelTypeEnabled(@Param("modelType") ModelType modelType);
}
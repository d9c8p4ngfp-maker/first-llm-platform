package com.first.gateway.service.user;

import com.first.gateway.domain.entity.UserGroup;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.UserGroupRepository;
import com.first.gateway.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserGroupServiceTest {

    @Mock
    private UserGroupRepository userGroupRepository;
    @Mock
    private UserRepository userRepository;

    private UserGroupService userGroupService;

    @BeforeEach
    void setUp() {
        userGroupService = new UserGroupService(userGroupRepository, userRepository);
    }

    @Test
    void ratioForUser_returnsGroupRatio() {
        UserGroup group = new UserGroup();
        group.setId(1L);
        group.setRatio(new BigDecimal("1.500"));

        com.first.gateway.domain.entity.User user = new com.first.gateway.domain.entity.User();
        user.setGroupId(1L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        assertEquals(new BigDecimal("1.500"), userGroupService.ratioForUser(2L));
    }

    @Test
    void delete_rejectsDefaultGroup() {
        UserGroup group = new UserGroup();
        group.setId(1L);
        group.setName("default");
        when(userGroupRepository.findByName("default")).thenReturn(Optional.of(group));

        GatewayException ex = assertThrows(GatewayException.class, () -> userGroupService.delete(1L));

        assertEquals(GatewayError.INVALID_REQUEST, ex.getError());
    }
}

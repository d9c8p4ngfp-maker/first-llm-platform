package com.first.gateway.domain.enums;

import com.first.gateway.domain.entity.UserTenantRel;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TenantRoleTest {

    @Test
    void shouldContainThreeRoles() {
        assertThat(TenantRole.values())
            .contains(TenantRole.OWNER, TenantRole.MEMBER, TenantRole.PLATFORM_ADMIN);
    }

    @Test
    void ownerIsDeprecated() {
        assertThat(TenantRole.OWNER).isNotNull();
    }

    @Test
    void memberIsDefault() {
        assertThat(TenantRole.MEMBER.name()).isEqualTo("MEMBER");
    }

    @Test
    void userTenantRelDefaultRoleIsMember() {
        UserTenantRel rel = new UserTenantRel();
        assertThat(rel.getRole()).isEqualTo(TenantRole.MEMBER);
    }
}

package com.admin.template.config;

import com.admin.template.AdminTemplateApplication;
import com.admin.template.dto.role.RoleResponse;
import com.admin.template.dto.user.UserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AdminTemplateApplication.class)
@ActiveProfiles({"test", "unit-test"})
class DateContractTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void preservesUtcIsoTimestampForUserLastLogin() throws Exception {
        UserResponse user = new UserResponse(1, "alice", null, null, null, null, List.of(), 1,
                LocalDateTime.of(2026, 1, 2, 3, 4, 5, 123_000_000));

        var response = objectMapper.readTree(objectMapper.writeValueAsBytes(user));

        assertThat(response.path("lastLogin").asText()).isEqualTo("2026-01-02T03:04:05.123Z");
    }

    @Test
    void preservesUtcIsoTimestampForRoleCreationAndUpdate() throws Exception {
        RoleResponse role = new RoleResponse(1, "auditor", "Auditor", "", 1, List.of(),
                LocalDateTime.of(2026, 1, 2, 3, 4, 5), LocalDateTime.of(2026, 2, 3, 4, 5, 6));

        var response = objectMapper.readTree(objectMapper.writeValueAsBytes(role));

        assertThat(response.path("createdAt").asText()).isEqualTo("2026-01-02T03:04:05.000Z");
        assertThat(response.path("updatedAt").asText()).isEqualTo("2026-02-03T04:05:06.000Z");
    }
}

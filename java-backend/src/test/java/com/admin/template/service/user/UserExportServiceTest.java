package com.admin.template.service.user;

import com.admin.template.dto.user.UserResponse;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserExportServiceTest {
    @Test
    void createsXlsxWithExistingChineseHeadersAndUserValues() throws Exception {
        UserResponse user = new UserResponse(7, "alice", "Alice", "a@example.com", "12345678", null,
                List.of("operator", "auditor"), 1, LocalDateTime.of(2026, 1, 2, 3, 4));

        byte[] bytes = new UserExportService().createUserListBuffer(List.of(user));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet("用户列表");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("ID");
            assertThat(sheet.getRow(0).getCell(6).getStringCellValue()).isEqualTo("角色标识");
            assertThat(sheet.getRow(1).getCell(6).getStringCellValue()).isEqualTo("operator,auditor");
            assertThat(org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(sheet.getRow(1).getCell(7)))
                    .isTrue();
            assertThat(sheet.getRow(1).getCell(7).getLocalDateTimeCellValue())
                    .isEqualTo(LocalDateTime.of(2026, 1, 2, 3, 4));
        }
    }
}

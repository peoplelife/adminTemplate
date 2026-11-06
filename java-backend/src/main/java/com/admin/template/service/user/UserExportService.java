package com.admin.template.service.user;

import com.admin.template.dto.user.UserResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class UserExportService {
    private static final List<String> HEADERS = List.of("ID", "用户名", "昵称", "邮箱", "手机号", "状态", "角色标识", "最近登录时间");
    private static final int[] WIDTHS = {10, 20, 20, 30, 18, 10, 24, 24};

    public byte[] createUserListBuffer(List<UserResponse> users) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.getProperties().getCoreProperties().setCreator("Admin Template");
            var sheet = workbook.createSheet("用户列表");
            sheet.createFreezePane(0, 1);
            CellStyle headerStyle = workbook.createCellStyle();
            var font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
            var header = sheet.createRow(0);
            for (int column = 0; column < HEADERS.size(); column++) {
                Cell cell = header.createCell(column);
                cell.setCellValue(HEADERS.get(column));
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(column, WIDTHS[column] * 256);
            }
            int rowIndex = 1;
            for (UserResponse user : users) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(user.id());
                setText(row, 1, user.username());
                setText(row, 2, user.nickname());
                setText(row, 3, user.email());
                setText(row, 4, user.phone());
                row.createCell(5).setCellValue(user.status());
                setText(row, 6, user.roleCodes() == null ? "" : String.join(",", user.roleCodes()));
                if (user.lastLogin() != null) {
                    Cell dateCell = row.createCell(7);
                    dateCell.setCellValue(user.lastLogin());
                    dateCell.setCellStyle(dateStyle);
                }
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create user export", exception);
        }
    }

    private void setText(Row row, int column, String value) {
        row.createCell(column).setCellValue(value == null ? "" : value);
    }
}

package com.genie.admin.audit.application;

import java.io.IOException;
import java.io.Writer;
import java.time.format.DateTimeFormatter;

/**
 * Ghi CSV nhật ký kiểm toán (plan nhật ký kiểm toán L04.01): BOM UTF-8 (Excel nhận đúng tiếng Việt), tiêu đề tiếng Việt,
 * thoát theo RFC 4180, <b>vô hiệu công thức</b> (ô bắt đầu bằng {@code = + - @}, tab, CR thêm {@code '} ở đầu — chống
 * CSV injection), thời gian giờ Việt Nam {@code yyyy-MM-dd HH:mm:ss}, xuống dòng CRLF.
 */
public final class AuditLogCsvWriter {

    static final String BOM = "﻿";
    static final String HEADER = "Thời gian,User,Vai trò,Hành động,Nhóm,Đối tượng,IP,Kết quả";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Writer out;

    public AuditLogCsvWriter(Writer out) {
        this.out = out;
    }

    public void writeHeader() throws IOException {
        out.write(BOM);
        out.write(HEADER);
        out.write("\r\n");
    }

    public void writeRow(AuditLogView v) throws IOException {
        String[] cells = {
            v.createdAt().atZoneSameInstant(AuditLogQueryService.VN).format(TIME),
            v.actorEmail(), v.actorRole(), v.action(), v.category().name(), v.target(), v.ipAddress(), v.result()
        };
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                out.write(',');
            }
            out.write(cell(cells[i]));
        }
        out.write("\r\n");
    }

    /** Một ô: vô hiệu công thức rồi thoát RFC 4180 khi có dấu phẩy / nháy kép / xuống dòng. */
    static String cell(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String v = value;
        char first = v.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
            v = "'" + v;
        }
        if (v.indexOf(',') >= 0 || v.indexOf('"') >= 0 || v.indexOf('\n') >= 0 || v.indexOf('\r') >= 0) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}

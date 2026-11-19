package com.genie.admin.audit.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.common.audit.AuditCategory;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/** Bộ ghi CSV nhật ký (plan nhật ký kiểm toán L04.01): BOM, thoát RFC 4180, chống CSV injection, giờ Việt Nam. */
class AuditLogCsvWriterTest {

    @Test
    void escapesAndNeutralizesCells() {
        assertThat(AuditLogCsvWriter.cell("a,b")).isEqualTo("\"a,b\"");
        assertThat(AuditLogCsvWriter.cell("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(AuditLogCsvWriter.cell("x\ny")).isEqualTo("\"x\ny\"");
        assertThat(AuditLogCsvWriter.cell("=1+1")).isEqualTo("'=1+1");
        assertThat(AuditLogCsvWriter.cell("@SUM(A1)")).isEqualTo("'@SUM(A1)");
        assertThat(AuditLogCsvWriter.cell("-2,3")).isEqualTo("\"'-2,3\"");
        assertThat(AuditLogCsvWriter.cell(null)).isEmpty();
        assertThat(AuditLogCsvWriter.cell("plain")).isEqualTo("plain");
    }

    @Test
    void writesBomHeaderAndVietnamTime() throws Exception {
        StringWriter out = new StringWriter();
        AuditLogCsvWriter csv = new AuditLogCsvWriter(out);
        csv.writeHeader();
        csv.writeRow(new AuditLogView("1", OffsetDateTime.parse("2026-06-08T02:42:11Z"), "phat.nt@genie.example",
                "MODERATOR", "KYC_APPROVED", AuditCategory.KYC, "KYC-1037 · TravelGear", "113.161.40.12", "SUCCESS"));
        assertThat(out.toString()).startsWith("﻿Thời gian,User,Vai trò,Hành động,Nhóm,Đối tượng,IP,Kết quả\r\n")
                .endsWith("2026-06-08 09:42:11,phat.nt@genie.example,MODERATOR,KYC_APPROVED,KYC,KYC-1037 · TravelGear,"
                        + "113.161.40.12,SUCCESS\r\n");
    }
}

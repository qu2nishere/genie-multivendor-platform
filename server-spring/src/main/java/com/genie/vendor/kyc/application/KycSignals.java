package com.genie.vendor.kyc.application;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Nhãn tiếng Việt của tín hiệu rủi ro KYC (ENTITY 1.6) — ghép lý do từ chối tự động (UC-VND-03 ràng buộc N6). */
public final class KycSignals {

    private KycSignals() {
    }

    /** Tín hiệu dựa trên khuôn mặt — bỏ qua khi người bán không đồng ý xử lý sinh trắc. */
    public static final Set<String> FACE = Set.of("FACE_DUPLICATE", "FACE_SCORE_BORDERLINE", "FACE_SCORE_LOW",
            "LIVENESS_SUSPECT");

    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("QR_MRZ_MISMATCH", "thông tin mã QR và dòng MRZ trên CCCD không khớp nhau"),
            Map.entry("ID_STRUCTURE_MISMATCH", "số CCCD không đúng cấu trúc"),
            Map.entry("EXPIRY_RULE_MISMATCH", "ngày hết hạn CCCD không đúng quy định"),
            Map.entry("DUPLICATE_ID", "số CCCD đã được dùng cho một shop khác"),
            Map.entry("FACE_DUPLICATE", "khuôn mặt trùng với hồ sơ của người khác"),
            Map.entry("FACE_SCORE_BORDERLINE", "ảnh chân dung chưa khớp rõ với ảnh trên CCCD"),
            Map.entry("FACE_SCORE_LOW", "ảnh chân dung không khớp với ảnh trên CCCD"),
            Map.entry("LIVENESS_SUSPECT", "ảnh chân dung có dấu hiệu chụp lại, không phải người thật"),
            Map.entry("LEGACY_CARD", "thẻ mẫu cũ cần đối chiếu thêm"),
            Map.entry("DECLARED_DATA_MISMATCH", "thông tin kê khai không khớp với giấy tờ"),
            Map.entry("UNREADABLE_IMAGE", "ảnh giấy tờ mờ, không đọc được"));

    public static String label(String code) {
        return LABELS.getOrDefault(code, code);
    }

    /** Lý do từ chối tự động: liệt kê tín hiệu + hướng dẫn nộp lại / yêu cầu nhân viên xem xét. */
    public static String rejectionReason(Collection<String> codes) {
        String list = codes.stream().map(KycSignals::label).collect(Collectors.joining("; "));
        return "Hồ sơ chưa đạt thẩm định tự động: " + (list.isEmpty() ? "điểm rủi ro cao" : list)
                + ". Bạn có thể nộp lại hồ sơ hoặc bấm \"Yêu cầu nhân viên xem xét\".";
    }

    public static List<String> labels(Collection<String> codes) {
        return codes.stream().map(KycSignals::label).toList();
    }
}

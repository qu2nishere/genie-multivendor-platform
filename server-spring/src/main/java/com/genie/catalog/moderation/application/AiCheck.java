// Nhãn "AI đã / chưa đánh giá" cho lần gửi duyệt hiện tại — UC-CAT-12/13 (update_gemini.md mục 1)
package com.genie.catalog.moderation.application;

public enum AiCheck {
    NOT_SCREENED,
    FAILED,
    PASSED,
    SUSPECTED,
    REJECTED;

    /** {@code decision == null} nghĩa là chưa có kết quả cho lần gửi duyệt hiện tại. */
    public static AiCheck of(String decision, Integer riskScore, Boolean fallback, int rejectThreshold) {
        if (decision == null) return NOT_SCREENED;
        if (Boolean.TRUE.equals(fallback)) return FAILED;
        return switch (decision) {
            case "APPROVE" -> PASSED;
            case "REJECT" -> riskScore != null && riskScore >= rejectThreshold ? REJECTED : SUSPECTED;
            default -> SUSPECTED; // NEED_REVIEW
        };
    }
}

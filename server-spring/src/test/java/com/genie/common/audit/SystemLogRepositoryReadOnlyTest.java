package com.genie.common.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Modifying;

/** Nhật ký kiểm toán bất biến (UC-ADM-07, plan nhật ký kiểm toán N2, L02.05): repository không tự khai báo đường sửa / xóa. */
class SystemLogRepositoryReadOnlyTest {

    @Test
    void declaresNoUpdateOrDeleteMethods() {
        Method[] declared = SystemLogRepository.class.getDeclaredMethods();
        assertThat(Arrays.stream(declared).map(Method::getName))
                .noneMatch(n -> n.startsWith("delete") || n.startsWith("update") || n.startsWith("remove"));
        assertThat(declared).noneMatch(m -> m.isAnnotationPresent(Modifying.class));
    }

    @Test
    void escapesLikeWildcards() {
        assertThat(SystemLogSpecs.escapeLike("50%_a\\b")).isEqualTo("50\\%\\_a\\\\b");
    }
}

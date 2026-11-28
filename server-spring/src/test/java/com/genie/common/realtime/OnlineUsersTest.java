// Tra người đang có kết nối WebSocket (plan 2026-11-26 thông báo N02.06)
package com.genie.common.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;

class OnlineUsersTest {

    private static SimpUser user(String name, boolean sessions) {
        SimpUser u = mock(SimpUser.class);
        when(u.getName()).thenReturn(name);
        when(u.hasSessions()).thenReturn(sessions);
        return u;
    }

    @Test
    void reportsOnlyUsersWithOpenSessions() {
        SimpUserRegistry registry = mock(SimpUserRegistry.class);
        SimpUser a = user("7", true);
        SimpUser b = user("8", false);
        SimpUser weird = user("anonymous", true);
        when(registry.getUser("7")).thenReturn(a);
        when(registry.getUser("8")).thenReturn(b);
        when(registry.getUsers()).thenReturn(Set.of(a, b, weird));

        OnlineUsers online = new OnlineUsers(registry);

        assertThat(online.isOnline(7)).isTrue();
        assertThat(online.isOnline(8)).isFalse();
        assertThat(online.isOnline(9)).isFalse();
        assertThat(online.onlineAmong(List.of(9L, 7L, 8L))).containsExactly(7L);
        assertThat(online.allOnline()).containsExactly(7L);
    }
}

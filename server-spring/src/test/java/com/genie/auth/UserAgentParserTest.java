package com.genie.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.auth.token.application.UserAgentParser;
import org.junit.jupiter.api.Test;

class UserAgentParserTest {

    @Test
    void parsesCommonBrowsers() {
        assertThat(UserAgentParser.parse("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Chrome/126.0.0.0 Safari/537.36").name()).isEqualTo("Windows PC · Chrome 126");
        assertThat(UserAgentParser.parse("Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 "
                + "(KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"))
                .isEqualTo(new UserAgentParser.Device("iPhone · Safari 17", UserAgentParser.DeviceType.MOBILE));
        assertThat(UserAgentParser.parse("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0").name()).isEqualTo("Mac · Edge 126");
        assertThat(UserAgentParser.parse(null).type()).isEqualTo(UserAgentParser.DeviceType.DESKTOP);
    }
}

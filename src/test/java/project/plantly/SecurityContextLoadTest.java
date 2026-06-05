package project.plantly;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;


//test 프로파일(H2 임베디드)로 전체 애플리케이션 컨텍스트를 띄워
//세션 기반 SecurityConfig의 빈 구성이 정상적으로 와이어링되는지 검증한다.

@ActiveProfiles("test")
@SpringBootTest
class SecurityContextLoadTest {

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private SecurityContextRepository securityContextRepository;

    @Autowired
    private RememberMeServices rememberMeServices;

    @Test
    @DisplayName("세션 인증 보안 빈들이 정상 구성된다")
    void securityBeansAreWired() {
        assertThat(securityFilterChain).isNotNull();
        assertThat(authenticationManager).isNotNull();
        assertThat(securityContextRepository).isNotNull();
        assertThat(rememberMeServices).isNotNull();
    }
}
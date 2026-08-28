package project.plantly.global.config;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.RememberMeAuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import project.plantly.global.security.CustomRememberMeServices;
import project.plantly.global.security.CustomUserDetailsService;
import project.plantly.global.security.handler.RestAccessDeniedHandler;
import project.plantly.global.security.handler.RestAuthenticationEntryPoint;
import project.plantly.global.security.handler.RestLogoutSuccessHandler;

import java.util.List;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final int REMEMBER_ME_VALIDITY_SECONDS = 60 * 60 * 24 * 30; // 30일

    private final CustomUserDetailsService customUserDetailsService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final RestLogoutSuccessHandler logoutSuccessHandler;

    @Value("${app.remember-me.key}")
    private String rememberMeKey;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS: FE/BE origin이 다를 때 자격증명(쿠키) 포함 요청 허용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF: SPA 대응 - 토큰을 읽을 수 있는 쿠키(XSRF-TOKEN)로 발급
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))

                // 커스텀 로그인/리멤버미 필터가 사용할 AuthenticationManager 명시
                .authenticationManager(authenticationManager())

                // 인증 컨텍스트를 세션에 저장/로드할 저장소 (AuthService와 동일 빈 사용)
                .securityContext(sc -> sc.securityContextRepository(securityContextRepository()))

                // 세션: 필요 시 생성 + 세션 고정 보호(기본 changeSessionId)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.changeSessionId()))

                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/sign-up", "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        // 인증 옵션 목록은 누구에게나 허용 — 검색 필터 패널은 비로그인도 쓴다.
                        // (/api/v1/admin/certifications 와 경로가 달라 관리자 경로는 영향 없다)
                        .requestMatchers(HttpMethod.GET, "/api/v1/certifications").permitAll()
                        // 산업 옵션 목록도 같은 이유로 공개 — 검색 패싯(industryIds)의 선택지다.
                        .requestMatchers(HttpMethod.GET, "/api/v1/industries").permitAll()
                        // 카테고리 트리도 같은 이유로 공개 — 검색 패싯(categoryIds)의 선택지다.
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories").permitAll()
                        // 국가 목록도 공개. 검색 패싯은 아니고 등록/수정 폼의 수출국 선택지다.
                        .requestMatchers(HttpMethod.GET, "/api/v1/countries").permitAll()
                        // 국내 지역 트리도 공개 — 등록/수정 폼의 커버리지 선택지이자 향후 검색 패싯이다.
                        .requestMatchers(HttpMethod.GET, "/api/v1/domestic-regions").permitAll()
                        // 업로드된 이미지 서빙은 공개. 이 파일들은 공개 회사 상세/카드에 실려 나가므로
                        // 인증을 걸면 브라우저의 <img> 가 읽지 못한다. 반대로 업로드(POST /api/v1/uploads)는
                        // 아래 anyRequest().authenticated() 에 그대로 걸려 로그인이 필요하다 - 여기서 여는 것은
                        // 읽기뿐이고, key 가 UUID 라 주소를 모르면 발행 전 초안 이미지에 닿을 수도 없다.
                        .requestMatchers(HttpMethod.GET, "/api/v1/files/{key}").permitAll()
                        // 공개 회사 목록/검색은 누구에게나 허용. (단일 세그먼트라 /{id}·/private·/admin 과 구분된다)
                        .requestMatchers(HttpMethod.GET, "/api/v1/companies").permitAll()
                        // 메인 화면 노출 영역(스팟라이트/추천)도 공개. /{id} permitAll 로도 통과하지만,
                        // 'my'/'favorites' 와 나란히 두어 단일 세그먼트 경로의 의도를 한 곳에서 읽히게 한다.
                        .requestMatchers(HttpMethod.GET, "/api/v1/companies/showcase").permitAll()
                        // 현황 지표도 공개. 로그인 여부와 무관한 집계 숫자만 나간다.
                        .requestMatchers(HttpMethod.GET, "/api/v1/companies/stats").permitAll()
                        // 내 회사 목록은 인증 필수. /{id} permitAll 이 'my' 도 단일 세그먼트로 잡으므로 반드시 그 앞에 둔다.
                        .requestMatchers(HttpMethod.GET, "/api/v1/companies/my").authenticated()
                        // 내 즐겨찾기 목록도 인증 필수. 'my' 와 같은 이유로 /{id} permitAll 앞에 둔다.
                        .requestMatchers(HttpMethod.GET, "/api/v1/companies/favorites").authenticated()
                        // 공개 회사 상세 조회는 누구에게나 허용(공개 안전 필드만 반환). 소유자 전용(.../private)·관리자 경로는 제외된다.
                        .requestMatchers(HttpMethod.GET, "/api/v1/companies/{id}").permitAll()
                        // 관리자 전용 엔드포인트 추가 시: .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())

                // remember=true 시 30일 영속 로그인
                .rememberMe(remember -> remember
                        .rememberMeServices(rememberMeServices())
                        .key(rememberMeKey))

                // 로그아웃: 세션 무효화 + 쿠키 제거 (remember-me 쿠키는 RememberMeServices가 LogoutHandler로 자동 제거)
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler(logoutSuccessHandler))

                // 필터 단계 인증/인가 실패를 동일 ApiResponse 포맷으로 매핑
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))

                // h2-console iframe 렌더링 허용 (로컬 개발용)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider daoProvider = new DaoAuthenticationProvider(customUserDetailsService);
        daoProvider.setPasswordEncoder(passwordEncoder());

        RememberMeAuthenticationProvider rememberMeProvider = new RememberMeAuthenticationProvider(rememberMeKey);

        return new ProviderManager(daoProvider, rememberMeProvider);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public RememberMeServices rememberMeServices() {
        CustomRememberMeServices services = new CustomRememberMeServices(rememberMeKey, customUserDetailsService);
        services.setTokenValiditySeconds(REMEMBER_ME_VALIDITY_SECONDS);
        return services;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // 세션/리멤버미 쿠키 전송 허용
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
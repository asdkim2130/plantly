package project.plantly.userTest;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import project.plantly.domain.user.User;
import project.plantly.domain.user.UserController;
import project.plantly.domain.user.UserService;
import project.plantly.domain.user.dto.request.SignUpRequest;
import project.plantly.domain.user.dto.request.UpdateProfileRequest;
import project.plantly.domain.user.dto.response.AdminUserListResponse;
import project.plantly.domain.user.dto.response.ProfileResponse;
import project.plantly.domain.user.enums.UserRole;
import project.plantly.domain.user.enums.UserStatus;
import project.plantly.domain.user.exception.UserErrorCode;
import project.plantly.global.PageInfo;
import project.plantly.global.PageResponse;
import project.plantly.global.exception.BusinessException;
import project.plantly.global.security.UserPrincipal;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import project.plantly.domain.user.dto.response.UserDetailResponse;
import project.plantly.domain.user.enums.UserGrade;


import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


@ActiveProfiles("test")
@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(UserControllerTest.MethodSecurityTestConfig.class)
public class UserControllerTest {

    // @PreAuthorize 를 슬라이스 테스트에서 강제하기 위한 메서드 시큐리티 활성화
    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig { }


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("정상 요청이면 200과 성공 메세지 반환")
    public void signUp_success () throws Exception {
        SignUpRequest request = new SignUpRequest("test@example.com", "rawPassword!",  "rawPassword!", "홍길동", "01012345678");

        mockMvc.perform(post("/api/v1/users/sign-up")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."));
    }

    @Test
    @DisplayName("중복 이메일이면 409와 에러 메세지 반환")
    public void signup_duplicateEmail () throws Exception {
        SignUpRequest request = new SignUpRequest("test@example.com", "rawPassword!", "rawPassword!", "홍길동", "01012345678");

        willThrow(new BusinessException(UserErrorCode.DUPLICATE_EMAIL))
                .given(userService).createUser(any(SignUpRequest.class));

        mockMvc.perform(post("/api/v1/users/sign-up")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("이미 사용 중인 이메일입니다."));

    }

    @Test
    @DisplayName("이메일 형식이 잘못되면 400과 검증 에러 반환")
    public void signup_validationFail () throws Exception {
        SignUpRequest request = new SignUpRequest("testexample.com", "rawPassword!", "rawPassword!", "홍길동", "01012345678");

        mockMvc.perform(post("/api/v1/users/sign-up")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());

    }

    @Test
    @DisplayName("인증된 유저 프로프리 200 반환")
    public void getProfile_success () throws Exception {
        Long userId = 1L;

        ProfileResponse profile = new ProfileResponse(
                "email@example.com",
                "홍길동",
                null,
                "01012345678",
                UserStatus.ACTIVE,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );

        given(userService.getMyProfile(userId)).willReturn(profile);
        authenticate(userId);

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("email@example.com"))   // ApiResponse 래퍼 키 확인
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.userStatus").value("ACTIVE"));
    }

    @Test
    @DisplayName("유저 없음 404 반환")
    public void getProfile_userNotFount () throws Exception {
        Long userId = 1L;
        given(userService.getMyProfile(userId))
                .willThrow(new BusinessException(UserErrorCode.USER_NOT_FOUND));
        authenticate(userId);

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isNotFound());

    }

    @Test
    @DisplayName("회원 프로필 수정시 200과 성공 메세지 반환")
    public void profileUpdate_success () throws Exception {
        Long userId = 1L;

        ProfileResponse profile = new ProfileResponse(
                "email@example.com",
                "홍길동",
                null,
                "01012345678",
                UserStatus.ACTIVE,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );

        authenticate(userId);
        UpdateProfileRequest request = new UpdateProfileRequest(null, "닉네임", null);

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("프로필 수정이 완료되었습니다."));
    }

    @Test
    @DisplayName("관리자가 회원 상세 조회시 200과 데이터 반환")
    public void getUserDetailForAdmin_admin_success () throws Exception {
        Long targetUserId = 2L;

        UserDetailResponse response = UserDetailResponse.builder()
                .id(targetUserId)
                .email("target@example.com")
                .name("대상회원")
                .nickname("닉네임")
                .phone("01099998888")
                .userStatus(UserStatus.ACTIVE)
                .userGrade(UserGrade.BASIC)
                .userRole(UserRole.MEMBER)
                .trialEndDate(LocalDateTime.of(2026, 1, 1, 0, 0))
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .deletedAt(null)
                .build();

        given(userService.getUserDetailForAdmin(targetUserId)).willReturn(response);
        authenticate(1L, UserRole.ADMIN);  // 관리자 권한으로 인증

        mockMvc.perform(get("/api/v1/admin/users/{userId}", targetUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.email").value("target@example.com"))
                .andExpect(jsonPath("$.data.userRole").value("MEMBER"));
    }

    @Test
    @DisplayName("권한이 없는 회원이 회원 상세 조회시 403 반환")
    public void getUserDetailForAdmin_member_forbidden() throws Exception {
        Long targetId = 2L;
        authenticate(1L, UserRole.MEMBER);  //일반 권한으로 인증

        mockMvc.perform(get("/api/v1/admin/users/{userId}", targetId))
                .andDo(print())
                .andExpect(status().isForbidden());

        // 인가 단계에서 막혀 서비스는 호출되지 않아야 함
        verify(userService, never()).getUserDetailForAdmin(anyLong());
    }

    @Test
    @DisplayName("관리자가 회원 목록 조회 시 200과 페이지 데이터 반환")
    public void getUserListForAdmin_admin_success() throws Exception {
        //given
        AdminUserListResponse content = new AdminUserListResponse(
                "target@example.com", "대상회원", "01099998888",
                UserGrade.BASIC, LocalDateTime.of(2026, 1, 1, 0, 0),
                UserRole.MEMBER, UserStatus.ACTIVE
        );

        PageInfo pageInfo = new PageInfo(1, 30, 1, 1);

        PageResponse<AdminUserListResponse> response = new PageResponse<>(List.of(content), pageInfo);

        given(userService.getUserListForAdmin(any(Pageable.class))).willReturn(response);
        authenticate(1L, UserRole.ADMIN);

        //when & then
        mockMvc.perform(get("/api/v1/admin/users")
                .param("page", "0")
                .param("size", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].email").value("target@example.com"))
                .andExpect(jsonPath("$.data.content[0].userRole").value("MEMBER"))
                .andExpect(jsonPath("$.data.pageInfo.pageNumber").value(1))
                .andExpect(jsonPath("$.data.pageInfo.totalElement").value(1));
    }

    @Test
    @DisplayName("권한 없는 회원이 회원 목록 조회 시 403 반환")
    public void getUserListForAdmin_member_forbidden () throws Exception {
        //given
        authenticate(1L, UserRole.MEMBER);

        mockMvc.perform(get("/api/v1/admin/users"))
                .andDo(print())
                .andExpect(status().isForbidden());

        // 인가 단계에서 Block -> 서비스 호출 되지 않음 확인
        verify(userService, never()).getUserListForAdmin(any(Pageable.class));

    }


    // 테스트 격리: 매 테스트 후 SecurityContext 비우기
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long userId) {
        authenticate(userId, UserRole.MEMBER);
    }


    // addFilters = false 라 보안 필터가 SecurityContext 를 채워주지 않으므로 직접 주입
    // (@AuthenticationPrincipal 리졸버는 SecurityContextHolder 에서 직접 읽음)
    private void authenticate(Long userId, UserRole role) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authOf(userId, role));
        SecurityContextHolder.setContext(context);
    }

    // 주어진 userId 를 가진 UserPrincipal 로 인증 객체 생성
    private Authentication authOf(Long userId, UserRole role) {
        User user = BeanUtils.instantiateClass(User.class);
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(user, "userStatus", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "userRole", role);

        UserPrincipal principal = new UserPrincipal(user);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}

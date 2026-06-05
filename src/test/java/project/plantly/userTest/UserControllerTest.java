package project.plantly.userTest;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import project.plantly.domain.user.UserController;
import project.plantly.domain.user.UserService;
import project.plantly.domain.user.dto.request.SignUpRequest;
import project.plantly.domain.user.exception.UserErrorCode;
import project.plantly.global.exception.BusinessException;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ActiveProfiles("test")
@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("정상 요청이면 200과 성공 메세지 반환")
    public void signUp_success () throws Exception {
        SignUpRequest request = new SignUpRequest("test@example.com", "rawPassword!", "홍길동", "01012345678");

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
        SignUpRequest request = new SignUpRequest("test@example.com", "rawPassword!", "홍길동", "01012345678");

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
        SignUpRequest request = new SignUpRequest("testexample.com", "rawPassword!", "홍길동", "01012345678");

        mockMvc.perform(post("/api/v1/users/sign-up")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());

    }
}

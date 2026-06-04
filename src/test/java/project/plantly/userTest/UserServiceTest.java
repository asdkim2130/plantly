package project.plantly.userTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import project.plantly.domain.user.User;
import project.plantly.domain.user.UserRepository;
import project.plantly.domain.user.UserService;
import project.plantly.domain.user.dto.request.SignUpRequest;
import project.plantly.domain.user.exception.UserErrorCode;
import project.plantly.global.exception.BusinessException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private SignUpRequest signUpRequest (){
        return new SignUpRequest(
                "test@example.com",
                "rawPassword",
                "홍길동",
                "01012345678"
        );
    }

    @Test
    @DisplayName("회원가입 성공")
    public void 회원가입 (){
        //given
        SignUpRequest request = signUpRequest();
        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");

        //when
        userService.createUser(request);

        //then
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("test@example.com");
        assertThat(saved.getPassword()).isEqualTo("encodedPassword");
    }

    @Test
    @DisplayName("이메일 중복 Duplicate_email 예외 반환")
    public void 이메일중복 (){
        //given
        SignUpRequest request = signUpRequest();
        given(userRepository.existsByEmail(request.email())).willReturn(true);

        //when & then
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(UserErrorCode.DUPLICATE_EMAIL);

        verify(userRepository, never()).save(any(User.class));  // 저장 없음
        verify(passwordEncoder, never()).encode(anyString());  // 비밀번호 인코딩 없음
    }

}

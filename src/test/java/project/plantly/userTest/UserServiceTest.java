package project.plantly.userTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import project.plantly.domain.user.User;
import project.plantly.domain.user.UserRepository;
import project.plantly.domain.user.UserService;
import project.plantly.domain.auth.dto.request.SignUpRequest;
import project.plantly.domain.user.dto.request.UpdateProfileRequest;
import project.plantly.domain.user.dto.response.ProfileResponse;
import project.plantly.domain.user.enums.UserStatus;
import project.plantly.domain.user.exception.UserErrorCode;
import project.plantly.global.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.Optional;

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

    @Test
    @DisplayName("사용자가 프로필 조회 성공시 ProfileResponse 반환")
    public void getUserProfile_success (){
        //given
        Long userId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.of(userFixture(userId)));

        //when
        ProfileResponse result = userService.getUserProfile(userId);

        //then
        assertThat(result.email()).isEqualTo("email@example.com");
        assertThat(result.name()).isEqualTo("홍길동");
        assertThat(result.phone()).isEqualTo("01012345678");
        assertThat(result.nickname()).isNull();
        assertThat(result.userStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(result.trialEndDate()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
        assertThat(result.createdAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    @Test
    @DisplayName("사용자가 프로필 조회 실패시 USER_NOT_FOUNT 예외")
    public void getUserProfile_userNotFound(){
        //given
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        //when/then
        assertThatThrownBy(() -> userService.getUserProfile(userId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("사용자가 프로필 수정 성공시 ProfileResponse 반환")
    public void updateProfile_success (){
        //given
        Long userId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.of(userFixture(userId)));

        //when
        ProfileResponse profile = userService.updateUserProfile(userId, new UpdateProfileRequest(null, "닉네임", null));

        //then
        assertThat(profile.email()).isEqualTo("email@example.com");
        assertThat(profile.name()).isEqualTo("홍길동");
        assertThat(profile.phone()).isEqualTo("01012345678");
        assertThat(profile.nickname()).isEqualTo("닉네임");
        assertThat(profile.userStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(profile.createdAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
        assertThat(profile.trialEndDate()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));

    }



    private User userFixture (Long id){
        User user = BeanUtils.instantiateClass(User.class);

        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "email", "email@example.com");
        ReflectionTestUtils.setField(user, "name", "홍길동");
        ReflectionTestUtils.setField(user, "phone", "01012345678");
        ReflectionTestUtils.setField(user, "userStatus", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "trialEndDate", LocalDateTime.of(2026, 1, 1, 0, 0));
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.of(2026, 1, 1, 0, 0));

        return user;

    }

}

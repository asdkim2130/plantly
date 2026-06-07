package project.plantly.domain.user;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.domain.auth.dto.request.SignUpRequest;
import project.plantly.domain.user.dto.request.UpdateProfileRequest;
import project.plantly.domain.user.dto.response.UserDetailResponse;
import project.plantly.domain.user.dto.response.ProfileResponse;
import project.plantly.domain.user.exception.UserErrorCode;
import project.plantly.global.exception.BusinessException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    //회원가입
    public void createUser (SignUpRequest request){

        if(userRepository.existsByEmail(request.email())){
            throw new BusinessException(UserErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.create(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                request.phone());

        try {
            userRepository.save(user);
        }catch (DataIntegrityViolationException e){
            throw new BusinessException(UserErrorCode.DUPLICATE_EMAIL);
        }

    }

    // 회원 자신의 프로필 조회
    public ProfileResponse getMyProfile (Long userId){

        User user = userRepository.findById(userId).orElseThrow(
                () -> new BusinessException(UserErrorCode.USER_NOT_FOUND)
        );

        return ProfileResponse.from(user);
    }

    // 회원 프로필 수정
    @Transactional
    public ProfileResponse updateUserProfile (Long userId, UpdateProfileRequest request){

        User user = userRepository.findById(userId).orElseThrow(
                () -> new BusinessException(UserErrorCode.USER_NOT_FOUND)
        );

        user.update(request.name(), request.nickname(), request.phone());

        return ProfileResponse.from(user);

    }

    @Transactional(readOnly = true)
    public UserDetailResponse getUserDetailForAdmin (Long userId){

        User user = userRepository.findById(userId).orElseThrow(
                () -> new BusinessException(UserErrorCode.USER_NOT_FOUND)
        );

        return UserDetailResponse.from(user);
    }

}
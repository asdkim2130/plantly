package project.plantly.domain.user;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import project.plantly.domain.user.dto.request.SignUpRequest;
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
    public ProfileResponse getUserProfile (Long userId){

        User user = userRepository.findById(userId).orElseThrow(
                () -> new BusinessException(UserErrorCode.USER_NOT_FOUND)
        );

        return ProfileResponse.form(user);
    }

}
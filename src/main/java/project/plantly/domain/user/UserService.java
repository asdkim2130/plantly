package project.plantly.domain.user;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import project.plantly.domain.user.dto.request.LoginRequest;
import project.plantly.domain.user.dto.request.SignUpRequest;
import project.plantly.domain.user.dto.response.LoginResponse;
import project.plantly.domain.user.exception.UserErrorCode;
import project.plantly.global.exception.BusinessException;
import project.plantly.global.security.jwt.JwtProvider;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

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

    //로그인
    public LoginResponse loginUser (LoginRequest request){

        User user = userRepository.findByEmail(request.email()).orElseThrow(
                () -> new BusinessException(UserErrorCode.INVALID_CREDENTIALS)
        );

        if(!passwordEncoder.matches(request.password(), user.getPassword())){
            throw new BusinessException(UserErrorCode.INVALID_CREDENTIALS);
        }

        return LoginResponse.from(user);

    }

}

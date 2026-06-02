package project.plantly.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import project.plantly.domain.user.User;
import project.plantly.domain.user.UserRepository;

@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("회원을 찾을 수 없습니다."));
        return new UserPrincipal(user);
    }

    public UserDetails loadUserById(Long id){
        User user = userRepository.findById(id).orElseThrow(
                () -> new UsernameNotFoundException("회원을 찾을 수 없습니다.")
        );

        return new UserPrincipal(user);


    }
}

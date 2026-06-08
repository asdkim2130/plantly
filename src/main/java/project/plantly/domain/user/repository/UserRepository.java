package project.plantly.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.user.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}

package project.plantly.domain.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import project.plantly.domain.user.enums.UserGrade;
import project.plantly.domain.user.enums.UserRole;
import project.plantly.domain.user.enums.UserStatus;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(unique = true, nullable = false)
    private String email;

    @NotNull
    @Column(nullable = false, length = 60)
    private String password;

    @NotNull
    private String name;

    @NotNull
    private String phone;

    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus userStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserGrade userGrade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole userRole;

    private LocalDateTime trialEndDate;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @Builder
    public User(String email, String password, String name, String phone, String nickname, UserStatus userStatus, UserGrade userGrade, UserRole userRole, LocalDateTime trialEndDate, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.nickname = nickname;
        this.userStatus = (userStatus != null) ? userStatus : UserStatus.ACTIVE;
        this.userGrade = (userGrade != null) ? userGrade : UserGrade.BASIC;
        this.userRole = (userRole != null) ? userRole : UserRole.MEMBER;
        this.trialEndDate = trialEndDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static User create (String email, String encodedPassword, String name, String phone){

        return User.builder()
                .email(email)
                .password(encodedPassword)
                .name(name)
                .phone(phone)
                .build();
    }
}

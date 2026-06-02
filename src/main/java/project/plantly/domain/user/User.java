package project.plantly.domain.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import project.plantly.domain.user.enums.UserGrade;
import project.plantly.domain.user.enums.UserRole;
import project.plantly.domain.user.enums.UserState;

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
    private String password;

    @NotNull
    private String name;

    @NotNull
    private String phone;

    @NotNull
    @Enumerated(EnumType.STRING)
    private UserState userState;

    @NotNull
    @Enumerated(EnumType.STRING)
    private UserGrade userGrade;

    @NotNull
    @Enumerated(EnumType.STRING)
    private UserRole userRole;

    @NotNull
    private boolean isDeleted = false;

    private LocalDateTime trialEndDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @Builder
    public User(String email, String password, String name, String phone, UserState userState, UserGrade userGrade, UserRole userRole, boolean isDeleted, LocalDateTime trialEndDate, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.userState = (userState != null) ? userState : UserState.ACTIVE;
        this.userGrade = (userGrade != null) ? userGrade : UserGrade.BASIC;
        this.userRole = (userRole != null) ? userRole : UserRole.MEMBER;
        this.isDeleted = isDeleted;
        this.trialEndDate = trialEndDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }
}

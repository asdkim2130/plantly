package project.plantly.domain.user.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import project.plantly.domain.user.QUser;
import org.springframework.data.domain.Page;
import project.plantly.domain.user.dto.response.AdminUserRow;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class QUserRepository {

    private final JPAQueryFactory jpaQueryFactory;
    private final QUser qUser = QUser.user;

    // 유저 본체만 투영한다(userId 포함). 소유 회사 구독 배지는 서비스가 userId 로 배치 조회해 병합한다.
    public Page<AdminUserRow> getAdminUsers (Pageable pageable){
        List<AdminUserRow> content = jpaQueryFactory
                .select(Projections.constructor(AdminUserRow.class,
                        qUser.id,
                        qUser.email,
                        qUser.name,
                        qUser.phone,
                        qUser.createdAt,
                        qUser.userRole,
                        qUser.userStatus))
                .from(qUser)
                .where()
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(qUser.id.desc())
                .fetch();

        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(qUser.count())
                .from(qUser);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
}

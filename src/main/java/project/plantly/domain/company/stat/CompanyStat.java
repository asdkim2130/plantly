package project.plantly.domain.company.stat;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회사 단위 집계 카운터(회사당 1행). 좋아요/즐겨찾기/조회 토글 시 함께 증감한다.
// company 는 raw id 참조(회사당 유일). 상세/목록에서 원본 행을 count 하는 대신 이 캐시 값을 노출한다.
// NOTE: 지금은 read-modify-write 로 증감한다. 트래픽이 커지면 UPDATE ... SET like_count = like_count + 1
//       원자적 갱신으로 전환해 경합을 없앤다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"company_id"}))
public class CompanyStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long companyId;

    @Column(nullable = false)
    private long viewCount;

    @Column(nullable = false)
    private long likeCount;

    @Column(nullable = false)
    private long favoriteCount;

    private CompanyStat(Long companyId) {
        this.companyId = companyId;
    }

    // 회사 등록 시점에 0으로 초기화된 집계 행을 생성한다.
    public static CompanyStat init(Long companyId) {
        return new CompanyStat(companyId);
    }

    public void increaseView() {
        this.viewCount++;
    }

    public void increaseLike() {
        this.likeCount++;
    }

    // 카운터는 0 아래로 내려가지 않는다(중복 취소·정합성 오차 방어).
    public void decreaseLike() {
        if (this.likeCount > 0) this.likeCount--;
    }

    public void increaseFavorite() {
        this.favoriteCount++;
    }

    public void decreaseFavorite() {
        if (this.favoriteCount > 0) this.favoriteCount--;
    }
}

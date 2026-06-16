package project.plantly.domain.company;

import java.util.function.IntSupplier;


// CompanyChild 계열(산업군/인증/카테고리 등)의 displayOrder 자동 부여 로직 공통 처리
// 도메인마다 달라지는 "최대 displayOrder 조회" 부분만 maxSupplier로 주입받는다.
public final class DisplayOrders {

    private DisplayOrders() {}


//     displayOrder가 지정돼 있으면 그대로 사용하고,
//     없으면 maxSupplier 결과 + 1 을 부여한다 (max가 -1이면 첫 항목은 0).
    public static int resolve(Integer requested, IntSupplier maxSupplier) {
        return (requested != null) ? requested : maxSupplier.getAsInt() + 1;
    }
}
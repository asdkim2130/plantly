package project.plantly.companyTest.support;

import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.request.ParameterDescriptor;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;

// 회사 등록 API 의 요청/응답 필드 문서 디스크립터.
// 관리자 등록(AdminCompanyController)은 companyCreateRequestFields 를, 유저 자가등록(CompanyController)은
// 신원 3종을 뺀 myCompanyCreateRequestFields 를 쓴다 — 두 경로의 요청 형태가 갈라진 지점이다.
public class CompanyApiDocs {

    // 자가등록 요청은 사업자번호·대표자명·개업일자를 받지 않는다(선행 인증에서만 온다). 대신 verificationId 를 받는다.
    // 공통 필드를 복제하지 않고 관리자용에서 신원 3종만 걷어내 파생시킨다 — 필드가 추가돼도 한 곳만 고치면 된다.
    public static FieldDescriptor[] myCompanyCreateRequestFields() {
        java.util.Set<String> identityFields = java.util.Set.of("businessNumber", "ceoName", "establishmentDate");
        java.util.List<FieldDescriptor> fields = new java.util.ArrayList<>();
        fields.add(fieldWithPath("verificationId").type(JsonFieldType.NUMBER)
                .description("선행 사업자 인증 식별자 (필수). POST /api/v1/companies/verification 응답의 verificationId"));
        for (FieldDescriptor descriptor : companyCreateRequestFields()) {
            if (!identityFields.contains(descriptor.getPath())) {
                fields.add(descriptor);
            }
        }
        return fields.toArray(new FieldDescriptor[0]);
    }

    // 사업자 인증 요청/응답.
    public static FieldDescriptor[] verificationRequestFields() {
        return new FieldDescriptor[]{
                fieldWithPath("businessNumber").type(JsonFieldType.STRING)
                        .description("사업자등록번호 (필수). 하이픈 유무 무관 — 서버가 숫자 10자리로 정규화한다"),
                fieldWithPath("ceoName").type(JsonFieldType.STRING).description("대표자 성명 (필수)"),
                fieldWithPath("businessStartDate").type(JsonFieldType.STRING)
                        .description("개업일자 yyyy-MM-dd (필수). 사업자등록증 기준이며 법인 등기 설립일과 다를 수 있다")
        };
    }

    public static FieldDescriptor[] verificationResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메시지"),
                fieldWithPath("data.verificationId").type(JsonFieldType.NUMBER)
                        .description("발급된 인증 식별자. 회사 등록 요청에 그대로 실어 보낸다"),
                fieldWithPath("data.businessNumber").type(JsonFieldType.STRING).description("정규화된 사업자등록번호"),
                fieldWithPath("data.ceoName").type(JsonFieldType.STRING).description("국세청 검증을 통과한 대표자 성명"),
                fieldWithPath("data.businessStartDate").type(JsonFieldType.STRING).description("국세청 검증을 통과한 개업일자"),
                fieldWithPath("data.expiresAt").type(JsonFieldType.STRING)
                        .description("인증 만료 시각. 이 시각을 넘기면 재인증이 필요하다"),
                fieldWithPath("error").type(JsonFieldType.STRING).optional().description("오류 메시지 (성공 시 null)")
        };
    }

    // 사업자 재인증 요청/응답. 요청에 businessNumber 자리가 없다 — 저장된 번호로만 국세청에 재질의한다(탈취 방지).
    public static FieldDescriptor[] reverificationRequestFields() {
        return new FieldDescriptor[]{
                fieldWithPath("ceoName").type(JsonFieldType.STRING).description("새 대표자 성명 (필수). 국세청 재확인 대상"),
                fieldWithPath("businessStartDate").type(JsonFieldType.STRING)
                        .description("새 개업일자 yyyy-MM-dd (필수). 국세청 정보가 바뀌었을 수 있어 이전 값과 비교하지 않는다")
        };
    }

    public static FieldDescriptor[] reverificationResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메시지"),
                fieldWithPath("data.businessNumber").type(JsonFieldType.STRING)
                        .description("재인증에 사용된 사업자등록번호. 요청으로 받지 않고 저장값을 쓰므로 바뀌지 않는다"),
                fieldWithPath("data.ceoName").type(JsonFieldType.STRING).description("재인증을 통과해 갱신된 대표자 성명"),
                fieldWithPath("data.businessStartDate").type(JsonFieldType.STRING).description("재인증을 통과해 갱신된 개업일자"),
                fieldWithPath("data.verifiedAt").type(JsonFieldType.STRING)
                        .description("이번 재인증 시각. 최초 인증 후 1년 재인증 주기의 기준점이 이 값으로 갱신된다"),
                fieldWithPath("error").type(JsonFieldType.STRING).optional().description("오류 메시지 (성공 시 null)")
        };
    }

    public static FieldDescriptor[] adminVerificationRevokeRequestFields() {
        return new FieldDescriptor[]{
                fieldWithPath("reason").type(JsonFieldType.STRING)
                        .description("회수 사유 (필수). 인증은 향후 혜택 자격 조건이라 되돌린 근거를 반드시 남긴다")
        };
    }

    // CompanyCreateRequest 전체 필드. 발행(publish) 전 임시저장 호환을 위해 companyName/ceoName 외에는 모두 optional.
    public static FieldDescriptor[] companyCreateRequestFields() {
        return new FieldDescriptor[]{
                // ===== 본체 =====
                fieldWithPath("businessNumber").type(JsonFieldType.STRING).optional().description("사업자번호"),
                fieldWithPath("companyName").type(JsonFieldType.STRING).description("기업 이름 (필수)"),
                fieldWithPath("ceoName").type(JsonFieldType.STRING).description("대표자 (필수)"),
                fieldWithPath("establishmentDate").type(JsonFieldType.STRING).optional().description("설립일 (yyyy-MM-dd)"),
                fieldWithPath("postalCode").type(JsonFieldType.STRING).optional().description("우편번호"),
                fieldWithPath("roadAddress").type(JsonFieldType.STRING).optional().description("도로명 주소"),
                fieldWithPath("jibunAddress").type(JsonFieldType.STRING).optional().description("지번 주소"),
                fieldWithPath("detailAddress").type(JsonFieldType.STRING).optional().description("상세주소"),
                fieldWithPath("website").type(JsonFieldType.STRING).optional().description("기업 홈페이지"),
                fieldWithPath("logoUrl").type(JsonFieldType.STRING).optional().description("로고 이미지 URL"),
                fieldWithPath("introTitle").type(JsonFieldType.STRING).optional().description("한 줄 요약"),
                fieldWithPath("content").type(JsonFieldType.STRING).optional().description("소개글"),
                fieldWithPath("trlLevel").type(JsonFieldType.STRING).optional().description("기술성숙도: PROTOTYPE, MASS_PRODUCTION, GLOBAL_STANDARD"),
                fieldWithPath("videoUrl").type(JsonFieldType.STRING).optional().description("동영상 링크 (등급별 사용 제한)"),
                fieldWithPath("leadTime").type(JsonFieldType.STRING).optional().description("예상 리드타임"),
                fieldWithPath("asInfo").type(JsonFieldType.STRING).optional().description("유지보수/AS 정보"),
                fieldWithPath("pricingType").type(JsonFieldType.STRING).optional().description("견적 산출 방식: FIXED, CONSULTATION, PROJECT_BASED"),
                fieldWithPath("brandColor").type(JsonFieldType.STRING).optional().description("브랜드 컬러 (커스텀 불가 등급은 기본값으로 고정)"),
                fieldWithPath("visibility").type(JsonFieldType.STRING).optional().description("공개 범위: PUBLIC(공개), PRIVATE(비공개). 생략 시 PUBLIC"),

                // ===== 자식(소유) 엔티티 =====
                fieldWithPath("contacts").type(JsonFieldType.ARRAY).optional().description("담당자 연락처 목록"),
                fieldWithPath("contacts[].contactName").type(JsonFieldType.STRING).optional().description("담당자명"),
                fieldWithPath("contacts[].position").type(JsonFieldType.STRING).optional().description("직책"),
                fieldWithPath("contacts[].phone").type(JsonFieldType.STRING).optional().description("전화번호"),
                fieldWithPath("contacts[].email").type(JsonFieldType.STRING).optional().description("이메일"),

                fieldWithPath("images").type(JsonFieldType.ARRAY).optional().description("회사 직속 갤러리 이미지 목록 (DETAIL 타입만 허용)"),
                fieldWithPath("images[].imageUrl").type(JsonFieldType.STRING).optional().description("이미지 URL"),
                fieldWithPath("images[].imageType").type(JsonFieldType.STRING).optional().description("이미지 타입 (DETAIL)"),

                fieldWithPath("references").type(JsonFieldType.ARRAY).optional().description("프로젝트 레퍼런스 목록"),
                fieldWithPath("references[].projectTitle").type(JsonFieldType.STRING).optional().description("프로젝트명"),
                fieldWithPath("references[].achievements").type(JsonFieldType.STRING).optional().description("성과"),
                fieldWithPath("references[].partners").type(JsonFieldType.STRING).optional().description("협력사"),
                fieldWithPath("references[].period").type(JsonFieldType.STRING).optional().description("기간"),
                fieldWithPath("references[].imageUrls").type(JsonFieldType.ARRAY).optional().description("레퍼런스 이미지 URL 목록 (등급별 장수 제한)"),

                fieldWithPath("materialNames").type(JsonFieldType.ARRAY).optional().description("취급 소재명 목록"),
                fieldWithPath("equipmentNames").type(JsonFieldType.ARRAY).optional().description("보유 설비명 목록"),
                fieldWithPath("tagNames").type(JsonFieldType.ARRAY).optional().description("태그 목록"),

                // ===== 링크(M:N) 엔티티 - 기존 마스터 ID 참조 =====
                fieldWithPath("categoryIds").type(JsonFieldType.ARRAY).optional().description("카테고리 ID 목록 (등급별 개수 제한)"),
                fieldWithPath("certificationIds").type(JsonFieldType.ARRAY).optional().description("인증 ID 목록"),
                fieldWithPath("countryIds").type(JsonFieldType.ARRAY).optional().description("수출 국가 ID 목록"),
                fieldWithPath("domesticRegionIds").type(JsonFieldType.ARRAY).optional().description("국내 지역 ID 목록"),
                fieldWithPath("industryIds").type(JsonFieldType.ARRAY).optional().description("산업군 ID 목록")
        };
    }

    // 생성 응답(ApiResponse<IdResponse>). error 는 성공 시 직렬화에서 생략된다.
    public static FieldDescriptor[] idResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                fieldWithPath("data").type(JsonFieldType.OBJECT).description("생성 결과"),
                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("생성된 회사 ID"),
                fieldWithPath("error").type(JsonFieldType.STRING).optional().description("에러 메시지 (성공 시 생략됨)")
        };
    }

    // 실패 응답(ApiResponse.failure). success=false + error 만 존재하고 message/data 는 NON_NULL 로 생략된다.
    public static FieldDescriptor[] errorResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부 (실패 시 false)"),
                fieldWithPath("error").type(JsonFieldType.STRING).description("에러 메시지")
        };
    }

    // 본문 없는 성공 응답(ApiResponse.ok). success=true 만 존재하고 message/data/error 는 NON_NULL 로 생략된다.
    // 수정 API(본체 PATCH·컬렉션 PUT)는 변경 결과가 이미 화면에 반영되므로 성공 플래그만 내려준다.
    // 임시저장 저장(PUT)·폐기(DELETE) 도 결과 본문 없이 성공 플래그만 내려주므로 이 디스크립터를 공유한다.
    public static FieldDescriptor[] okResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부 (true)")
        };
    }

    // 임시저장 초안 조회 응답(ApiResponse<CompanyDraftResponse>). data.payload 는 저장했던 자가등록 폼 전체로,
    // 필드 구성이 회사 등록 요청(company-create 스니펫)과 동일하다 — 28개 필드를 data.payload. 접두로 복제하지 않고
    // relaxedResponseFields 와 함께 써서 봉투·payload 객체·저장 시각만 문서화한다(payload 내부는 등록 요청 문서 참조).
    public static FieldDescriptor[] companyDraftResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                fieldWithPath("data.payload").type(JsonFieldType.OBJECT)
                        .description("저장했던 자가등록 폼 상태(부분 입력 가능). 필드 구성은 회사 등록 요청과 동일 — company-create 참조"),
                fieldWithPath("data.updatedAt").type(JsonFieldType.STRING)
                        .description("마지막 저장 시각 (\"n분 전 저장됨\" 등 표시용)")
        };
    }

    // 기본 정보 부분 수정(PATCH /api/v1/companies/{id}) 요청 필드. 모두 선택 = null 이면 미변경(sparse update).
    // 선택 문자열은 빈 문자열("")로 비울 수 있고, 필수(NOT NULL) 필드는 @Size(min=1) 로 빈 문자열을 막는다.
    public static FieldDescriptor[] companyUpdateRequestFields() {
        return new FieldDescriptor[]{
                fieldWithPath("companyName").type(JsonFieldType.STRING).optional().description("기업 이름 (빈 문자열로 비울 수 없음)"),
                fieldWithPath("ceoName").type(JsonFieldType.STRING).optional().description("대표자 (빈 문자열로 비울 수 없음)"),
                fieldWithPath("establishmentDate").type(JsonFieldType.STRING).optional().description("설립일 (yyyy-MM-dd, clear 미지원)"),
                fieldWithPath("postalCode").type(JsonFieldType.STRING).optional().description("우편번호 (빈 문자열로 비울 수 없음)"),
                fieldWithPath("roadAddress").type(JsonFieldType.STRING).optional().description("도로명 주소 (빈 문자열로 비울 수 없음)"),
                fieldWithPath("jibunAddress").type(JsonFieldType.STRING).optional().description("지번 주소 (빈 문자열 = 비우기)"),
                fieldWithPath("detailAddress").type(JsonFieldType.STRING).optional().description("상세주소 (빈 문자열로 비울 수 없음)"),
                fieldWithPath("website").type(JsonFieldType.STRING).optional().description("기업 홈페이지 (빈 문자열 = 비우기)"),
                fieldWithPath("logoUrl").type(JsonFieldType.STRING).optional().description("로고 이미지 URL (빈 문자열로 비울 수 없음)"),
                fieldWithPath("introTitle").type(JsonFieldType.STRING).optional().description("한 줄 요약 (빈 문자열 = 비우기)"),
                fieldWithPath("content").type(JsonFieldType.STRING).optional().description("소개글 (빈 문자열 = 비우기)"),
                fieldWithPath("trlLevel").type(JsonFieldType.STRING).optional().description("기술성숙도: PROTOTYPE, MASS_PRODUCTION, GLOBAL_STANDARD (clear 미지원)"),
                fieldWithPath("videoUrl").type(JsonFieldType.STRING).optional().description("동영상 링크 (등급별 사용 제한, 빈 문자열 = 비우기)"),
                fieldWithPath("leadTime").type(JsonFieldType.STRING).optional().description("예상 리드타임 (빈 문자열 = 비우기)"),
                fieldWithPath("asInfo").type(JsonFieldType.STRING).optional().description("유지보수/AS 정보 (빈 문자열 = 비우기)"),
                fieldWithPath("pricingType").type(JsonFieldType.STRING).optional().description("견적 산출 방식: FIXED, CONSULTATION, PROJECT_BASED (clear 미지원)"),
                fieldWithPath("brandColor").type(JsonFieldType.STRING).optional().description("브랜드 컬러 (커스텀 불가 등급은 무시, 빈 문자열 = 비우기)")
        };
    }

    // 컬렉션 전체 교체(PUT) 요청이 원시 배열 본문일 때(태그·소재·설비 이름 목록, 링크 마스터 ID 목록)의 루트 배열 디스크립터.
    // 빈 배열([])을 보내면 해당 컬렉션을 전부 비운다. 표시 순서는 서버가 배열 인덱스로 재부여한다.
    public static FieldDescriptor[] replaceListRequestFields(String description) {
        return new FieldDescriptor[]{
                fieldWithPath("[]").description(description)
        };
    }

    // 관리자 운영 플래그 조정(PATCH .../{id}/flags) 요청 본문. sparse: 보낸 필드만 목표값으로 설정(멱등).
    public static FieldDescriptor[] adminCompanyFlagsRequestFields() {
        return new FieldDescriptor[]{
                fieldWithPath("verified").type(JsonFieldType.BOOLEAN).optional().description("관리자 인증 노출 여부 (생략 시 미변경)"),
                fieldWithPath("featured").type(JsonFieldType.BOOLEAN).optional().description("추천 노출 여부 (생략 시 미변경)"),
                fieldWithPath("spotlight").type(JsonFieldType.BOOLEAN).optional().description("스팟라이트 노출 여부 (생략 시 미변경)")
        };
    }

    // 공개/비공개 전환(PATCH .../{id}/visibility) 요청 본문. 목표 상태를 그대로 지정한다(멱등).
    public static FieldDescriptor[] companyVisibilityUpdateRequestFields() {
        return new FieldDescriptor[]{
                fieldWithPath("visibility").type(JsonFieldType.STRING).description("전환할 공개 범위: PUBLIC(공개) / PRIVATE(비공개)")
        };
    }

    // 목록/검색 쿼리 파라미터(GET /api/v1/companies). 전부 선택적.
    public static ParameterDescriptor[] companySearchQueryParameters() {
        return new ParameterDescriptor[]{
                parameterWithName("keyword").optional().description("통합 키워드 (공백으로 나눈 토큰 AND, 회사명·소개·레퍼런스 등 전체 텍스트 부분일치)"),
                parameterWithName("companyName").optional().description("고급검색: 회사명"),
                parameterWithName("introTitle").optional().description("고급검색: 한 줄 요약"),
                parameterWithName("content").optional().description("고급검색: 소개글"),
                parameterWithName("ceoName").optional().description("고급검색: 대표자"),
                parameterWithName("address").optional().description("고급검색: 주소"),
                parameterWithName("detailAddress").optional().description("고급검색: 상세주소"),
                parameterWithName("reference").optional().description("고급검색: 레퍼런스(프로젝트명·성과·협력사)"),
                parameterWithName("equipment").optional().description("고급검색: 보유 설비"),
                parameterWithName("material").optional().description("고급검색: 취급 소재"),
                parameterWithName("certificationIds").optional().description(
                        "인증 ID 목록 (같은 구분 내에서는 하나라도 보유, 서로 다른 구분끼리는 모두 보유. "
                                + "예: 경영시스템 2개 + 시장진입 1개 선택 → 경영시스템 중 하나 이상 AND 해당 시장진입 보유)"),
                parameterWithName("industryIds").optional().description("산업군 ID 목록 (선택 중 하나라도 보유)"),
                parameterWithName("categoryIds").optional().description("카테고리 ID 목록 (대분류 선택 시 후손 서브트리까지 매칭)"),
                parameterWithName("page").optional().description("페이지 번호 (1-base 입력)"),
                parameterWithName("size").optional().description("페이지 크기 (기본 20, 최대 100)")
        };
    }

    // 페이징만 있는 목록의 쿼리 파라미터. 내 회사 목록(GET /api/v1/companies/my)과
    // 내 즐겨찾기 목록(GET /api/v1/companies/favorites)이 공유한다 — 둘 다 검색/패싯이 없다.
    public static ParameterDescriptor[] companyMyQueryParameters() {
        return new ParameterDescriptor[]{
                parameterWithName("page").optional().description("페이지 번호 (1-base 입력)"),
                parameterWithName("size").optional().description("페이지 크기 (기본 20)")
        };
    }

    // 목록/검색 응답(ApiResponse<PageResponse<CompanySummary>>). content[] = 요약 카드, pageInfo = 페이지 메타.
    // 내 회사 목록(/my)·내 즐겨찾기 목록(/favorites)도 동일한 요약 카드 페이지 구조라 이 디스크립터를 공유한다.
    public static FieldDescriptor[] companySearchResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("회사 ID"),
                fieldWithPath("data.content[].companyName").type(JsonFieldType.STRING).description("기업 이름"),
                fieldWithPath("data.content[].introTitle").type(JsonFieldType.STRING).optional().description("한 줄 요약 (없으면 null)"),
                fieldWithPath("data.content[].logoUrl").type(JsonFieldType.STRING).description("로고 이미지 URL"),
                fieldWithPath("data.content[].address").type(JsonFieldType.STRING).description("지역 (시도+시군구, 예: \"서울시 강남구\"). 전체 주소는 상세 조회 참고"),
                fieldWithPath("data.content[].verified").type(JsonFieldType.BOOLEAN).description("관리자 인증 여부"),
                fieldWithPath("data.content[].featured").type(JsonFieldType.BOOLEAN).description("추천 노출 여부"),
                fieldWithPath("data.content[].spotlight").type(JsonFieldType.BOOLEAN).description("스팟라이트 노출 여부"),
                fieldWithPath("data.content[].likedByMe").type(JsonFieldType.BOOLEAN).description("로그인 뷰어가 이 회사를 좋아요 했는지 (익명·내 회사 목록은 false)"),
                fieldWithPath("data.content[].favoritedByMe").type(JsonFieldType.BOOLEAN).description("로그인 뷰어가 이 회사를 즐겨찾기 했는지 (익명·내 회사 목록은 false / 즐겨찾기 목록은 정의상 항상 true)"),
                fieldWithPath("data.content[].categoryNames").type(JsonFieldType.ARRAY).description("회사가 연결한 카테고리명 목록"),
                fieldWithPath("data.content[].tagNames").type(JsonFieldType.ARRAY).description("태그명 목록"),
                fieldWithPath("data.content[].industryNames").type(JsonFieldType.ARRAY).description("산업군명 목록"),
                fieldWithPath("data.pageInfo.pageNumber").type(JsonFieldType.NUMBER).description("현재 페이지 (1-base)"),
                fieldWithPath("data.pageInfo.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                fieldWithPath("data.pageInfo.totalElement").type(JsonFieldType.NUMBER).description("전체 건수"),
                fieldWithPath("data.pageInfo.totalPage").type(JsonFieldType.NUMBER).description("전체 페이지 수")
        };
    }

    // 관리자 목록 쿼리 파라미터(GET /api/v1/admin/companies). 전부 선택적, 지정된 것만 AND(교집합).
    public static ParameterDescriptor[] adminCompanyQueryParameters() {
        return new ParameterDescriptor[]{
                parameterWithName("verified").optional().description("인증 여부 (true/false, 생략 시 상관없음)"),
                parameterWithName("featured").optional().description("추천 여부 (true/false, 생략 시 상관없음)"),
                parameterWithName("spotlight").optional().description("스팟라이트 여부 (true/false, 생략 시 상관없음)"),
                parameterWithName("deleted").optional().description("삭제 여부 (생략=삭제 포함 전체, true=삭제만, false=활성만)"),
                parameterWithName("companyName").optional().description("회사명 부분일치"),
                parameterWithName("ownerUserId").optional().description("소유자 유저 ID 정확 일치"),
                parameterWithName("page").optional().description("페이지 번호 (1-base 입력)"),
                parameterWithName("size").optional().description("페이지 크기 (기본 20)")
        };
    }

    // 관리자 목록 응답(ApiResponse<PageResponse<AdminCompanySummary>>). 공개 카드 + 운영 필드(deleted/소유자/출처/등록시각).
    public static FieldDescriptor[] adminCompanyListResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("회사 ID"),
                fieldWithPath("data.content[].companyName").type(JsonFieldType.STRING).description("기업 이름"),
                fieldWithPath("data.content[].introTitle").type(JsonFieldType.STRING).optional().description("한 줄 요약 (없으면 null)"),
                fieldWithPath("data.content[].logoUrl").type(JsonFieldType.STRING).description("로고 이미지 URL"),
                fieldWithPath("data.content[].address").type(JsonFieldType.STRING).description("지역 (시도+시군구, 예: \"서울시 강남구\"). 전체 주소는 상세 조회 참고"),
                fieldWithPath("data.content[].verified").type(JsonFieldType.BOOLEAN).description("관리자 인증 여부"),
                fieldWithPath("data.content[].featured").type(JsonFieldType.BOOLEAN).description("추천 노출 여부"),
                fieldWithPath("data.content[].spotlight").type(JsonFieldType.BOOLEAN).description("스팟라이트 노출 여부"),
                fieldWithPath("data.content[].deleted").type(JsonFieldType.BOOLEAN).description("소프트 삭제 여부"),
                fieldWithPath("data.content[].visibility").type(JsonFieldType.STRING).description("공개 범위 (PUBLIC / PRIVATE). 비공개도 관리자 목록엔 노출"),
                fieldWithPath("data.content[].ownerUserId").type(JsonFieldType.NUMBER).optional().description("소유 유저 ID (미연동이면 null)"),
                fieldWithPath("data.content[].registrationSource").type(JsonFieldType.STRING).description("등록 출처 (USER / ADMIN)"),
                fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING).description("등록 시각"),
                fieldWithPath("data.content[].effectiveGrade").type(JsonFieldType.STRING).optional().description("지금 유효한 구독 등급 (만료 시 FREE 로 강등). 구독 없으면 null"),
                fieldWithPath("data.content[].status").type(JsonFieldType.STRING).optional().description("구독 상태: ACTIVE(결제), TRIAL(체험), ADMIN_EXEMPT(면제). 체험/결제 구분용. 구독 없으면 null"),
                fieldWithPath("data.content[].expiresAt").type(JsonFieldType.STRING).optional().description("구독 만료일 (yyyy-MM-dd, null = 무기한 또는 구독 없음)"),
                fieldWithPath("data.content[].categoryNames").type(JsonFieldType.ARRAY).description("회사가 연결한 카테고리명 목록"),
                fieldWithPath("data.content[].tagNames").type(JsonFieldType.ARRAY).description("태그명 목록"),
                fieldWithPath("data.content[].industryNames").type(JsonFieldType.ARRAY).description("산업군명 목록"),
                fieldWithPath("data.pageInfo.pageNumber").type(JsonFieldType.NUMBER).description("현재 페이지 (1-base)"),
                fieldWithPath("data.pageInfo.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                fieldWithPath("data.pageInfo.totalElement").type(JsonFieldType.NUMBER).description("전체 건수"),
                fieldWithPath("data.pageInfo.totalPage").type(JsonFieldType.NUMBER).description("전체 페이지 수")
        };
    }

    // 소유자 구독 조회 응답(ApiResponse<CompanySubscriptionResponse>). 회사 데이터와 섞지 않은 구독 단독 정보.
    public static FieldDescriptor[] companySubscriptionResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                fieldWithPath("data.companyId").type(JsonFieldType.NUMBER).description("구독 주체(회사) ID"),
                fieldWithPath("data.companyName").type(JsonFieldType.STRING).description("구독 주체(회사) 이름"),
                fieldWithPath("data.grade").type(JsonFieldType.STRING).description("계약(저장) 등급: FREE, BASIC, STANDARD, PREMIUM, ENTERPRISE"),
                fieldWithPath("data.effectiveGrade").type(JsonFieldType.STRING).description("지금 유효한 등급 (체험/만료 반영, 정책이 실제 참조하는 값). 만료 시 FREE 로 강등됨"),
                fieldWithPath("data.status").type(JsonFieldType.STRING).description("구독 상태: ACTIVE(정상), TRIAL(체험), ADMIN_EXEMPT(관리자 등록·한도 면제)"),
                fieldWithPath("data.startedAt").type(JsonFieldType.STRING).description("구독 시작일 (yyyy-MM-dd)"),
                fieldWithPath("data.expiresAt").type(JsonFieldType.STRING).optional().description("구독 만료일 (yyyy-MM-dd, null = 무기한)")
        };
    }

    // 관리자 구독 조회 응답(ApiResponse<AdminCompanySubscriptionResponse>). 사용자용 + 감사 타임스탬프.
    public static FieldDescriptor[] adminCompanySubscriptionResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                fieldWithPath("data.companyId").type(JsonFieldType.NUMBER).description("구독 주체(회사) ID"),
                fieldWithPath("data.companyName").type(JsonFieldType.STRING).description("구독 주체(회사) 이름"),
                fieldWithPath("data.grade").type(JsonFieldType.STRING).description("계약(저장) 등급: FREE, BASIC, STANDARD, PREMIUM, ENTERPRISE"),
                fieldWithPath("data.effectiveGrade").type(JsonFieldType.STRING).description("지금 유효한 등급 (체험/만료 반영, 정책이 실제 참조하는 값). 만료 시 FREE 로 강등됨"),
                fieldWithPath("data.status").type(JsonFieldType.STRING).description("구독 상태: ACTIVE(정상), TRIAL(체험), ADMIN_EXEMPT(관리자 등록·한도 면제)"),
                fieldWithPath("data.startedAt").type(JsonFieldType.STRING).description("구독 시작일 (yyyy-MM-dd)"),
                fieldWithPath("data.expiresAt").type(JsonFieldType.STRING).optional().description("구독 만료일 (yyyy-MM-dd, null = 무기한)"),
                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("구독 생성 시각 (감사용)"),
                fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("구독 최종 수정 시각 (감사용)")
        };
    }

    // 관리자 구독 수정 요청(AdminSubscriptionUpdateRequest). 팝업이 세 필드를 항상 채워 보내는 full-replace.
    public static FieldDescriptor[] adminSubscriptionUpdateRequestFields() {
        return new FieldDescriptor[]{
                fieldWithPath("grade").type(JsonFieldType.STRING).description("변경할 등급 (필수): FREE, BASIC, STANDARD, PREMIUM, ENTERPRISE"),
                fieldWithPath("status").type(JsonFieldType.STRING).description("변경할 상태 (필수): ACTIVE, TRIAL, ADMIN_EXEMPT"),
                fieldWithPath("expiresAt").type(JsonFieldType.STRING).optional().description("만료일 (yyyy-MM-dd). null = 무기한(만료 없음). startedAt 은 수정하지 않는다")
        };
    }

    // 공개 상세 조회 응답(ApiResponse<CompanyPublicResponse>). data 가 곧 공개 프로필이다.
    public static FieldDescriptor[] companyPublicResponseFields() {
        return concat(
                envelopeFields("공개 회사 프로필"),
                profileFields("data."));
    }

    // 소유자/관리자 상세 조회 응답(ApiResponse<CompanyDetailResponse>). data = profile(공개) + meta(내부·운영).
    public static FieldDescriptor[] companyDetailResponseFields() {
        return concat(
                envelopeFields("회사 상세 (profile + meta)"),
                new FieldDescriptor[]{
                        fieldWithPath("data.profile").type(JsonFieldType.OBJECT).description("공개 프로필 (공개 조회 응답과 동일)")
                },
                profileFields("data.profile."),
                metaFields("data.meta."));
    }

    // ApiResponse 공통 봉투 필드 (data 객체 자체 포함, 내부 필드는 호출부에서 이어 붙인다).
    private static FieldDescriptor[] envelopeFields(String dataDescription) {
        return new FieldDescriptor[]{
                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                fieldWithPath("message").type(JsonFieldType.STRING).optional().description("응답 메시지 (조회는 생략될 수 있음)"),
                fieldWithPath("error").type(JsonFieldType.STRING).optional().description("에러 메시지 (성공 시 생략됨)"),
                fieldWithPath("data").type(JsonFieldType.OBJECT).description(dataDescription)
        };
    }

    // CompanyPublicResponse 본문 필드. prefix 로 data. / data.profile. 둘 다에 재사용한다.
    private static FieldDescriptor[] profileFields(String p) {
        return new FieldDescriptor[]{
                fieldWithPath(p + "id").type(JsonFieldType.NUMBER).description("회사 ID"),
                fieldWithPath(p + "companyName").type(JsonFieldType.STRING).description("기업 이름"),
                fieldWithPath(p + "ceoName").type(JsonFieldType.STRING).description("대표자"),
                fieldWithPath(p + "establishmentDate").type(JsonFieldType.STRING).optional().description("설립일 (yyyy-MM-dd)"),
                fieldWithPath(p + "roadAddress").type(JsonFieldType.STRING).optional().description("도로명 주소"),
                fieldWithPath(p + "jibunAddress").type(JsonFieldType.STRING).optional().description("지번 주소"),
                fieldWithPath(p + "detailAddress").type(JsonFieldType.STRING).optional().description("상세주소"),
                fieldWithPath(p + "website").type(JsonFieldType.STRING).optional().description("기업 홈페이지"),
                fieldWithPath(p + "logoUrl").type(JsonFieldType.STRING).optional().description("로고 이미지 URL"),
                fieldWithPath(p + "introTitle").type(JsonFieldType.STRING).optional().description("한 줄 요약"),
                fieldWithPath(p + "content").type(JsonFieldType.STRING).optional().description("소개글"),
                fieldWithPath(p + "trlLevel").type(JsonFieldType.STRING).optional().description("기술성숙도"),
                fieldWithPath(p + "videoUrl").type(JsonFieldType.STRING).optional().description("동영상 링크"),
                fieldWithPath(p + "leadTime").type(JsonFieldType.STRING).optional().description("예상 리드타임"),
                fieldWithPath(p + "asInfo").type(JsonFieldType.STRING).optional().description("유지보수/AS 정보"),
                fieldWithPath(p + "pricingType").type(JsonFieldType.STRING).optional().description("견적 산출 방식"),
                fieldWithPath(p + "brandColor").type(JsonFieldType.STRING).optional().description("브랜드 컬러"),
                fieldWithPath(p + "verified").type(JsonFieldType.BOOLEAN).description("에디터 선정 배지 노출 여부 (관리자 큐레이션)"),
                fieldWithPath(p + "businessVerified").type(JsonFieldType.BOOLEAN).description("국세청 사업자 확인 여부 (자가등록 시 선행 인증 통과)"),
                fieldWithPath(p + "featured").type(JsonFieldType.BOOLEAN).description("추천 노출 여부"),
                fieldWithPath(p + "spotlight").type(JsonFieldType.BOOLEAN).description("스포트라이트 노출 여부"),
                fieldWithPath(p + "likedByMe").type(JsonFieldType.BOOLEAN).description("로그인 뷰어가 이 회사를 좋아요 했는지 (익명·소유자/관리자 뷰는 false)"),
                fieldWithPath(p + "favoritedByMe").type(JsonFieldType.BOOLEAN).description("로그인 뷰어가 이 회사를 즐겨찾기 했는지 (익명·소유자/관리자 뷰는 false)"),

                // 대표 연락처 1건 (없으면 null)
                fieldWithPath(p + "representativeContact").type(JsonFieldType.OBJECT).optional().description("대표 연락처 (없으면 null)"),
                fieldWithPath(p + "representativeContact.contactName").type(JsonFieldType.STRING).optional().description("담당자명"),
                fieldWithPath(p + "representativeContact.position").type(JsonFieldType.STRING).optional().description("직책"),
                fieldWithPath(p + "representativeContact.phone").type(JsonFieldType.STRING).optional().description("전화번호"),
                fieldWithPath(p + "representativeContact.email").type(JsonFieldType.STRING).optional().description("이메일"),

                // 갤러리 이미지 목록
                fieldWithPath(p + "galleryImages").type(JsonFieldType.ARRAY).description("회사 갤러리 이미지 목록"),
                fieldWithPath(p + "galleryImages[].imageUrl").type(JsonFieldType.STRING).description("이미지 URL"),
                fieldWithPath(p + "galleryImages[].imageType").type(JsonFieldType.STRING).description("이미지 타입"),
                fieldWithPath(p + "galleryImages[].displayOrder").type(JsonFieldType.NUMBER).description("표시 순서"),

                // 대표 레퍼런스 1건 + 표지 썸네일 (없으면 null)
                fieldWithPath(p + "representativeReference").type(JsonFieldType.OBJECT).optional().description("대표 프로젝트 레퍼런스 (없으면 null)"),
                fieldWithPath(p + "representativeReference.projectTitle").type(JsonFieldType.STRING).optional().description("프로젝트명"),
                fieldWithPath(p + "representativeReference.achievements").type(JsonFieldType.STRING).optional().description("성과"),
                fieldWithPath(p + "representativeReference.partners").type(JsonFieldType.STRING).optional().description("협력사"),
                fieldWithPath(p + "representativeReference.period").type(JsonFieldType.STRING).optional().description("기간"),
                fieldWithPath(p + "representativeReference.thumbnailUrl").type(JsonFieldType.STRING).optional().description("표지 썸네일 URL (없으면 null)"),

                fieldWithPath(p + "materialNames").type(JsonFieldType.ARRAY).description("취급 소재명 목록"),
                fieldWithPath(p + "equipmentNames").type(JsonFieldType.ARRAY).description("보유 설비명 목록"),
                fieldWithPath(p + "tagNames").type(JsonFieldType.ARRAY).description("태그 목록"),

                fieldWithPath(p + "categories").type(JsonFieldType.ARRAY).description("카테고리 목록"),
                fieldWithPath(p + "categories[].id").type(JsonFieldType.NUMBER).description("카테고리 ID"),
                fieldWithPath(p + "categories[].categoryName").type(JsonFieldType.STRING).description("카테고리명"),
                fieldWithPath(p + "categories[].slug").type(JsonFieldType.STRING).description("카테고리 슬러그(URL slug)"),
                fieldWithPath(p + "categories[].depth").type(JsonFieldType.NUMBER).description("계층 깊이"),
                fieldWithPath(p + "categories[].iconUrl").type(JsonFieldType.STRING).optional().description("아이콘 URL"),

                fieldWithPath(p + "certifications").type(JsonFieldType.ARRAY).description("인증 목록"),
                fieldWithPath(p + "certifications[].id").type(JsonFieldType.NUMBER).description("인증 ID"),
                fieldWithPath(p + "certifications[].certificationName").type(JsonFieldType.STRING).description("인증명"),
                fieldWithPath(p + "certifications[].type").type(JsonFieldType.STRING)
                        .description("인증 구분: MANAGEMENT_SYSTEM(경영시스템), INDUSTRY_SPECIFIC(산업특화), MARKET_ACCESS(시장진입)"),

                fieldWithPath(p + "countries").type(JsonFieldType.ARRAY).description("수출 국가 목록"),
                fieldWithPath(p + "countries[].id").type(JsonFieldType.NUMBER).description("국가 ID"),
                fieldWithPath(p + "countries[].code").type(JsonFieldType.STRING).description("국가 코드"),
                fieldWithPath(p + "countries[].nameKo").type(JsonFieldType.STRING).description("국가명(한글)"),
                fieldWithPath(p + "countries[].nameEn").type(JsonFieldType.STRING).description("국가명(영문)"),
                fieldWithPath(p + "countries[].continent").type(JsonFieldType.STRING).description("대륙"),

                fieldWithPath(p + "regions").type(JsonFieldType.ARRAY).description("국내 지역 목록"),
                fieldWithPath(p + "regions[].id").type(JsonFieldType.NUMBER).description("지역 ID"),
                fieldWithPath(p + "regions[].code").type(JsonFieldType.STRING).description("지역 코드"),
                fieldWithPath(p + "regions[].name").type(JsonFieldType.STRING).description("지역명(행정안전부 원본 명칭)"),
                fieldWithPath(p + "regions[].displayName").type(JsonFieldType.STRING).description("화면 표기용 지역명 (예: 서울 / 경기 전역 / 경기 오산시)"),
                fieldWithPath(p + "regions[].level").type(JsonFieldType.STRING).description("지역 레벨 (NATION/SIDO/SIGUNGU)"),

                fieldWithPath(p + "industries").type(JsonFieldType.ARRAY).description("산업군 목록"),
                fieldWithPath(p + "industries[].id").type(JsonFieldType.NUMBER).description("산업군 ID"),
                fieldWithPath(p + "industries[].industryName").type(JsonFieldType.STRING).description("산업군명"),
                fieldWithPath(p + "industries[].slug").type(JsonFieldType.STRING).description("산업군 슬러그"),
                fieldWithPath(p + "industries[].iconUrl").type(JsonFieldType.STRING).optional().description("아이콘 URL")
        };
    }

    // CompanyDetailResponse.ManagementMeta 필드 (소유자/관리자에게만 보이는 내부·운영 메타).
    private static FieldDescriptor[] metaFields(String p) {
        return new FieldDescriptor[]{
                fieldWithPath(p.substring(0, p.length() - 1)).type(JsonFieldType.OBJECT).description("내부·운영 메타데이터 (소유자/관리자 전용)"),
                fieldWithPath(p + "businessNumber").type(JsonFieldType.STRING).optional().description("사업자번호"),
                fieldWithPath(p + "registrationSource").type(JsonFieldType.STRING).description("등록 출처 (USER / ADMIN)"),
                fieldWithPath(p + "registeredBy").type(JsonFieldType.NUMBER).optional().description("등록한 관리자 ID (유저 자가등록이면 null)"),
                fieldWithPath(p + "ownerUserId").type(JsonFieldType.NUMBER).optional().description("소유 유저 ID (미연동이면 null)"),
                fieldWithPath(p + "claimed").type(JsonFieldType.BOOLEAN).description("소유자 연동 여부"),
                fieldWithPath(p + "spotlightOrder").type(JsonFieldType.NUMBER).description("스포트라이트 노출 순서값"),
                fieldWithPath(p + "verified").type(JsonFieldType.BOOLEAN).description("에디터 선정 여부 (관리자 큐레이션)"),
                fieldWithPath(p + "businessVerified").type(JsonFieldType.BOOLEAN).description("국세청 사업자 확인 여부"),
                fieldWithPath(p + "businessVerifiedAt").type(JsonFieldType.STRING).optional().description("사업자 인증 시각 (미인증이면 null)"),
                fieldWithPath(p + "featured").type(JsonFieldType.BOOLEAN).description("추천 여부"),
                fieldWithPath(p + "spotlight").type(JsonFieldType.BOOLEAN).description("스포트라이트 여부"),
                fieldWithPath(p + "deleted").type(JsonFieldType.BOOLEAN).description("소프트 삭제 여부"),
                fieldWithPath(p + "visibility").type(JsonFieldType.STRING).description("공개 범위 (PUBLIC / PRIVATE)"),
                fieldWithPath(p + "createdAt").type(JsonFieldType.STRING).description("생성 시각"),
                fieldWithPath(p + "updatedAt").type(JsonFieldType.STRING).description("수정 시각")
        };
    }

    private static FieldDescriptor[] concat(FieldDescriptor[]... groups) {
        int total = 0;
        for (FieldDescriptor[] g : groups) {
            total += g.length;
        }
        FieldDescriptor[] merged = new FieldDescriptor[total];
        int i = 0;
        for (FieldDescriptor[] g : groups) {
            System.arraycopy(g, 0, merged, i, g.length);
            i += g.length;
        }
        return merged;
    }
}

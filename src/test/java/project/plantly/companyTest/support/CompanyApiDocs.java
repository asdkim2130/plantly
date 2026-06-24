package project.plantly.companyTest.support;

import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

// 회사 등록 API 의 요청/응답 필드 문서 디스크립터.
// 유저 등록(CompanyController)과 관리자 등록(AdminCompanyController)이 동일한 요청/응답 형태라 두 슬라이스 테스트가 공유한다.
public class CompanyApiDocs {

    // CompanyCreateRequest 전체 필드. 발행(publish) 전 임시저장 호환을 위해 companyName/ceoName 외에는 모두 optional.
    public static FieldDescriptor[] companyCreateRequestFields() {
        return new FieldDescriptor[]{
                // ===== 본체 =====
                fieldWithPath("businessNumber").type(JsonFieldType.STRING).optional().description("사업자번호"),
                fieldWithPath("companyName").type(JsonFieldType.STRING).description("기업 이름 (필수)"),
                fieldWithPath("ceoName").type(JsonFieldType.STRING).description("대표자 (필수)"),
                fieldWithPath("establishmentDate").type(JsonFieldType.STRING).optional().description("설립일 (yyyy-MM-dd)"),
                fieldWithPath("postalCode").type(JsonFieldType.STRING).optional().description("우편번호"),
                fieldWithPath("address").type(JsonFieldType.STRING).optional().description("주소"),
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
}

package project.plantly.globalTest;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import project.plantly.global.response.ApiResponse;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 검증 실패 응답은 위반을 전부 담아 내려간다(ApiResponse.errors). 프론트는 각 입력칸 아래에 메시지를 붙이고
// errors[0] 으로 스크롤·포커스를 옮긴다. 그래서 순서가 계약의 일부다 - 이 테스트가 잠그는 것이 그 순서다.
//
// 잠그는 이유: Bean Validation 은 제약 평가 순서를 보장하지 않는다. 정렬하지 않으면 같은 요청에 같은 순서가
// 나온다는 보장조차 없고, errors[0] 이 화면 최상단 항목이 아니게 되어 스크롤이 엉뚱한 데로 뛴다.
//
// 도메인 DTO(CompanyCreateRequest 등) 대신 probe DTO 를 쓰는 것은 GlobalExceptionHandlerTest 와 같은 이유다.
// 도메인 DTO 의 필드 순서가 화면 사정으로 바뀔 때 정렬 규칙과 무관한 이유로 이 테스트가 깨지면 안 된다.
@ActiveProfiles("test")
@WebMvcTest(controllers = ValidationMessageOrderingTest.OrderingProbeController.class)
@Import(ValidationMessageOrderingTest.OrderingProbeController.class)
@DisplayName("검증 실패 응답 - 위반 전부를 폼 순서로 내려보낸다")
class ValidationMessageOrderingTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Nested
    @DisplayName("요청 본문 객체 - MethodArgumentNotValidException")
    class BodyObject {

        // 선언 순서가 곧 화면의 폼 순서다. 사용자는 위에서 아래로 채우므로 지적도 그 순서를 따라야 한다.
        @Test
        @DisplayName("여러 필드가 함께 틀리면 먼저 선언된 필드를 지적한다")
        void earlierFieldWins() throws Exception {
            mockMvc.perform(post("/ordering/body")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "", "password": "", "rePassword": "", "contacts": []}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].message").value("이메일은 필수입니다."));
        }

        // password 하나에 @NotBlank·@Size·@Pattern 이 함께 걸린다. 비어 있는데 "특수문자가 없다" 부터
        // 지적하면 사용자는 빈 칸에 특수문자를 넣으려 한다.
        @Test
        @DisplayName("한 필드에 여러 제약이 걸리면 비어 있음을 먼저 지적한다")
        void emptinessBeforeFormat() throws Exception {
            mockMvc.perform(post("/ordering/body")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "user@plantly.test", "password": "", "rePassword": "", "contacts": []}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].message").value("비밀번호는 필수입니다."));
        }

        // 비어 있지는 않고 길이와 형식이 함께 틀린 경우. 길이가 먼저다 - 10자를 채우는 과정에서
        // 특수문자가 들어갈 수도 있어, 형식부터 지적하면 두 번 지적할 일이 늘어난다.
        @Test
        @DisplayName("길이와 형식이 함께 틀리면 길이를 먼저 지적한다")
        void lengthBeforePattern() throws Exception {
            mockMvc.perform(post("/ordering/body")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "user@plantly.test", "password": "abc", "rePassword": "abc", "contacts": []}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].message").value("비밀번호는 최소 10자입니다."));
        }

        // 부속 컬렉션은 본체 아래에 놓인다. 본체가 아직 틀렸는데 컬렉션 안쪽부터 지적하면
        // 사용자는 화면을 아래위로 오간다.
        @Test
        @DisplayName("본체와 컬렉션이 함께 틀리면 본체를 먼저 지적한다")
        void bodyBeforeNestedCollection() throws Exception {
            mockMvc.perform(post("/ordering/body")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "", "password": "verylongpassword!", "rePassword": "verylongpassword!",
                                     "contacts": [{"name": "", "phone": "010-1234-5678"}]}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].message").value("이메일은 필수입니다."));
        }

        // 컬렉션 안쪽도 원소의 선언 순서를 따른다.
        @Test
        @DisplayName("컬렉션 원소 안에서도 먼저 선언된 필드를 지적한다")
        void earlierFieldWinsInsideElement() throws Exception {
            mockMvc.perform(post("/ordering/body")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "user@plantly.test", "password": "verylongpassword!",
                                     "rePassword": "verylongpassword!",
                                     "contacts": [{"name": "", "phone": "010-1234-5678"}]}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].message").value("담당자 이름은 필수입니다."));
        }

        @Test
        @DisplayName("컬렉션 원소가 여럿이면 앞선 인덱스를 먼저 지적한다")
        void earlierIndexWins() throws Exception {
            mockMvc.perform(post("/ordering/body")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "user@plantly.test", "password": "verylongpassword!",
                                     "rePassword": "verylongpassword!",
                                     "contacts": [{"name": "김담당", "phone": "010-1234-5678"},
                                                  {"name": "", "phone": "01012345678"}]}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].message").value("연락처는 숫자만 입력 가능합니다."));
        }

        // 클래스 레벨 제약(여러 필드를 함께 보는 검증)은 붙일 필드가 없어 맨 뒤로 간다.
        // 개별 필드가 각자 유효해지기 전에 필드 간 정합성을 따질 이유가 없다.
        @Test
        @DisplayName("클래스 레벨 위반은 필드 위반보다 뒤로 밀린다")
        void classLevelComesLast() throws Exception {
            mockMvc.perform(post("/ordering/body")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "", "password": "verylongpassword!", "rePassword": "다른값입니다!!",
                                     "contacts": []}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].message").value("이메일은 필수입니다."));
        }

        // 다만 밀려날 뿐 사라지지는 않는다. 필드가 모두 유효해지면 그때 이 메시지가 나간다 -
        // 사용자 흐름으로는 "칸을 다 채우고 나니 비밀번호가 서로 다르다고 한다" 가 된다.
        @Test
        @DisplayName("필드가 모두 유효하면 클래스 레벨 위반이 나간다")
        void classLevelSurfacesWhenFieldsAreValid() throws Exception {
            mockMvc.perform(post("/ordering/body")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "user@plantly.test", "password": "verylongpassword!",
                                     "rePassword": "다른값입니다!!", "contacts": []}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].message").value("비밀번호가 일치하지 않습니다."));
        }
    }

    @Nested
    @DisplayName("리스트 본문 - HandlerMethodValidationException")
    class ListBody {

        @Test
        @DisplayName("앞선 인덱스의 위반을 먼저 지적한다")
        void earlierIndexWins() throws Exception {
            mockMvc.perform(post("/ordering/contacts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    [{"name": "", "phone": "01012345678"},
                                     {"name": "박담당", "phone": "010-1234-5678"}]
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].message").value("담당자 이름은 필수입니다."));
        }

        @Test
        @DisplayName("원소 안에서도 먼저 선언된 필드를 지적한다")
        void earlierFieldWinsInsideElement() throws Exception {
            mockMvc.perform(post("/ordering/contacts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    [{"name": "", "phone": "010-1234-5678"}]
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].message").value("담당자 이름은 필수입니다."));
        }

        // 원소가 통째로 null 인 경우(컨테이너 원소 제약)는 안쪽 필드 경로가 없다.
        // 그래도 인덱스 순서는 지켜져야 한다.
        @Test
        @DisplayName("null 원소도 인덱스 순서를 따른다")
        void nullElementFollowsIndexOrder() throws Exception {
            mockMvc.perform(post("/ordering/contacts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    [null, {"name": "", "phone": "010-1234-5678"}]
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].message").value("연락처 항목은 비어 있을 수 없습니다."));
        }
    }

    @Nested
    @DisplayName("봉투 - 프론트가 입력칸을 찾는 근거")
    class Envelope {

        // 폼이 30종인 화면에서 하나씩 알려주면 저장을 몇 번이나 눌러야 한다. 전부 담아 한 번에 표시한다.
        @Test
        @DisplayName("위반이 여럿이면 전부 담긴다 - 하나로 줄이지 않는다")
        void allViolationsAreIncluded() throws Exception {
            mockMvc.perform(post("/ordering/body")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "", "password": "abc", "rePassword": "abc",
                                     "contacts": [{"name": "", "phone": "010-1234-5678"}]}
                                    """))
                    .andExpect(status().isBadRequest())
                    // email 비어있음 / password 길이 / password 형식 / contacts[0].name 비어있음 / contacts[0].phone 형식
                    .andExpect(jsonPath("$.errors.length()").value(5));
        }

        // 프론트는 이 경로로 입력칸을 찾는다. 중첩 컬렉션은 인덱스까지 포함해야 몇 번째 항목인지 정해진다.
        @Test
        @DisplayName("항목마다 필드 경로가 붙는다 - 중첩 컬렉션은 인덱스까지")
        void eachErrorCarriesItsFieldPath() throws Exception {
            mockMvc.perform(post("/ordering/body")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "", "password": "verylongpassword!",
                                     "rePassword": "verylongpassword!",
                                     "contacts": [{"name": "김담당", "phone": "010-1234-5678"}]}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("email"))
                    .andExpect(jsonPath("$.errors[1].field").value("contacts[0].phone"));
        }

        // 클래스 레벨 위반은 붙일 칸이 없다. field 키 자체가 빠지므로 프론트는 키 존재 여부로
        // "칸 아래에 표시" 와 "폼 전체에 표시" 를 가른다.
        @Test
        @DisplayName("클래스 레벨 위반에는 field 키가 아예 없다")
        void formLevelErrorHasNoField() throws Exception {
            mockMvc.perform(post("/ordering/body")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "user@plantly.test", "password": "verylongpassword!",
                                     "rePassword": "다른값입니다!!", "contacts": []}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].message").value("비밀번호가 일치하지 않습니다."))
                    .andExpect(jsonPath("$.errors[0].field").doesNotExist());
        }

        // 항목별 표시를 하지 않는 화면(관리자 도구 등)이 error 하나만 읽어도 동작해야 한다.
        @Test
        @DisplayName("error 에는 첫 항목의 메시지가 그대로 들어간다")
        void errorMirrorsFirstEntry() throws Exception {
            mockMvc.perform(post("/ordering/body")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "", "password": "", "rePassword": "", "contacts": []}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error").value("이메일은 필수입니다."))
                    .andExpect(jsonPath("$.errors[0].message").value("이메일은 필수입니다."));
        }

        // 리스트 본문은 바깥에 필드명이 없어 인덱스가 경로의 시작이 된다.
        @Test
        @DisplayName("리스트 본문의 경로는 인덱스로 시작한다")
        void listBodyPathStartsWithIndex() throws Exception {
            mockMvc.perform(post("/ordering/contacts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    [{"name": "김담당", "phone": "010-1234-5678"}]
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("[0].phone"));
        }

        // 검증 실패가 아닌 응답에는 errors 가 실리지 않는다. 봉투에 늘 있는 키가 되면
        // 프론트가 "있으면 항목별 표시" 로 분기할 수 없다.
        @Test
        @DisplayName("검증 실패가 아닌 오류에는 errors 가 없다")
        void nonValidationFailureHasNoErrors() throws Exception {
            mockMvc.perform(post("/ordering/body")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\": "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors").doesNotExist());
        }
    }

    // 순서만 유발하기 위한 최소 엔드포인트. 도메인 계약이 아니라 정렬 규칙이 검증 대상이다.
    @RestController
    @RequestMapping("/ordering")
    static class OrderingProbeController {

        @PostMapping(value = "/body", consumes = MediaType.APPLICATION_JSON_VALUE)
        ApiResponse<Void> body(@Valid @RequestBody ProbeForm form) {
            return ApiResponse.ok();
        }

        @PostMapping(value = "/contacts", consumes = MediaType.APPLICATION_JSON_VALUE)
        ApiResponse<Void> contacts(
                @Valid @RequestBody List<@NotNull(message = "연락처 항목은 비어 있을 수 없습니다.") ProbeContact> contacts) {
            return ApiResponse.ok();
        }
    }

    // 필드 선언 순서 = 화면의 폼 순서. 정렬이 이 순서를 따라가는지가 이 테스트의 핵심이다.
    @PasswordMatch
    record ProbeForm(
            @NotBlank(message = "이메일은 필수입니다.")
            @Email(message = "올바른 이메일 형식이 아닙니다.")
            String email,

            @NotBlank(message = "비밀번호는 필수입니다.")
            @Size(min = 10, message = "비밀번호는 최소 10자입니다.")
            @Pattern(regexp = "^(?=.*[^A-Za-z0-9]).+$", message = "비밀번호에 특수문자가 필요합니다.")
            String password,

            String rePassword,

            @Valid
            List<ProbeContact> contacts
    ) {
    }

    record ProbeContact(
            @NotBlank(message = "담당자 이름은 필수입니다.")
            String name,

            @Pattern(regexp = "^[0-9]+$", message = "연락처는 숫자만 입력 가능합니다.")
            String phone
    ) {
    }

    // 필드 하나에 붙지 않는 제약. FieldError 가 아니라 ObjectError 로 담기므로 정렬에서 맨 뒤로 간다.
    @Documented
    @Constraint(validatedBy = PasswordMatchValidator.class)
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface PasswordMatch {

        String message() default "비밀번호가 일치하지 않습니다.";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    static class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, ProbeForm> {

        @Override
        public boolean isValid(ProbeForm form, ConstraintValidatorContext context) {
            return form.password() != null && form.password().equals(form.rePassword());
        }
    }
}

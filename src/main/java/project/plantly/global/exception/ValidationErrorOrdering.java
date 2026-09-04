package project.plantly.global.exception;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import project.plantly.global.response.ValidationError;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// 검증 위반 여러 건을 화면의 폼 순서로 정렬하는 규칙.
//
// 응답은 위반을 전부 담아 내려간다(ApiResponse.errors). 프론트는 각 입력칸 아래에 메시지를 붙이고
// 첫 항목으로 스크롤·포커스를 옮긴다. 그 "첫 항목" 이 화면 최상단의 위반이어야 하므로 순서가 계약의 일부다 -
// 정렬하지 않으면 프론트가 필드명을 자기 DOM 순서에 다시 매핑해야 하고, 그건 폼 레이아웃이 바뀔 때마다
// 프론트에서 깨지는 종류의 코드다.
//
// Bean Validation 은 제약 평가 순서를 보장하지 않는다. 정렬 없이 받은 순서를 그대로 쓰면 같은 요청에
// 같은 순서가 나온다는 보장조차 없고, 한 필드 안에서도(SignUpRequest.password 는 @NotBlank·@Size·@Pattern
// 세 개가 붙어 있다) 어느 제약이 먼저 나올지 알 수 없다.
//
// 정렬 기준은 두 단계다:
//   1) 위치 - 필드가 DTO 에 선언된 순서. DTO 가 전부 record 라 getRecordComponents() 가 선언 순서를
//      확정적으로 준다(일반 클래스의 getDeclaredFields() 는 JLS 가 순서를 보장하지 않아 이렇게 못 한다).
//      선언 순서는 곧 화면의 폼 순서이므로 "최상단부터" 가 그대로 나온다.
//   2) 제약 종류 - 같은 필드 안에서는 비어 있음 → 길이 → 형식 순. "필수인데 비었다" 를 놔두고
//      "특수문자가 없다" 부터 지적하지 않기 위해서다.
//
// 필드에 붙지 않는 클래스 레벨 위반(ObjectError)은 맨 뒤로 보낸다. 개별 필드가 각자 유효해지기 전에
// 필드 간 정합성(비밀번호 재입력 일치 등)을 따질 이유가 없다.
final class ValidationErrorOrdering {

    // 같은 필드에 걸린 제약들 사이의 순서. 앞에 있을수록 먼저 지적한다.
    // 여기 없는 제약(커스텀 제약, 타입 변환 실패 등)은 전부 뒤로 간다.
    private static final List<String> CONSTRAINT_ORDER = List.of(
            "NotNull", "NotBlank", "NotEmpty",           // 비어 있음
            "Size", "Length", "Min", "Max",              // 길이·범위
            "Email", "Pattern");                         // 형식

    // 경로 세그먼트를 풀지 못했을 때 쓰는 서수. 못 푼 것끼리는 뒤에 오되, 서로는 메시지로 순서를 가른다.
    private static final int UNRESOLVED = Integer.MAX_VALUE - 1;

    // 필드에 붙지 않는 클래스 레벨 위반의 서수. 항상 맨 뒤.
    private static final int CLASS_LEVEL = Integer.MAX_VALUE;

    private ValidationErrorOrdering() {
    }

    // 요청 본문 객체 검증(MethodArgumentNotValidException).
    //
    // getFieldErrors 가 아니라 getAllErrors 를 훑는다 - 클래스 레벨 제약은 FieldError 가 아니라
    // ObjectError 로 담기는데, 필드 에러만 보면 그 위반이 응답에서 통째로 빠진다.
    static List<ValidationError> sorted(BindingResult bindingResult) {
        Object target = bindingResult.getTarget();
        Class<?> rootType = (target != null) ? target.getClass() : null;

        return bindingResult.getAllErrors().stream()
                .filter(error -> error.getDefaultMessage() != null)
                .sorted((left, right) -> sortKey(rootType, left).compareTo(sortKey(rootType, right)))
                .map(ValidationErrorOrdering::toValidationError)
                .toList();
    }

    // 메서드 파라미터 검증(HandlerMethodValidationException).
    // 본문이 객체가 아니라 리스트라 파라미터 레벨 검증이 도는 경우다.
    static List<ValidationError> sorted(HandlerMethodValidationException ex) {
        record Entry(SortKey key, ValidationError error) {
        }

        List<Entry> entries = new ArrayList<>();

        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                if (error.getDefaultMessage() == null) {
                    continue;
                }

                entries.add(new Entry(sortKey(result, error), toValidationError(result, error)));
            }
        }

        return entries.stream()
                .sorted((left, right) -> left.key().compareTo(right.key()))
                .map(Entry::error)
                .toList();
    }

    // ===== 응답 항목으로 옮기기 =====

    private static ValidationError toValidationError(ObjectError error) {
        if (error instanceof FieldError fieldError) {
            return ValidationError.of(fieldError.getField(), error.getDefaultMessage());
        }

        return ValidationError.form(error.getDefaultMessage());
    }

    // 리스트 본문의 경로는 컨테이너 인덱스가 앞에 붙는다 - "[0].phone" 처럼 만들어야 프론트가
    // 몇 번째 항목의 어느 칸인지 찾을 수 있다. 원소가 통째로 null 이면 안쪽 경로가 없어 "[0]" 까지만 나온다.
    private static ValidationError toValidationError(ParameterValidationResult result,
                                                     MessageSourceResolvable error) {
        Integer containerIndex = result.getContainerIndex();
        String prefix = (containerIndex != null) ? "[" + containerIndex + "]" : "";

        if (error instanceof FieldError fieldError) {
            String path = prefix.isEmpty() ? fieldError.getField() : prefix + "." + fieldError.getField();
            return ValidationError.of(path, error.getDefaultMessage());
        }

        if (prefix.isEmpty()) {
            return ValidationError.form(error.getDefaultMessage());
        }

        return ValidationError.of(prefix, error.getDefaultMessage());
    }

    // ===== 정렬 키 =====

    // 위치를 서수 목록으로 편다. "contacts[0].phone" 은 [contacts 의 선언 순서, 0, phone 의 선언 순서] 가 된다.
    // 목록을 앞에서부터 비교하므로 바깥 필드 순서가 먼저, 같은 컬렉션 안에서는 인덱스가, 그다음 안쪽 필드 순서가 걸린다.
    private static SortKey sortKey(Class<?> rootType, ObjectError error) {
        if (!(error instanceof FieldError fieldError)) {
            return new SortKey(List.of(CLASS_LEVEL), constraintPriority(error), error.getDefaultMessage());
        }

        return new SortKey(resolvePath(rootType, fieldError.getField()),
                constraintPriority(error), error.getDefaultMessage());
    }

    // 파라미터 검증 쪽. 바깥 위치는 "몇 번째 파라미터인가 → 컬렉션의 몇 번째 원소인가" 로 정해지고,
    // 원소 안쪽 필드 위반은 위와 같은 방식으로 이어 붙인다.
    //
    // 원소 타입은 result.getArgument() 에서 직접 얻는다 - 검증 대상 객체 자체라 제네릭을 풀 필요가 없다.
    // 원소가 통째로 null 인 경우(List<@NotNull X> 위반)에는 argument 가 없지만, 그때는 안쪽 경로도 없다.
    private static SortKey sortKey(ParameterValidationResult result, MessageSourceResolvable error) {
        List<Integer> path = new ArrayList<>();
        path.add(result.getMethodParameter().getParameterIndex());

        Integer containerIndex = result.getContainerIndex();

        if (containerIndex != null) {
            path.add(containerIndex);
        }

        if (error instanceof FieldError fieldError) {
            Object argument = result.getArgument();
            path.addAll(resolvePath((argument != null) ? argument.getClass() : null, fieldError.getField()));
        }

        return new SortKey(path, constraintPriority(error), error.getDefaultMessage());
    }

    // 필드 경로("password", "contacts[0].phone")를 서수 목록으로 바꾼다.
    // 타입을 따라가지 못하면(record 가 아니거나 이름이 안 맞으면) 남은 세그먼트를 전부 UNRESOLVED 로 채운다 -
    // 순서를 못 정할 뿐 메시지를 잃지는 않는다.
    private static List<Integer> resolvePath(Class<?> rootType, String field) {
        List<Integer> path = new ArrayList<>();
        Class<?> current = rootType;

        for (String segment : field.split("\\.")) {
            int bracket = segment.indexOf('[');
            String name = (bracket >= 0) ? segment.substring(0, bracket) : segment;

            RecordComponent component = componentOf(current, name);
            path.add((component != null) ? ordinalOf(current, name) : UNRESOLVED);

            if (bracket >= 0) {
                path.add(indexOf(segment.substring(bracket)));
            }

            current = (component != null) ? elementTypeOf(component) : null;
        }

        return path;
    }

    private static RecordComponent componentOf(Class<?> type, String name) {
        if (type == null || !type.isRecord()) {
            return null;
        }

        for (RecordComponent component : type.getRecordComponents()) {
            if (component.getName().equals(name)) {
                return component;
            }
        }

        return null;
    }

    private static int ordinalOf(Class<?> type, String name) {
        RecordComponent[] components = type.getRecordComponents();

        for (int i = 0; i < components.length; i++) {
            if (components[i].getName().equals(name)) {
                return i;
            }
        }

        return UNRESOLVED;
    }

    // 컬렉션 컴포넌트(List<ContactRequest>)면 원소 타입을, 아니면 그 타입 자체를 준다.
    // 다음 세그먼트를 풀 때 어느 타입의 컴포넌트를 찾을지가 여기서 정해진다.
    private static Class<?> elementTypeOf(RecordComponent component) {
        if (!Collection.class.isAssignableFrom(component.getType())) {
            return component.getType();
        }

        if (component.getGenericType() instanceof ParameterizedType parameterized) {
            Type[] arguments = parameterized.getActualTypeArguments();

            if (arguments.length == 1 && arguments[0] instanceof Class<?> elementType) {
                return elementType;
            }
        }

        return null;
    }

    // "[0]" 또는 "[0][1]" 의 첫 인덱스. 숫자가 아니면(맵 키 등) 순서를 정하지 않는다.
    private static int indexOf(String bracketed) {
        int close = bracketed.indexOf(']');

        try {
            return Integer.parseInt(bracketed.substring(1, close));
        } catch (RuntimeException e) {
            return UNRESOLVED;
        }
    }

    // 제약 이름은 코드 목록의 마지막 항목이다.
    // 예: ["NotBlank.signUpRequest.password", "NotBlank.password", "NotBlank.java.lang.String", "NotBlank"]
    private static int constraintPriority(MessageSourceResolvable error) {
        String[] codes = error.getCodes();

        if (codes == null || codes.length == 0) {
            return CONSTRAINT_ORDER.size();
        }

        int index = CONSTRAINT_ORDER.indexOf(codes[codes.length - 1]);

        return (index >= 0) ? index : CONSTRAINT_ORDER.size();
    }

    // 위치 → 제약 종류 → 메시지 순으로 비교한다. 마지막 메시지 비교는 앞의 둘이 모두 같을 때
    // 실행 순서에 기대지 않도록 순서를 못 박기 위한 것이다.
    private record SortKey(List<Integer> path, int constraintPriority, String message)
            implements Comparable<SortKey> {

        @Override
        public int compareTo(SortKey other) {
            int shared = Math.min(path.size(), other.path.size());

            for (int i = 0; i < shared; i++) {
                int compared = Integer.compare(path.get(i), other.path.get(i));

                if (compared != 0) {
                    return compared;
                }
            }

            int compared = Integer.compare(path.size(), other.path.size());

            if (compared != 0) {
                return compared;
            }

            compared = Integer.compare(constraintPriority, other.constraintPriority);

            if (compared != 0) {
                return compared;
            }

            return message.compareTo(other.message);
        }
    }
}

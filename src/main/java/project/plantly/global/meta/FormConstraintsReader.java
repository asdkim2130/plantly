package project.plantly.global.meta;

import jakarta.validation.MessageInterpolator;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.metadata.BeanDescriptor;
import jakarta.validation.metadata.ConstraintDescriptor;
import jakarta.validation.metadata.ContainerElementTypeDescriptor;
import jakarta.validation.metadata.PropertyDescriptor;
import org.springframework.stereotype.Component;
import project.plantly.global.meta.dto.ConstraintRule;
import project.plantly.global.meta.dto.FieldConstraints;
import project.plantly.global.meta.dto.FormConstraintsResponse;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 요청 DTO 에 붙은 검증 애너테이션을 읽어 폼 제약 응답으로 옮긴다.
 *
 * <p><b>왜 값을 손으로 적지 않는가.</b> 제약의 단일 출처는 {@code CompanyConstraints} 상수이고, 그 값이
 * 실제로 강제되는 자리는 DTO 의 애너테이션이다. 응답에 숫자를 따로 적어두면 애너테이션을 고칠 때 이쪽이
 * 따라오지 않아, 프론트가 통과시킨 값을 서버가 400 으로 막는 상태가 조용히 생긴다. Jakarta 가 이미 들고
 * 있는 메타데이터({@link Validator#getConstraintsForClass})에서 뽑으면 그 어긋남이 성립하지 않는다.
 *
 * <p><b>왜 springdoc 이 아닌가.</b> 이 프로젝트는 REST Docs 로 문서를 만든다. springdoc 을 들이면
 * 같은 사실을 말하는 문서가 두 벌이 되고, 게다가 OpenAPI 스키마는 사람이 읽는 문서지 화면이 폼을 그리려고
 * 파싱할 계약이 아니다. 필요한 것은 "이 폼의 이 칸에 무엇을 걸어야 하는가" 뿐이라 응답을 따로 만든다.
 *
 * <p><b>필드 순서.</b> {@code getConstrainedProperties()} 가 아니라 {@code getRecordComponents()} 를
 * 훑는다. 전자는 순서가 없는 Set 이라 응답 순서가 요청마다 달라질 수 있고, 후자는 선언 순서를 확정적으로
 * 준다 — 그 순서가 곧 화면의 폼 순서다. 검증 실패 응답의 정렬({@code ValidationErrorOrdering})과 같은
 * 근거를 쓰므로 두 응답의 필드 순서가 서로 어긋나지 않는다.
 *
 * <p>DTO 가 record 가 아니면 하위 필드를 훑지 않는다. 일반 클래스의 {@code getDeclaredFields()} 는 JLS 가
 * 순서를 보장하지 않아, 순서가 계약인 이 응답에서는 쓸 수 없기 때문이다(정렬 쪽도 같은 이유로 record 만 푼다).
 */
@Component
public class FormConstraintsReader {

    // 한 필드 안에서 규칙을 늘어놓는 순서. 검증 실패 응답의 제약 정렬(비어 있음 → 길이·개수 → 형식)과
    // 같은 순서다 - 프론트가 "먼저 지적할 규칙" 을 두 응답에서 다르게 읽지 않게 하려는 것.
    // 여기 없는 규칙(모르는 커스텀 제약)은 전부 뒤로 간다.
    private static final List<String> RULE_ORDER = List.of(
            "required",
            "minItems", "maxItems",
            "minLength", "maxLength",
            "min", "max",
            "email", "pattern");

    // Jakarta 는 제약 집합을 Set 으로 준다. 같은 요청에 같은 응답이 나오도록 규칙 순서를 못 박는다 -
    // 우선순위가 같은 규칙끼리는 이름·메시지로 마저 가른다.
    private static final Comparator<ConstraintRule> RULE_ORDERING =
            Comparator.comparingInt((ConstraintRule rule) -> priorityOf(rule.type()))
                    .thenComparing(ConstraintRule::type)
                    .thenComparing(rule -> Objects.toString(rule.message(), ""));

    private final Validator validator;
    private final MessageInterpolator messageInterpolator;

    // 응답은 클래스 메타데이터에서만 나오므로 기동 후 절대 바뀌지 않는다. 폼당 한 번만 만들어 재사용한다.
    private final Map<ConstraintForm, FormConstraintsResponse> cache = new ConcurrentHashMap<>();

    public FormConstraintsReader(ValidatorFactory validatorFactory) {
        this.validator = validatorFactory.getValidator();
        this.messageInterpolator = validatorFactory.getMessageInterpolator();
    }

    public FormConstraintsResponse read(ConstraintForm form) {
        return cache.computeIfAbsent(form, this::describe);
    }

    private FormConstraintsResponse describe(ConstraintForm form) {
        BeanDescriptor bean = validator.getConstraintsForClass(form.type());

        return new FormConstraintsResponse(
                form.slug(),
                rulesOf(bean.getConstraintDescriptors(), form.type()),
                fieldsOf(form.type(), Set.of()));
    }

    // ===== 필드 =====

    // enclosing 은 지금 내려온 경로에 이미 나온 타입들이다. DTO 가 자기 자신을 품는 구조(트리 모양 입력)를
    // 만들면 재귀가 끝나지 않으므로 여기서 끊는다.
    private List<FieldConstraints> fieldsOf(Class<?> type, Set<Class<?>> enclosing) {
        if (type == null || !type.isRecord() || enclosing.contains(type)) {
            return List.of();
        }

        BeanDescriptor bean = validator.getConstraintsForClass(type);

        Set<Class<?>> nested = new HashSet<>(enclosing);
        nested.add(type);

        List<FieldConstraints> fields = new ArrayList<>();

        for (RecordComponent component : type.getRecordComponents()) {
            // 제약도 @Valid 도 없는 필드는 서술자가 없다 - 검증이 걸리지 않는 자리라 응답에서도 뺀다.
            PropertyDescriptor property = bean.getConstraintsForProperty(component.getName());

            if (property == null) {
                continue;
            }

            fields.add(new FieldConstraints(
                    component.getName(),
                    rulesOf(property.getConstraintDescriptors(), property.getElementClass()),
                    nestedFieldsOf(property, component, nested),
                    itemsOf(property, component, nested)));
        }

        return fields;
    }

    // 컬렉션이 아닌 중첩 객체(@Valid 가 붙은 단일 DTO)의 하위 필드.
    // 컬렉션이면 하위 필드는 필드가 아니라 '원소' 에 딸리므로 여기서는 비운다(itemsOf 가 담는다).
    private List<FieldConstraints> nestedFieldsOf(PropertyDescriptor property, RecordComponent component,
                                                  Set<Class<?>> enclosing) {
        if (isContainer(component.getType()) || !property.isCascaded()) {
            return List.of();
        }

        return fieldsOf(component.getType(), enclosing);
    }

    // 컬렉션 원소의 제약. 두 가지가 한 자리에 모인다:
    //   - 원소 자체에 걸린 제약 - List<@NotBlank @Size(max = 20) String> 의 타입 애너테이션
    //   - 원소가 객체일 때 그 안쪽 필드 - @Valid List<ContactRequest> 의 ContactRequest 필드들
    //
    // 원소 타입을 서술자가 아니라 record 컴포넌트의 제네릭 타입에서 얻는 이유는, 원소에 제약이 하나도
    // 없으면(@Valid 만 붙은 경우) 컨테이너 원소 서술자 자체가 없기 때문이다.
    private FieldConstraints itemsOf(PropertyDescriptor property, RecordComponent component,
                                     Set<Class<?>> enclosing) {
        if (!isContainer(component.getType())) {
            return null;
        }

        Class<?> elementType = elementTypeOf(component);

        List<ConstraintDescriptor<?>> elementConstraints = new ArrayList<>();
        boolean cascaded = property.isCascaded();

        // @Valid 가 컨테이너에 붙었는지 원소에 붙었는지는 선언 형태에 따라 갈린다(레거시 List 취급 포함).
        // 어느 쪽이든 "안쪽까지 검증한다" 는 뜻이라 둘 다 본다.
        for (ContainerElementTypeDescriptor element : property.getConstrainedContainerElementTypes()) {
            elementConstraints.addAll(element.getConstraintDescriptors());
            cascaded |= element.isCascaded();
        }

        List<ConstraintRule> rules = rulesOf(elementConstraints, elementType);
        List<FieldConstraints> fields = cascaded ? fieldsOf(elementType, enclosing) : List.of();

        if (rules.isEmpty() && fields.isEmpty()) {
            return null;
        }

        return new FieldConstraints(null, rules, fields, null);
    }

    // ===== 규칙 =====

    private List<ConstraintRule> rulesOf(Iterable<ConstraintDescriptor<?>> descriptors, Class<?> owner) {
        List<ConstraintRule> rules = new ArrayList<>();

        for (ConstraintDescriptor<?> descriptor : descriptors) {
            rules.addAll(toRules(descriptor, owner));
        }

        rules.sort(RULE_ORDERING);

        return rules;
    }

    // 애너테이션 하나 → 규칙 0~2개. @Size 만 둘로 갈라진다(min 과 max 는 화면에서 다른 규칙이다).
    private List<ConstraintRule> toRules(ConstraintDescriptor<?> descriptor, Class<?> owner) {
        Map<String, Object> attributes = descriptor.getAttributes();
        String message = messageOf(descriptor);
        String name = descriptor.getAnnotation().annotationType().getSimpleName();

        return switch (name) {
            // 셋 다 화면에서는 "필수" 하나로 읽힌다.
            case "NotNull", "NotBlank", "NotEmpty" -> List.of(ConstraintRule.of("required", message));
            case "Size", "Length" -> sizeRules(attributes, owner, message);
            // @Email 은 regexp 속성을 갖지만 기본값이라 실어 보내도 쓸모가 없다. 형식 판정은 프론트가
            // 자기 이메일 검사로 하고, 여기서는 "이메일 형식이어야 한다" 는 사실과 문구만 전한다.
            case "Email" -> List.of(ConstraintRule.of("email", message));
            case "Pattern" -> List.of(ConstraintRule.of("pattern", attributes.get("regexp"), message));
            case "Min", "Max" -> List.of(ConstraintRule.of(uncapitalize(name), attributes.get("value"), message));
            // 모르는 제약도 메시지는 내보낸다. 값을 해석할 수 없을 뿐 "이런 규칙이 있다" 는 사실은 참이다.
            default -> List.of(ConstraintRule.of(uncapitalize(name), message));
        };
    }

    // @Size 의 대상이 문자열이면 길이, 컬렉션이면 개수다. 같은 애너테이션이 화면에서는 전혀 다른 규칙이라
    // (한쪽은 입력칸의 maxlength, 다른 쪽은 '더 추가' 버튼의 비활성 조건) 여기서 갈라 내보낸다.
    //
    // 기본값(min 0 / max Integer.MAX_VALUE)은 규칙이 아니라 "제한 없음" 이므로 싣지 않는다.
    private List<ConstraintRule> sizeRules(Map<String, Object> attributes, Class<?> owner, String message) {
        int min = intAttribute(attributes, "min", 0);
        int max = intAttribute(attributes, "max", Integer.MAX_VALUE);

        boolean container = isContainer(owner);

        List<ConstraintRule> rules = new ArrayList<>();

        if (min > 0) {
            rules.add(ConstraintRule.of(container ? "minItems" : "minLength", min, message));
        }

        if (max < Integer.MAX_VALUE) {
            rules.add(ConstraintRule.of(container ? "maxItems" : "maxLength", max, message));
        }

        return rules;
    }

    // 애너테이션에 적힌 message 는 문구가 아니라 템플릿이다. 메시지를 생략한 제약은 기본 템플릿 키
    // ("{jakarta.validation.constraints.NotNull.message}")가 그대로 들어 있어, 그냥 내보내면 프론트가
    // 중괄호 덩어리를 화면에 띄운다. 검증 실패 응답이 문구를 만들 때 쓰는 것과 같은 보간기를 태워
    // 두 응답의 문구가 갈리지 않게 한다(기본 메시지 안의 {max} 같은 자리도 여기서 채워진다).
    private String messageOf(ConstraintDescriptor<?> descriptor) {
        String template = Objects.toString(descriptor.getAttributes().get("message"), null);

        if (template == null) {
            return null;
        }

        return messageInterpolator.interpolate(template, new TemplateOnlyContext(descriptor));
    }

    // 보간에 필요한 최소 문맥. 검증 중이 아니라 값이 없고(getValidatedValue → null), 보간기가 구현체별
    // 확장 문맥을 요구하면 거절한다 - 기본 메시지 템플릿은 제약 속성만으로 채워지므로 그럴 일이 없다.
    private record TemplateOnlyContext(ConstraintDescriptor<?> descriptor) implements MessageInterpolator.Context {

        @Override
        public ConstraintDescriptor<?> getConstraintDescriptor() {
            return descriptor;
        }

        @Override
        public Object getValidatedValue() {
            return null;
        }

        @Override
        public <T> T unwrap(Class<T> type) {
            if (type.isInstance(this)) {
                return type.cast(this);
            }

            throw new ValidationException("지원하지 않는 보간 문맥입니다: " + type);
        }
    }

    private static int priorityOf(String type) {
        int index = RULE_ORDER.indexOf(type);

        return (index >= 0) ? index : RULE_ORDER.size();
    }

    private static int intAttribute(Map<String, Object> attributes, String name, int fallback) {
        return (attributes.get(name) instanceof Number number) ? number.intValue() : fallback;
    }

    private static String uncapitalize(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private static boolean isContainer(Class<?> type) {
        return type != null && (Collection.class.isAssignableFrom(type) || type.isArray());
    }

    // List<ContactRequest> 의 ContactRequest. 원소 타입을 못 풀면(와일드카드 등) 안쪽을 훑지 않는다.
    private static Class<?> elementTypeOf(RecordComponent component) {
        if (component.getGenericType() instanceof ParameterizedType parameterized) {
            Type[] arguments = parameterized.getActualTypeArguments();

            if (arguments.length == 1 && arguments[0] instanceof Class<?> elementType) {
                return elementType;
            }
        }

        return null;
    }
}

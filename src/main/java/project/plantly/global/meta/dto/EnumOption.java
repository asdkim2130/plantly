package project.plantly.global.meta.dto;

import project.plantly.global.meta.LabeledEnum;

/**
 * 선택지 1건. 드롭다운의 한 줄이다.
 *
 * <p>{@code value} 는 요청·응답에 그대로 오가는 enum 상수 이름이고, {@code label} 은 화면에 찍는 문구다.
 * 둘을 함께 내리는 이유는 프론트가 문구를 스스로 지으면 그게 사실상의 정본이 되어, 서버가 문구를 고쳐도
 * 화면은 따라오지 않기 때문이다. 값과 문구가 한 응답에서 나오면 그 어긋남이 성립하지 않는다.
 */
public record EnumOption(String value, String label) {

    public static EnumOption from(LabeledEnum source) {
        return new EnumOption(source.name(), source.getLabel());
    }
}

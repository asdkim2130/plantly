package project.plantly.domain.company.country;

// 대륙 구분. 시드(country.sql)의 continent 값과 1:1로 매핑된다.
// Americas는 subregion 기준으로 남/북으로 분리한다.
public enum Continent {
    ASIA,
    EUROPE,
    AFRICA,
    NORTH_AMERICA,
    SOUTH_AMERICA,
    OCEANIA,
    ANTARCTICA
}

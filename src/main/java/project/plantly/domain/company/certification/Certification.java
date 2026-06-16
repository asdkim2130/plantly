package project.plantly.domain.company.certification;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import project.plantly.domain.company.category.CompanyChild;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Certification extends CompanyChild {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false, unique = true)
    private String certificationName;

    public Certification(String certificationName) {
        this.certificationName = certificationName;
    }

    public static Certification create (String certificationName, int displayOrder){
        Certification certification = new Certification(certificationName);
        certification.activate();
        certification.changeDisplayOrder(displayOrder);

        return certification;
    }


}

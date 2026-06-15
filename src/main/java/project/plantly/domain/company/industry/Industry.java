package project.plantly.domain.company.industry;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import project.plantly.domain.company.category.CompanyChild;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Industry extends CompanyChild {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false, unique = true)
    private String industryName;

    @NotNull
    @Column(nullable = false, unique = true)
    private String industryCode;

    private String iconUrl;

    private String description;

    public Industry(String industryName, String industryCode, String iconUrl, String description) {
        this.industryName = industryName;
        this.industryCode = industryCode;
        this.iconUrl = iconUrl;
        this.description = description;
    }

    public static Industry create (String industryName, String industryCode, String iconUrl, String description, int displayOrder){
        Industry industry = new Industry(industryName, industryCode, iconUrl, description);
        industry.activate();
        industry.changeDisplayOrder(displayOrder);

        return industry;
    }
}

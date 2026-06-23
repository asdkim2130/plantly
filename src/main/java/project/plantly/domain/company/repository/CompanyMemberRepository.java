package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.link.CompanyMember;

public interface CompanyMemberRepository extends JpaRepository<CompanyMember, Long> {
}
package com.example.student_service.repository;

import com.example.student_service.model.UniversityStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<UniversityStudent, Long> {
    Optional<UniversityStudent> findByEmailAddressIgnoreCase(String email);
    boolean existsByEmailAddress(String email);
}
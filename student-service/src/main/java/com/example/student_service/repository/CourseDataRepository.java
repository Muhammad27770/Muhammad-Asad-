package com.example.student_service.repository;

import com.example.student_service.model.AcademicCourse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseDataRepository extends JpaRepository<AcademicCourse, Long> {
    AcademicCourse findByCourseTitleIgnoreCase(String title);
}
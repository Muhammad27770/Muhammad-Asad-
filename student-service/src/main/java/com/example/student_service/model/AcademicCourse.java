package com.example.student_service.model;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "academic_courses")
public class AcademicCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long courseId;

    private String courseTitle;
    private String courseDescription;

    @Column(name = "payment_reference")
    private String paymentRef;

    public AcademicCourse() {}

    public AcademicCourse(String courseTitle, String courseDescription) {
        this.courseTitle = courseTitle;
        this.courseDescription = courseDescription;
    }

    // Getters and setters
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    
    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
    
    public String getCourseDescription() { return courseDescription; }
    public void setCourseDescription(String courseDescription) { 
        this.courseDescription = courseDescription; 
    }
    
    public String getPaymentRef() { return paymentRef; }
    public void setPaymentRef(String paymentRef) { this.paymentRef = paymentRef; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AcademicCourse other = (AcademicCourse) obj;
        return Objects.equals(courseId, other.courseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseId);
    }
}
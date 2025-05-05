package com.example.student_service.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "university_students")
public class UniversityStudent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studentId;
    private String firstName;
    private String lastName;
    private String emailAddress;
    private boolean hasGraduated;
    private String loginPassword;

    @ManyToMany
    @JoinTable(
        name = "student_course_registrations",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private List<AcademicCourse> registeredCourses = new ArrayList<>();

    public UniversityStudent() {}

    // Getters and setters
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }
    
    public boolean isHasGraduated() { return hasGraduated; }
    public void setHasGraduated(boolean hasGraduated) { this.hasGraduated = hasGraduated; }
    
    public String getLoginPassword() { return loginPassword; }
    public void setLoginPassword(String loginPassword) { this.loginPassword = loginPassword; }

    public List<AcademicCourse> getRegisteredCourses() {
        return registeredCourses;
    }

    public void setRegisteredCourses(List<AcademicCourse> registeredCourses) {
        this.registeredCourses = registeredCourses;
    }
}
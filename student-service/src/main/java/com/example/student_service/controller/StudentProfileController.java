package com.example.student_service.controller;

import com.example.student_service.model.UniversityStudent;
import com.example.student_service.model.AcademicCourse;
import com.example.student_service.service.StudentManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.student_service.dto.AuthRequest;
import java.util.List;

@RestController
@RequestMapping("/student")
public class StudentProfileController {

    private static final Logger log = LoggerFactory.getLogger(StudentProfileController.class);
    private final StudentManagementService studentService;

    @Autowired
    public StudentProfileController(StudentManagementService studentService) {
        this.studentService = studentService;
    }
    
    @GetMapping("/verify")
    public ResponseEntity<?> verifyStudentAccount(@RequestParam Long studentId) {
        try {
            boolean exists = studentService.studentExists(studentId);
            return exists ? ResponseEntity.ok().build() 
                         : ResponseEntity.status(HttpStatus.NOT_FOUND).body("Student not found");
        } catch (Exception e) {
            log.error("Verification error for student {}: {}", studentId, e.getMessage());
            return ResponseEntity.internalServerError().body("Verification failed");
        }
    }

    @PostMapping("/register")
    public UniversityStudent registerStudent(@RequestBody UniversityStudent newStudent) {
        System.out.println("Received register request: " + newStudent);
        return studentService.registerNewStudent(newStudent);
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateStudent(@RequestBody AuthRequest credentials) {
        try {
            UniversityStudent student = studentService.authenticateStudent(
                credentials.getEmailAddress(), 
                credentials.getLoginPassword()
            );
            return ResponseEntity.ok(student);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    // @GetMapping("/courses")
    // public List<AcademicCourse> getAllAvailableCourses() {
    //     return studentService.getAllAvailableCourses();
    // }

    @GetMapping("/{id}")
    public UniversityStudent getStudentProfile(@PathVariable Long id) {
        return studentService.getStudentProfile(id);
    }

    @PutMapping("/{id}")
    public UniversityStudent updateStudentProfile(@PathVariable Long id,
                                      @RequestParam String name,
                                      @RequestParam String surname) {
        return studentService.updateStudentProfile(id, name, surname);
    }

    @DeleteMapping("/remove/{studentId}")
    public ResponseEntity<?> removeStudent(@PathVariable Long studentId) {
        try {
            studentService.deleteStudentAccount(studentId);
            return ResponseEntity.ok("Student removed successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/graduation/{id}")
    public boolean checkGraduationStatus(@PathVariable Long id) {
        return studentService.verifyGraduationEligibility(id);
    }
}
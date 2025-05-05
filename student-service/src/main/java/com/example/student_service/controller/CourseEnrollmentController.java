package com.example.student_service.controller;

import com.example.student_service.model.AcademicCourse;
import com.example.student_service.model.UniversityStudent;
import com.example.student_service.service.CourseEnrollmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.example.student_service.repository.CourseDataRepository;
import java.util.Map;
import java.util.List;
import org.springframework.http.HttpStatus;

@RestController
@Transactional
@RequestMapping("/student")
public class CourseEnrollmentController {

    private final CourseDataRepository courseRepo;
    private final CourseEnrollmentService courseEnrollmentService;


    @Autowired
    public CourseEnrollmentController( CourseDataRepository courseRepo, CourseEnrollmentService courseEnrollmentService) {
        this.courseRepo = courseRepo;
        this.courseEnrollmentService = courseEnrollmentService;
    }

    @PostMapping("/courses/enroll")
    public ResponseEntity<?> enrollInCourse(@RequestParam Long studentId, @RequestParam Long courseId) {
        try {
            UniversityStudent student = courseEnrollmentService.enrolInCourse(studentId, courseId);
            
            return ResponseEntity.ok(student);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    

    @GetMapping("/courses/enrollments")
    public ResponseEntity<List<Map<String, Object>>> getStudentEnrollments(@RequestParam Long studentId) {
        List<Map<String, Object>> enrollments = courseEnrollmentService.getStudentEnrollments(studentId);
        return ResponseEntity.ok(enrollments);
    }

    @GetMapping("/courses/{studentId}/bills")
    public ResponseEntity<?> getStudentBills(
            @PathVariable Long studentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        try {
            List<Map<String, Object>> bills = 
            courseEnrollmentService.fetchStudentInvoices(studentId, status, type);
            return ResponseEntity.ok(bills);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                   .body("Failed to fetch bills: " + e.getMessage());
        }
    }


    @DeleteMapping("/courses/withdraw")
    public ResponseEntity<?> withdrawFromCourse(@RequestParam Long studentId, @RequestParam Long courseId) {
        try {
            courseEnrollmentService.unenrolFromCourse(studentId, courseId);
            return ResponseEntity.ok("Unenrolled successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    

    @GetMapping
    public List<AcademicCourse> getAllCourses() {
        return courseRepo.findAll();
    }
}
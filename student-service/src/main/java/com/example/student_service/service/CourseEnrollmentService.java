package com.example.student_service.service;

import com.example.student_service.model.UniversityStudent;
import com.example.student_service.model.AcademicCourse;
import com.example.student_service.repository.StudentProfileRepository;

import jakarta.persistence.EntityNotFoundException;

import com.example.student_service.repository.CourseDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.hibernate.service.spi.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CourseEnrollmentService {

    private final StudentProfileRepository studentRepo;
    private final CourseDataRepository courseRepo;
    private final RestTemplate restTemplate;
    private static final Logger logger = LoggerFactory.getLogger(CourseEnrollmentService.class);

    public CourseEnrollmentService(StudentProfileRepository studentRepo,
                                CourseDataRepository courseRepo,
                                RestTemplate restTemplate) {
        this.studentRepo = studentRepo;
        this.courseRepo = courseRepo;
        this.restTemplate = restTemplate;
    }

    public UniversityStudent enrolInCourse(Long studentId, Long courseId) {
        UniversityStudent student = studentRepo.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        AcademicCourse course = courseRepo.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
    
        if (student.getRegisteredCourses().stream()
                .anyMatch(c -> c.getCourseId().equals(courseId))) {
            throw new RuntimeException("Student already enrolled in this course");
        }
    
        student.getRegisteredCourses().add(course);
        UniversityStudent saved = studentRepo.save(student);
    
        Map<String, Object> invoicePayload = new HashMap<>();
        invoicePayload.put("studentId", studentId.toString());
        invoicePayload.put("amount", 100.0);
        invoicePayload.put("type", "TUITION_FEES");
        invoicePayload.put("description", "Tuition fee for course: " + course.getCourseTitle());
        invoicePayload.put("dueDate", LocalDate.now().plusDays(30).toString());
    
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:8081/invoices",
                invoicePayload,
                Map.class
            );
            String invoiceReference = (String) response.getBody().get("reference");
            course.setPaymentRef(invoiceReference);
            courseRepo.save(course);
        } catch (RestClientException e) {
            student.getRegisteredCourses().remove(course);
            studentRepo.save(student);
            throw new RuntimeException("Failed to create invoice: " + e.getMessage());
        }
    
        return saved;
    }

    public List<Map<String, Object>> getStudentEnrollments(Long studentId) {
        UniversityStudent student = studentRepo.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found"));
    
        return student.getRegisteredCourses().stream()
            .map(course -> {
                Map<String, Object> courseMap = new HashMap<>();
                courseMap.put("courseId", course.getCourseId());
                courseMap.put("courseTitle", course.getCourseTitle());
                courseMap.put("courseDescription", course.getCourseDescription());
                return courseMap;
            })
            .collect(Collectors.toList());
    }

    @Transactional
    public void unenrolFromCourse(Long studentId, Long courseId) {
        UniversityStudent student = studentRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        
        AcademicCourse course = courseRepo.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        boolean wasEnrolled = student.getRegisteredCourses()
                .removeIf(c -> c.getCourseId().equals(courseId));
        
        if (!wasEnrolled) {
            throw new RuntimeException("Student was not enrolled in this course");
        }
        
        studentRepo.save(student);

        String invoiceReference = course.getPaymentRef();
        if (invoiceReference != null) {
            try {
                restTemplate.delete("http://localhost:8081/invoices/" + invoiceReference + "/cancel");
                course.setPaymentRef(null);
                courseRepo.save(course);
            } catch (RestClientException e) {
                System.out.println("Invoice cancellation failed: " + e.getMessage());
            }
        }
    }

    public List<Map<String, Object>> fetchStudentInvoices(Long studentId, String status, String type) {
        List<Map<String, Object>> invoices = new ArrayList<>();

        // Step 1: Verify student has an account
        String accountUrl = "http://localhost:8081/accounts/student/" + studentId;
        try {
            ResponseEntity<Map<String, Object>> accountResponse = restTemplate.exchange(
                accountUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (!accountResponse.getStatusCode().is2xxSuccessful() || accountResponse.getBody() == null) {
                return invoices; // No account, return empty list
            }
        } catch (RestClientException e) {
            logger.warn("No account found for student ID {}: {}", studentId, e.getMessage());
            return invoices;
        }

        // Step 2: Fetch invoices by iterating over possible IDs
        for (long invoiceId = 1; invoiceId <= 100; invoiceId++) { // Arbitrary upper limit
            try {
                String invoiceUrl = "http://localhost:8081/invoices/" + invoiceId;
                ResponseEntity<Map<String, Object>> invoiceResponse = restTemplate.exchange(
                    invoiceUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
                );

                if (invoiceResponse.getStatusCode().is2xxSuccessful() && invoiceResponse.getBody() != null) {
                    Map<String, Object> invoice = invoiceResponse.getBody();
                    Object invoiceStudentId = invoice.get("studentId");

                    if (invoiceStudentId != null && invoiceStudentId.toString().equals(studentId.toString())) {
                        boolean matchesStatus = status == null || status.isEmpty() || 
                                                invoice.get("status").toString().equalsIgnoreCase(status);
                        boolean matchesType = type == null || type.isEmpty() || 
                                              invoice.get("type").toString().equalsIgnoreCase(type);

                        if (matchesStatus && matchesType) {
                            invoices.add(invoice);
                        }
                    }
                }
            } catch (RestClientException e) {
                logger.debug("Skipping invoice ID {} for student ID {}: {}", invoiceId, studentId, e.getMessage());
            }
        }

        return invoices;
    }

}
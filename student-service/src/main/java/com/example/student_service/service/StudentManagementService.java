package com.example.student_service.service;

import com.example.student_service.model.UniversityStudent;
import com.example.student_service.repository.StudentProfileRepository;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDate;
import java.util.*;
import com.example.student_service.model.AcademicCourse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class StudentManagementService {

    private final StudentProfileRepository studentRepo;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;
    private static final Logger log = LoggerFactory.getLogger(StudentManagementService.class);

    public StudentManagementService(StudentProfileRepository studentRepo,
                                 PasswordEncoder passwordEncoder,
                                 RestTemplate restTemplate) {
        this.studentRepo = studentRepo;
        this.passwordEncoder = passwordEncoder;
        this.restTemplate = restTemplate;
    }

    public UniversityStudent registerNewStudent(UniversityStudent student) {
        if (studentRepo.existsByEmailAddress(student.getEmailAddress())) {
            throw new IllegalArgumentException("Email already registered");
        }

        student.setLoginPassword(passwordEncoder.encode(student.getLoginPassword()));
        UniversityStudent savedStudent = studentRepo.save(student);
        // Initialize external accounts (async with retry)
        try {
            initializeExternalAccounts(savedStudent.getStudentId());
        } catch (Exception e) {
            log.error("External account initialization failed", e);
            // Continue with registration anyway
        }
    
        return savedStudent;
    }

    private void initializeExternalAccounts(Long studentId) {
        Map<String, String> payload = Map.of("studentId", studentId.toString());
        restTemplate.postForObject("http://localhost:8081/accounts", payload, String.class);
        restTemplate.postForObject("http://localhost:8082/api/register", payload, String.class);
    }

    public UniversityStudent authenticateStudent(String email, String password) {
        return studentRepo.findByEmailAddressIgnoreCase(email)
            .filter(student -> passwordEncoder.matches(password, student.getLoginPassword()))
            .orElseThrow(() -> new SecurityException("Invalid credentials"));
    }

    public boolean studentExists(Long studentId) {
        return studentRepo.existsById(studentId);
    }

    public UniversityStudent getStudentProfile(Long studentId) {
        return studentRepo.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found"));
    }

    public List<AcademicCourse> getAllAvailableCourses() {
        // Would normally call course service
        return Collections.emptyList();
    }

    public UniversityStudent updateStudentProfile(Long studentId, String firstName, String lastName) {
        UniversityStudent student = getStudentProfile(studentId);
        if (firstName != null) student.setFirstName(firstName);
        if (lastName != null) student.setLastName(lastName);
        return studentRepo.save(student);
    }

    public void deleteStudentAccount(Long studentId) {

        UniversityStudent student = studentRepo.findById(studentId)
        .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));

        // 2. Clear course enrollments
        student.getRegisteredCourses().clear();
        studentRepo.save(student);
        //logger.info("Cleared enrollments for student {}", studentId);

        try {
            // First try to get account ID
            ResponseEntity<Map> response = restTemplate.exchange(
                "http://localhost:8081/accounts/student/" + studentId,
                HttpMethod.GET,
                null,
                Map.class);

            if (response.getStatusCode().is2xxSuccessful() && 
                response.getBody() != null && 
                response.getBody().containsKey("id")) {
                
                Long accountId = ((Number) response.getBody().get("id")).longValue();
                restTemplate.delete("http://localhost:8081/accounts/" + accountId);
                log.info("Deleted finance account {} for student {}", accountId, studentId);
            }
        } catch (RestClientException e) {
            log.warn("Failed to delete finance account for student {}: {}", studentId, e.getMessage());
        }

        // 4. Delete student
        studentRepo.delete(student);
    }

public boolean verifyGraduationEligibility(Long studentId) {
        UniversityStudent student = getStudentProfile(studentId);
        
        // Check if student has no registered courses
        boolean noRegisteredCourses = student.getRegisteredCourses().isEmpty();
        
        // Check invoice statuses
        boolean allInvoicesCancelledOrNone = areAllInvoicesCancelledOrNone(studentId);
        
        // Eligible if no registered courses and all invoices are CANCELLED or none exist
        boolean eligible = noRegisteredCourses && allInvoicesCancelledOrNone;
        
        // Update student's graduation status
        student.setHasGraduated(eligible);
        studentRepo.save(student);
        
        return eligible;
    }

    private boolean areAllInvoicesCancelledOrNone(Long studentId) {
        // Fetch all invoices for the student (no status or type filter)
        List<Map<String, Object>> invoices = fetchStudentInvoices(studentId, null, null);
        
        // If no invoices, student is eligible
        if (invoices.isEmpty()) {
            return true;
        }
        
        // Check if all invoices have CANCELLED status
        return invoices.stream()
                .allMatch(invoice -> "CANCELLED".equalsIgnoreCase((String) invoice.get("status")));
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
            log.warn("No account found for student ID {}: {}", studentId, e.getMessage());
            return invoices;
        }

        // Step 2: Fetch invoices for the student
        try {
            StringBuilder invoiceUrl = new StringBuilder("http://localhost:8081/invoices?studentId=" + studentId);
            if (status != null && !status.isEmpty()) {
                invoiceUrl.append("&status=").append(status);
            }
            if (type != null && !type.isEmpty()) {
                invoiceUrl.append("&type=").append(type);
            }

            ResponseEntity<List<Map<String, Object>>> invoiceResponse = restTemplate.exchange(
                invoiceUrl.toString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            if (invoiceResponse.getStatusCode().is2xxSuccessful() && invoiceResponse.getBody() != null) {
                invoices = invoiceResponse.getBody();
            }
        } catch (RestClientException e) {
            log.error("Failed to fetch invoices for student ID {}: {}", studentId, e.getMessage());
        }

        return invoices;
    }
}
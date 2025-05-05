package com.example.student_service;

import com.example.student_service.model.AcademicCourse;
import com.example.student_service.repository.CourseDataRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@SpringBootApplication
@EnableScheduling
public class StudentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentServiceApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    CommandLineRunner initializeCourseData(CourseDataRepository courseRepo) {
        return args -> {
            // Step 1: Clean up duplicate courses
            List<AcademicCourse> existingCourses = courseRepo.findAll();
            Map<String, AcademicCourse> uniqueCourses = new HashMap<>();
            List<AcademicCourse> duplicates = new ArrayList<>();

            for (AcademicCourse course : existingCourses) {
                String courseKey = course.getCourseTitle() + "|" + course.getCourseDescription();
                if (uniqueCourses.containsKey(courseKey)) {
                    duplicates.add(course);
                } else {
                    uniqueCourses.put(courseKey, course);
                }
            }

            // Delete duplicates (keeping the newest by ID)
            duplicates.sort(Comparator.comparingLong(AcademicCourse::getCourseId).reversed());
            for (AcademicCourse duplicate : duplicates) {
                String courseKey = duplicate.getCourseTitle() + "|" + duplicate.getCourseDescription();
                AcademicCourse keeper = uniqueCourses.get(courseKey);
                if (duplicate.getCourseId() > keeper.getCourseId()) {
                    courseRepo.delete(duplicate);
                } else {
                    courseRepo.delete(keeper);
                    uniqueCourses.put(courseKey, duplicate);
                }
            }

            // Step 2: Ensure required courses exist
            createCourseIfMissing(courseRepo, "Introduction to Java", 
                "Fundamentals of Java programming language");
            createCourseIfMissing(courseRepo, "Web Development Fundamentals", 
                "Building modern web applications");
            createCourseIfMissing(courseRepo, "Database Systems", 
                "Relational databases and SQL programming");
            createCourseIfMissing(courseRepo, "Spring Framework", 
                "Enterprise application development with Spring");
        };
    }

    private void createCourseIfMissing(CourseDataRepository courseRepo, 
                                     String title, String description) {
        if (courseRepo.findByCourseTitleIgnoreCase(title) == null) {
            AcademicCourse newCourse = new AcademicCourse();
            newCourse.setCourseTitle(title);
            newCourse.setCourseDescription(description);
            courseRepo.save(newCourse);
        }
    }
}
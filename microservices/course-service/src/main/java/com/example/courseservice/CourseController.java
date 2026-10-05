package com.example.courseservice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service giả lập. Tin tưởng header do Gateway gắn (X-User-Role, X-User-Name),
 * vì vậy chỉ được truy cập qua Gateway, không mở port 8083 ra ngoài.
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final AtomicLong idSeq = new AtomicLong(3);
    private final List<Course> courses = new CopyOnWriteArrayList<>(List.of(
            new Course(1L, "Spring Boot cơ bản", "thaygiao"),
            new Course(2L, "Microservices với Spring Cloud", "thaygiao"),
            new Course(3L, "Bảo mật API với JWT", "thaygiao")
    ));

    /** Mọi role. Trả kèm header nhận được để kiểm chứng Gateway đã gắn X-User-Role. */
    @GetMapping
    public Map<String, Object> getAll(@RequestHeader(value = "X-User-Role", required = false) String role,
                                      @RequestHeader(value = "X-User-Name", required = false) String username) {
        return Map.of(
                "receivedHeaders", Map.of(
                        "X-User-Role", String.valueOf(role),
                        "X-User-Name", String.valueOf(username)),
                "courses", courses
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Course> getById(@PathVariable Long id) {
        return courses.stream().filter(c -> c.id().equals(id)).findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Ví dụ phân quyền dựa trên header: chỉ INSTRUCTOR/ADMIN được tạo khóa học. */
    @PostMapping
    public ResponseEntity<?> create(@RequestHeader(value = "X-User-Role", required = false) String role,
                                    @RequestHeader(value = "X-User-Name", required = false) String username,
                                    @RequestBody Map<String, String> body) {
        if (!"INSTRUCTOR".equals(role) && !"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("status", 403, "message", "Chỉ INSTRUCTOR hoặc ADMIN được tạo khóa học"));
        }
        Course course = new Course(idSeq.incrementAndGet(), body.getOrDefault("title", "Khóa học mới"), username);
        courses.add(course);
        return ResponseEntity.status(HttpStatus.CREATED).body(course);
    }
}

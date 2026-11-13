package com.thesismate.config;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.thesismate.model.StudentRecord;
import com.thesismate.repository.AppUserRepository;
import com.thesismate.repository.StudentRecordRepository;

/**
 * Dev-only credits and CGPA for the seeded students (20000001 to 20000010).
 * Chosen to cover the boundaries of the F6 eligibility rules. Safe to run repeatedly.
 * Needs the coordinator account from DevDataSeeder; if it is missing, restart once.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "thesismate.seed", havingValue = "true")
@Order(Ordered.LOWEST_PRECEDENCE)
public class DevStudentRecordSeeder implements CommandLineRunner {

    private final AppUserRepository users;
    private final StudentRecordRepository records;

    public DevStudentRecordSeeder(AppUserRepository users, StudentRecordRepository records) {
        this.users = users;
        this.records = records;
    }

    @Override
    public void run(String... args) {
        var coordinator = users.findByEmail("coordinator@thesismate.test");
        if (coordinator.isEmpty()) {
            return;
        }
        Long by = coordinator.get().getId();
        add("20000001", 110, "3.50", by); // eligible for every track
        add("20000002", 100, "3.20", by); // eligible for every track
        add("20000003", 90, "3.00", by);  // exactly 3.00: eligible for thesis
        add("20000004", 75, "2.50", by);  // exactly 75 credits, only project and internship
        add("20000005", 75, "3.00", by);  // both boundaries at once: eligible for every track
        add("20000006", 74, "3.80", by);  // 1 credit short: not eligible
        add("20000007", 120, "2.00", by); // exactly 2.00: project and internship only
        add("20000008", 80, "1.99", by);  // CGPA too low: not eligible
        add("20000009", 100, "3.40", by); // eligible for every track
        add("20000010", 95, "3.10", by);  // eligible for every track
    }

    private void add(String studentId, int credits, String cgpa, Long importedBy) {
        if (records.findByStudentId(studentId).isEmpty()) {
            records.save(new StudentRecord(studentId, credits, new BigDecimal(cgpa), importedBy));
        }
    }
}
package com.thesismate.service;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.dto.ImportResult;
import com.thesismate.model.StudentRecord;
import com.thesismate.repository.StudentRecordRepository;

@ExtendWith(MockitoExtension.class)
class StudentRecordImportServiceTest {

    private static final String HEADER = "student_id,credits_completed,cgpa";

    @Mock StudentRecordRepository records;

    private StudentRecordImportService service;

    @BeforeEach
    void setUp() {
        service = new StudentRecordImportService(records);
    }

    @Test
    void importsValidRowsAndIgnoresBlankLines() {
        ImportResult r = service.importCsv(1L, HEADER + "\r\n20000001,110,3.50\r\n\r\n20000002,95,3\r\n");
        assertEquals(2, r.created());
        assertEquals(0, r.updated());
        assertTrue(r.errors().isEmpty());
        verify(records, times(2)).save(any(StudentRecord.class));
    }

    @Test
    void acceptsFileWithByteOrderMark() {
        ImportResult r = service.importCsv(1L, "\uFEFF" + HEADER + "\n20000001,100,3.20");
        assertEquals(1, r.created());
    }

    @Test
    void existingStudentIsUpdatedNotDuplicated() {
        StudentRecord existing = new StudentRecord("20000001", 50, new BigDecimal("2.00"), 9L);
        when(records.findByStudentId("20000001")).thenReturn(Optional.of(existing));

        ImportResult r = service.importCsv(1L, HEADER + "\n20000001,80,2.50");

        assertEquals(0, r.created());
        assertEquals(1, r.updated());
        assertEquals(80, existing.getCreditsCompleted());
        assertEquals(0, new BigDecimal("2.50").compareTo(existing.getCgpa()));
    }

    @Test
    void badRowsAreReportedWithLineNumbersAndGoodRowsStillSaved() {
        String csv = HEADER + "\n"
                + "2000001,100,3.0\n"      // line 2: 7 digit ID
                + "20000002,-5,3.0\n"      // line 3: negative credits
                + "20000003,100,4.5\n"     // line 4: CGPA above 4
                + "20000004,100,3.456\n"   // line 5: too many decimals
                + "20000005,100\n"         // line 6: missing column
                + "20000006,abc,3.0\n"     // line 7: credits not a number
                + "20000007,100,3.0\n"     // line 8: valid
                + "20000007,90,2.0\n";     // line 9: duplicate ID

        ImportResult r = service.importCsv(1L, csv);

        assertEquals(1, r.created());
        assertEquals(7, r.errors().size());
        assertTrue(r.errors().get(0).startsWith("Line 2"));
        assertTrue(r.errors().get(6).startsWith("Line 9"));
        verify(records, times(1)).save(any(StudentRecord.class));
    }

    @Test
    void wrongHeaderRejectsWholeFile() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.importCsv(1L, "id,credits,gpa\n20000001,100,3.0"));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void emptyFileIsRejected() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.importCsv(1L, "  "));
        assertEquals(400, ex.getStatusCode().value());
    }
}
package com.thesismate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** F11 file upload setup: PDF checks, safe storage names, SHA-256 and safe reading. Uses a temporary folder. */
class FileStorageServiceTest {

    private static final byte[] PDF_BYTES = "%PDF-1.4 test content".getBytes(StandardCharsets.US_ASCII);

    @TempDir
    Path tempDir;

    private Path uploads;
    private FileStorageService service;

    @BeforeEach
    void setUp() {
        uploads = tempDir.resolve("uploads");
        service = new FileStorageService(uploads.toString());
    }

    private MockMultipartFile file(String originalName, byte[] content) {
        return new MockMultipartFile("report", originalName, "application/pdf", content);
    }

    private int statusOf(ResponseStatusException e) {
        return e.getStatusCode().value();
    }

    // ---------- validatePdf ----------

    @Test
    void validatePdf_validFile_isAccepted() {
        service.validatePdf(file("thesis.pdf", PDF_BYTES), "The report PDF");
    }

    @Test
    void validatePdf_upperCaseExtension_isAccepted() {
        service.validatePdf(file("THESIS.PDF", PDF_BYTES), "The report PDF");
    }

    @Test
    void validatePdf_nullFile_is400() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.validatePdf(null, "The report PDF"));
        assertEquals(400, statusOf(e));
        assertEquals("The report PDF is required", e.getReason());
    }

    @Test
    void validatePdf_emptyFile_is400() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.validatePdf(file("thesis.pdf", new byte[0]), "The report PDF"));
        assertEquals(400, statusOf(e));
    }

    @Test
    void validatePdf_wrongExtension_is400() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.validatePdf(file("thesis.docx", PDF_BYTES), "The report PDF"));
        assertEquals(400, statusOf(e));
        assertEquals("The report PDF must be a PDF file", e.getReason());
    }

    @Test
    void validatePdf_pdfNameButNotPdfContent_is400() {
        byte[] fake = "just some text".getBytes(StandardCharsets.US_ASCII);
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.validatePdf(file("fake.pdf", fake), "The report PDF"));
        assertEquals(400, statusOf(e));
        assertEquals("The report PDF is not a valid PDF file", e.getReason());
    }

    @Test
    void validatePdf_tooLarge_is400() {
        byte[] big = new byte[(int) FileStorageService.MAX_BYTES + 1];
        System.arraycopy(PDF_BYTES, 0, big, 0, 5);
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.validatePdf(file("big.pdf", big), "The report PDF"));
        assertEquals(400, statusOf(e));
        assertEquals("The report PDF must be 20 MB or smaller", e.getReason());
    }

    // ---------- store ----------

    @Test
    void store_validPdf_savesFileWithRandomNameSizeAndHash() throws Exception {
        FileStorageService.StoredFile stored = service.store(file("thesis.pdf", PDF_BYTES), "The report PDF");

        assertTrue(stored.key().endsWith(".pdf"));
        assertNotEquals("thesis.pdf", stored.key());
        assertEquals(PDF_BYTES.length, stored.size());
        String expected = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(PDF_BYTES));
        assertEquals(expected, stored.sha256());
        assertTrue(Files.isRegularFile(uploads.resolve(stored.key())));
        assertTrue(Arrays.equals(PDF_BYTES, Files.readAllBytes(uploads.resolve(stored.key()))));
    }

    @Test
    void store_sameFileTwice_getsTwoDifferentKeys() {
        FileStorageService.StoredFile a = service.store(file("a.pdf", PDF_BYTES), "The report PDF");
        FileStorageService.StoredFile b = service.store(file("a.pdf", PDF_BYTES), "The report PDF");
        assertNotEquals(a.key(), b.key());
        assertEquals(a.sha256(), b.sha256());
    }

    @Test
    void store_pathTraversalFileName_cannotEscapeTheFolder() throws IOException {
        FileStorageService.StoredFile stored = service.store(file("../evil.pdf", PDF_BYTES), "The report PDF");

        assertFalse(stored.key().contains(".."));
        assertFalse(stored.key().contains("/"));
        assertFalse(Files.exists(tempDir.resolve("evil.pdf")));
        try (Stream<Path> files = Files.list(uploads)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void store_invalidFile_savesNothing() {
        assertThrows(ResponseStatusException.class,
                () -> service.store(file("fake.pdf", "not a pdf".getBytes(StandardCharsets.US_ASCII)), "The report PDF"));
        assertFalse(Files.exists(uploads));
    }

    // ---------- load ----------

    @Test
    void load_storedKey_returnsTheFile() throws Exception {
        FileStorageService.StoredFile stored = service.store(file("thesis.pdf", PDF_BYTES), "The report PDF");

        Resource resource = service.load(stored.key());

        assertTrue(resource.exists());
        assertEquals(PDF_BYTES.length, resource.contentLength());
    }

    @Test
    void load_unknownKey_is404() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.load("nothing.pdf"));
        assertEquals(404, statusOf(e));
    }

    @Test
    void load_keyOutsideTheFolder_is404EvenIfTheFileExists() throws IOException {
        Files.createDirectories(uploads);
        Files.writeString(tempDir.resolve("secret.txt"), "secret");

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.load("../secret.txt"));

        assertEquals(404, statusOf(e));
    }
}

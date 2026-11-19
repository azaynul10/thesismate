package com.thesismate.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * F11 file upload setup: checks that an upload is a real PDF, stores it under a random name in the upload folder
 * (default: backend/uploads, which Git ignores) and returns its size and SHA-256 fingerprint.
 * The original file name is never used as a path, so a name like "../x.pdf" cannot escape the folder.
 */
@Service
public class FileStorageService {

    public static final long MAX_BYTES = 20L * 1024 * 1024;

    /** What was stored: the random key to read it back, its size and SHA-256 (hex). */
    public record StoredFile(String key, long size, String sha256) {
    }

    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final Path root;

    public FileStorageService(@Value("${thesismate.storage.dir:uploads}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    /** Throws 400 unless the file is present, at most 20 MB, named .pdf and starts with the PDF signature. */
    public void validatePdf(MultipartFile file, String label) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " must be 20 MB or smaller");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " must be a PDF file");
        }
        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(PDF_MAGIC.length);
            if (!Arrays.equals(head, PDF_MAGIC)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " is not a valid PDF file");
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " could not be read");
        }
    }

    public StoredFile store(MultipartFile file, String label) {
        validatePdf(file, label);
        String key = UUID.randomUUID() + ".pdf";
        try {
            byte[] bytes = file.getBytes();
            Files.createDirectories(root);
            Files.write(root.resolve(key), bytes, StandardOpenOption.CREATE_NEW);
            return new StoredFile(key, bytes.length, sha256(bytes));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "The file could not be saved");
        }
    }

    /** Reads a stored file by its key. Unknown keys and keys that point outside the folder give 404. */
    public Resource load(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        return new FileSystemResource(path);
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}

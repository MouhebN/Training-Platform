package com.training.platform.common.storage;

import com.training.platform.common.exception.BadRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CvStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    private final Path uploadDir;

    public CvStorageService(@Value("${app.upload.cv-dir:uploads/cvs}") String uploadDir) {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public String store(Long trainerId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("CV file is required");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BadRequestException("CV must be 5MB or smaller");
        }
        String extension = extension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("CV must be a PDF or Word document");
        }
        try {
            Files.createDirectories(uploadDir);
            String filename = "trainer-" + trainerId + "-" + UUID.randomUUID() + "." + extension;
            Path target = uploadDir.resolve(filename);
            file.transferTo(target);
            return filename;
        } catch (IOException exception) {
            throw new BadRequestException("Could not store CV file");
        }
    }

    public Path resolve(String storedName) {
        if (!StringUtils.hasText(storedName) || storedName.startsWith("http")) {
            return null;
        }
        Path path = uploadDir.resolve(storedName).normalize();
        if (!path.startsWith(uploadDir) || !Files.exists(path)) {
            return null;
        }
        return path;
    }

    public void deleteIfStored(String storedName) {
        Path path = resolve(storedName);
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // leftover files should not block a new upload
        }
    }

    public MediaType mediaType(String storedName) {
        String extension = extension(storedName);
        return switch (extension) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "doc" -> MediaType.parseMediaType("application/msword");
            case "docx" -> MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            );
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    private String extension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}

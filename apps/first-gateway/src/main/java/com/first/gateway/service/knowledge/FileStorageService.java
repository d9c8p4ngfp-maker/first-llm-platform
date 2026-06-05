package com.first.gateway.service.knowledge;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload-dir:/app/uploads}")
    private String uploadDir;

    public record FileStorageResult(String filePath, String fileType, long fileSize) {}

    public FileStorageResult store(MultipartFile file, Long kbId) throws IOException {
        Path dir = Paths.get(uploadDir, "kb_" + kbId);
        Files.createDirectories(dir);
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path target = dir.resolve(fileName);
        file.transferTo(target.toFile());
        return new FileStorageResult(target.toString(), resolveFileType(fileName), file.getSize());
    }

    private String resolveFileType(String name) {
        String ext = name.substring(name.lastIndexOf('.') + 1).toUpperCase();
        return switch (ext) {
            case "PDF" -> "PDF";
            case "HTML", "HTM" -> "HTML";
            case "MD" -> "MARKDOWN";
            case "DOCX" -> "DOCX";
            default -> "TXT";
        };
    }
}

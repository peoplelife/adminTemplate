package com.admin.template.service.file;

import com.admin.template.config.UploadProperties;
import com.admin.template.dto.file.UploadedFile;
import com.admin.template.common.exception.AppException;
import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import com.admin.template.common.exception.SystemError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class FileService {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_FILE_COUNT = 10;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final Path publicRoot;

    @Autowired
    public FileService(UploadProperties properties) {
        this(Path.of(properties.getDirectory()));
    }

    public FileService(Path publicRoot) {
        this.publicRoot = publicRoot.toAbsolutePath().normalize();
    }

    public List<UploadedFile> upload(List<MultipartFile> files, String requestedPath) {
        if (files == null || files.isEmpty()) throw validation("请选择要上传的文件");
        if (files.size() > MAX_FILE_COUNT) throw validation("文件数量超过限制（最多10个）");
        for (MultipartFile file : files) {
            if (file == null) throw validation("请选择要上传的文件");
            if (file.getSize() > MAX_FILE_SIZE) throw validation("文件大小超过限制（最大10MB）");
            if (!ALLOWED_MIME_TYPES.contains(file.getContentType())) throw validation("不支持的文件类型");
        }

        String relativeDirectory = normalizeDirectory(requestedPath);
        Path targetDirectory = resolveInsideRoot(relativeDirectory);
        List<Path> created = new ArrayList<>();
        List<UploadedFile> results = new ArrayList<>();
        try {
            Files.createDirectories(publicRoot);
            Path realRoot = publicRoot.toRealPath();
            Files.createDirectories(targetDirectory);
            Path realDirectory = targetDirectory.toRealPath();
            if (!realDirectory.startsWith(realRoot) || realDirectory.equals(realRoot)) {
                throw validation("文件路径不合法");
            }
            for (MultipartFile file : files) {
                String extension = extension(file.getOriginalFilename());
                String filename = System.currentTimeMillis() + "-" + UUID.randomUUID().toString().replace("-", "")
                        .substring(0, 12) + extension;
                Path destination = realDirectory.resolve(filename).normalize();
                if (!destination.startsWith(realRoot)) throw validation("文件路径不合法");
                file.transferTo(destination);
                created.add(destination);
                results.add(new UploadedFile("/public/" + relativeDirectory.replace('\\', '/') + "/" + filename,
                        filename, file.getOriginalFilename(), file.getSize(), file.getContentType()));
            }
            return results;
        } catch (IOException exception) {
            for (Path path : created) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Preserve the original upload failure.
                }
            }
            throw new AppException(SystemError.FILE_UPLOAD_FAILED, null, exception);
        }
    }

    public void deleteFile(String filePath) {
        if (filePath != null && filePath.startsWith("/public/")) filePath = filePath.substring("/public/".length());
        Path target = resolveInsideRoot(normalizeDirectory(filePath));
        try {
            if (!Files.exists(target)) {
                throw new BizException(BusinessError.FILE_NOT_FOUND);
            }
            Path realRoot = publicRoot.toRealPath();
            Path realTarget = target.toRealPath();
            if (!realTarget.startsWith(realRoot) || realTarget.equals(realRoot) || !Files.isRegularFile(realTarget)) {
                throw validation("文件路径不合法");
            }
            Files.delete(realTarget);
        } catch (IOException exception) {
            throw new AppException(SystemError.FILE_DELETE_FAILED, null, exception);
        }
    }

    private Path resolveInsideRoot(String relativePath) {
        Path relative = Path.of(relativePath);
        if (relative.isAbsolute()) throw validation("文件路径不合法");
        Path resolved = publicRoot.resolve(relative).normalize();
        if (resolved.equals(publicRoot) || !resolved.startsWith(publicRoot)) throw validation("文件路径不合法");
        return resolved;
    }

    private String normalizeDirectory(String value) {
        String normalized = value == null || value.isBlank() ? "uploads" : value.trim();
        normalized = normalized.replaceAll("^[\\/]+", "");
        if (normalized.contains(":") || normalized.indexOf('\0') >= 0) throw validation("文件路径不合法");
        return normalized;
    }

    private String extension(String filename) {
        if (filename == null) return "";
        String baseName = Path.of(filename.replace('\\', '/')).getFileName().toString();
        int dot = baseName.lastIndexOf('.');
        return dot < 0 ? "" : baseName.substring(dot);
    }

    private BizException validation(String message) {
        return new BizException(BusinessError.VALIDATION_ERROR, message, null);
    }
}

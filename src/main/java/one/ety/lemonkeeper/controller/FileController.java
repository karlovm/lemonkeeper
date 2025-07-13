package one.ety.lemonkeeper.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${lemonkeeper.api.token}")
    private String apiToken;

    private boolean isAuthorized(String token) {
        return token != null && token.equals(apiToken);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile( @RequestHeader(value = "X-API-Token", required = false) String token, @RequestParam("file") MultipartFile file) {
        try {
            if (!isAuthorized(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid API token");
            }
            // Создаем директорию если не существует
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Генерируем уникальное имя файла
            String fileName = file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);

            // Сохраняем файл
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return ResponseEntity.ok().body(new FileResponse(fileName, "File uploaded successfully"));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(new FileResponse(null, "Failed to upload file: " + e.getMessage()));
        }
    }


    @GetMapping("/disk-info")
    public ResponseEntity<Map<String, Object>> getDiskInfo(@RequestHeader(value = "X-API-Token", required = false) String token) {
        Map<String, Object> diskInfo = new HashMap<>();

        if (!isAuthorized(token)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            java.nio.file.FileStore fileStore = Files.getFileStore(uploadPath);

            long totalSpace = fileStore.getTotalSpace();
            long freeSpace = fileStore.getUsableSpace();
            long usedSpace = totalSpace - freeSpace;

            diskInfo.put("totalSpace", totalSpace);
            diskInfo.put("freeSpace", freeSpace);
            diskInfo.put("usedSpace", usedSpace);
            diskInfo.put("usagePercentage", (double) usedSpace / totalSpace * 100);

            // Подсчитываем размер загруженных файлов
            long uploadedFilesSize = 0;
            try (Stream<Path> paths = Files.list(uploadPath)) {
                uploadedFilesSize = paths.filter(Files::isRegularFile)
                        .mapToLong(path -> {
                            try {
                                return Files.size(path);
                            } catch (IOException e) {
                                return 0;
                            }
                        })
                        .sum();
            }

            diskInfo.put("uploadedFilesSize", uploadedFilesSize);
            diskInfo.put("uploadDirectory", uploadPath.toAbsolutePath().toString());

            return ResponseEntity.ok(diskInfo);

        } catch (IOException e) {
            diskInfo.put("error", "Ошибка при получении информации о диске: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(diskInfo);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<FileInfo>> listFiles(@RequestHeader(value = "X-API-Token", required = false) String token) {
        try {

            if (!isAuthorized(token)) {
                return ResponseEntity.badRequest().body(new ArrayList<>());
            }

            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            List<FileInfo> files = new ArrayList<>();
            Files.walk(uploadPath, 1)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        File file = path.toFile();
                        files.add(new FileInfo(
                                file.getName(),
                                file.length(),
                                file.lastModified()
                        ));
                    });

            return ResponseEntity.ok(files);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadFile(@RequestHeader(value = "X-API-Token", required = false) String token, @PathVariable String filename) {
        try {
            if (!isAuthorized(token)) {
                return ResponseEntity.badRequest().build();
            }

            Path filePath = Paths.get(uploadDir).resolve(filename);
            Resource resource = new FileSystemResource(filePath);

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/delete/{filename}")
    public ResponseEntity<?> deleteFile(@RequestHeader(value = "X-API-Token", required = false) String token, @PathVariable String filename) {
        try {

            if (!isAuthorized(token)) {
                return ResponseEntity.badRequest().body(new ArrayList<>());
            }

            Path filePath = Paths.get(uploadDir).resolve(filename);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                return ResponseEntity.ok().body(new FileResponse(filename, "File deleted successfully"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(new FileResponse(filename, "Failed to delete file: " + e.getMessage()));
        }
    }

    // Response classes
    static class FileResponse {
        private String filename;
        private String message;

        public FileResponse(String filename, String message) {
            this.filename = filename;
            this.message = message;
        }

        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    static class FileInfo {
        private String name;
        private long size;
        private long lastModified;

        public FileInfo(String name, long size, long lastModified) {
            this.name = name;
            this.size = size;
            this.lastModified = lastModified;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        public long getLastModified() { return lastModified; }
        public void setLastModified(long lastModified) { this.lastModified = lastModified; }
    }
}
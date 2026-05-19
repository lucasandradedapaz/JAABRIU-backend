package jaabriu.jaabriu_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileStorageConfig {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public String getUploadDir() {
        return uploadDir;
    }
}
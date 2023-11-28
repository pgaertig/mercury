package pl.amitec.mercury.providers.polsoft;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import pl.amitec.mercury.transport.FilesystemTransport;

import java.nio.file.Path;

/**
 * State of the FTP pull
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentState {
    private String rootDigest;
    private String exportDigest;
    private String contentDigest;
    private Path exportCacheDir;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private FilesystemTransport transport;
}

package pl.amitec.mercury.clients.bitbee.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.ZonedDateTime;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record Resource (
        Long id,
        String source,
        @JsonProperty("sourceid") String sourceId,
        String url,
        @JsonProperty("filename") String fileName,
        @JsonProperty("filetype") String fileType,
        @JsonProperty("filesize") Long fileSize,
        String type,
        String title,
        String description,
        String external,
        String active,
        ZonedDateTime added,
        ZonedDateTime modified
) {
}

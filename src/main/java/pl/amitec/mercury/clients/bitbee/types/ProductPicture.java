package pl.amitec.mercury.clients.bitbee.types;

import lombok.Builder;

@Builder
public record ProductPicture(
        Long id,
        String url,
        String filetype,
        String source,
        String sourceid
) {
}


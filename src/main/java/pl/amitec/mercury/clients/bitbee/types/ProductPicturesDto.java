package pl.amitec.mercury.clients.bitbee.types;

import lombok.Builder;

import java.util.List;

@Builder
public record ProductPicturesDto(
        Long id,
        List<ProductPicture> pictures
) {
}

package pl.amitec.mercury.integrators.polsoft;

import lombok.Builder;

@Builder
record PolsoftFtpConfig(
        String host,
        int port,
        String user,
        String password,
        String cacheDir,
        boolean readonly,
        boolean rerunLast
) {
}

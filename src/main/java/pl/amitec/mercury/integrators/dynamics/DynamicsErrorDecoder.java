package pl.amitec.mercury.integrators.dynamics;

import feign.Response;
import feign.codec.ErrorDecoder;

public class DynamicsErrorDecoder implements ErrorDecoder {

    private final OAuth2Session authSession;

    public DynamicsErrorDecoder(OAuth2Session authSession) {
        this.authSession = authSession;
    }

    @Override
    public Exception decode(String method, Response response) {
        if(response.status() == 401) {
            authSession.reset();
        }
        return new ErrorDecoder.Default().decode(method, response);
    }
}

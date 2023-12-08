package pl.amitec.mercury.integrators.dynamics;

import feign.RequestInterceptor;
import feign.RequestTemplate;

public class OAuth2FeignRequestInterceptor implements RequestInterceptor {

    private OAuth2Session authSession;

    public OAuth2FeignRequestInterceptor(OAuth2Session authSession) {
        this.authSession = authSession;
    }
    @Override
    public void apply(RequestTemplate requestTemplate) {
        requestTemplate.header("Authorization", "Bearer " + authSession.getAccessToken());
    }
}

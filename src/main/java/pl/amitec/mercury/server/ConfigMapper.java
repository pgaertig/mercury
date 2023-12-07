package pl.amitec.mercury.server;

import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.stereotype.Service;
import pl.amitec.mercury.Configurable;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converts configuration map to configuration object.
 */
@Service
public class ConfigMapper {

    public <T extends Configurable> T map(Map<String, String> configMap, Class<T> configClass) {
        Map<String, Object> configObjectMap = configMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        StandardEnvironment environment = new StandardEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.addFirst(new MapPropertySource("config", configObjectMap));

        BindHandler bindHandler = new IgnoreUnknownFieldsBindHandler();
        return Binder.get(environment).bind("", Bindable.of(configClass), bindHandler).get();
    }

    private static class IgnoreUnknownFieldsBindHandler implements BindHandler {
        public <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
            return target;
        }

        public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
            return result;
        }

        public Object onCreate(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
            return result;
        }

        public Object onFailure(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Exception error) throws Exception {
            throw error;
        }

        public void onFinish(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) throws Exception {
        }
    }
}

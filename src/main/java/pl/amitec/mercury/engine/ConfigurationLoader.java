package pl.amitec.mercury.engine;

import com.fasterxml.jackson.databind.PropertyName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.util.Properties;

public class ConfigurationLoader {
    public static <T> T loadConfiguration(Class<T> configClass, String... files) throws Exception {
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
        StandardEnvironment environment = new StandardEnvironment();
        MutablePropertySources propSources = environment.getPropertySources();

        for(String file: files) {
            Resource resource = resourceLoader.getResource(file);
            Properties props = PropertiesLoaderUtils.loadProperties(resource);
            propSources.addLast(new PropertiesPropertySource(file, props));
        }

        /*String provider = fileProps.getProperty("provider");
        Properties appProps = PropertiesLoaderUtils.loadProperties(
                new ClassPathResource(baseProps));
        */

        //propSources.addFirst(new PropertiesPropertySource(baseProps, appProps));

        //String className = environment.getProperty("class");
        //Class<? extends Configuration> configClass = (Class<? extends Configuration>) Class.forName(className);
        //Configuration config = Binder.get(environment).bind("", Bindable.of(configClass)).get();

        BindHandler bindHandler = new IgnoreUnknownFieldsBindHandler();

        T config = Binder.get(environment).bind("", Bindable.of(configClass), bindHandler).get();
        return config;
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

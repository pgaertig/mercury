package pl.amitec.mercury.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.amitec.mercury.Plan;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static pl.amitec.mercury.util.StructUtils.propertiesToMap;

@Service
public class PlanLoader {

    private static final Logger LOG = LoggerFactory.getLogger(PlanLoader.class);

    private List<Plan> plans;

    public PlanLoader(@Value("${mercury.plan.filesystem.path}") String path) {
        Map<String, Map<String, String>> allProperties = loadPropertiesFromDirectory(path);
        plans = allProperties.entrySet().stream().map(entry -> Plan.builder()
                        .name(entry.getValue().get("name"))
                        .integrator(entry.getValue().get("integrator"))
                        .planSource(entry.getKey())
                        .enabled(Boolean.parseBoolean(entry.getValue().get("enabled")))
                        .config(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        plans.forEach(plan -> LOG.info("Loaded plan \"{}\" from {}", plan.name(), plan.planSource()));

        // remove duplicates, log it, and keep the first one enabled, remove the rest
        Map<String, List<Plan>> plansByName = plans.stream().collect(Collectors.groupingBy(Plan::name));
        plansByName.forEach((name, planList) -> {
            if (planList.size() > 1) {
                LOG.warn("Found {} plans with name: {}", planList.size(), name);
                planList.stream().filter(plan -> !plan.enabled()).forEach(plan -> {
                    LOG.warn("Disabling plan \"{}\" from {}", plan.name(), plan.planSource());
                    plans.remove(plan);
                });
            }
        });
    }

    public static Map<String, Map<String, String>> loadPropertiesFromDirectory(String dir) {
        Map<String, Map<String, String>> propertiesMap = new HashMap<>();
        Path dirPath = Paths.get(dir);
        scanDirectory(dirPath, propertiesMap);
        return propertiesMap;
    }

    private static void scanDirectory(Path dirPath, Map<String, Map<String, String>> propertiesMap) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    scanDirectory(entry, propertiesMap);
                } else if (entry.toString().endsWith(".properties")) {
                    Properties properties = new Properties();

                    try (InputStream in = Files.newInputStream(entry)) {
                        properties.load(in);
                        var relativePath = dirPath.relativize(entry);
                        var fileNameWithoutExtension = relativePath.getFileName().toString().replaceFirst("[.]properties$", "");
                        properties.putIfAbsent("name", fileNameWithoutExtension);
                        propertiesMap.put(relativePath.toString(), propertiesToMap(properties));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Failed to load properties from directory: {}", dirPath, e);
        }
    }

    public List<Plan> getAllPlans() {
        return plans;
    }
}

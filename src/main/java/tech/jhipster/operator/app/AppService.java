package tech.jhipster.operator.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.jhipster.operator.core.K8SCoreRuntime;
import tech.jhipster.operator.crds.app.Application;
import tech.jhipster.operator.crds.app.ApplicationSpec;
import tech.jhipster.operator.crds.app.CustomService;
import tech.jhipster.operator.crds.app.ModuleDescr;
import tech.jhipster.operator.crds.gateway.Gateway;
import tech.jhipster.operator.crds.registry.Registry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AppService {
    private Logger logger = LoggerFactory.getLogger(AppService.class);
    private Map<String, Application> apps = new ConcurrentHashMap<>();
    private Map<String, String> appsUrls = new HashMap<>();

    @Autowired
    private K8SCoreRuntime k8SCoreRuntime;

    /*
     * Add the logic to define what are the rules for your application to be UP or DOWN
     */
    public boolean isAppHealthy(Application app) {
        //@TODO:  Compare modules against app.jdl modules
        Set<ModuleDescr> modules = app.getSpec().getModules();
        boolean moduleMissing = true;
        if (modules != null) {
            moduleMissing = modules.stream().filter(m -> m.getKind().equals("Module")).anyMatch(
                    m -> !k8SCoreRuntime.isServiceAvailable(m.getServiceName())
            );


        }
        String gateway = app.getSpec().getGateway();
        boolean gatewayAvailable = false;
        if (gateway != null && !gateway.isEmpty()) {
            gatewayAvailable = k8SCoreRuntime.isServiceAvailable(gateway);
        }
        String registry = app.getSpec().getRegistry();
        boolean registryAvailable = false;
        if (registry != null && !registry.isEmpty()) {
            registryAvailable = k8SCoreRuntime.isServiceAvailable(registry);
        }

        if (!moduleMissing && gatewayAvailable && registryAvailable) {
            return true;
        }

        return false;
    }

    public void addGatewayToApp(Gateway gateway) {
        String appName = gateway.getMetadata().getLabels().get("app");
        if (appName != null && !appName.isEmpty()) {
            Application application = apps.get(appName);
            if (application != null) {
                ApplicationSpec spec = application.getSpec();
                if (k8SCoreRuntime.isServiceAvailable(gateway.getSpec().getServiceName())) {
                    spec.setGateway(gateway.getSpec().getServiceName());
                    application.setSpec(spec);
                    apps.put(application.getMetadata().getName(), application);
                    logger.info("> Application: " + appName + " updated with Registry " + gateway.getMetadata().getName());
                } else {
                    logger.error("Registry: " + gateway.getSpec().getServiceName() + " doesn't exist. ");
                }
            }
        } else {
            logger.error("> Orphan Service: " + gateway.getMetadata().getName());
        }
    }

    public void addRegistryToApp(Registry registry) {
        String appName = registry.getMetadata().getLabels().get("app");
        if (appName != null && !appName.isEmpty()) {
            Application application = apps.get(appName);
            if (application != null) {
                ApplicationSpec spec = application.getSpec();
                if (k8SCoreRuntime.isServiceAvailable(registry.getSpec().getServiceName())) {
                    spec.setRegistry(registry.getSpec().getServiceName());
                    application.setSpec(spec);
                    apps.put(application.getMetadata().getName(), application);
                    logger.info("> Application: " + appName + " updated with Registry " + registry.getMetadata().getName());
                } else {
                    logger.error("Registry: " + registry.getSpec().getServiceName() + " doesn't exist. ");
                }
            }
        } else {
            logger.error("> Orphan Service: " + registry.getMetadata().getName());
        }
    }

    public void addModuleToApp(CustomService module) {
        String appName = module.getMetadata().getLabels().get("app");
        if (appName != null && !appName.isEmpty()) {
            Application application = apps.get(appName);
            if (application != null) {
                ApplicationSpec spec = application.getSpec();
                Set<ModuleDescr> modules = spec.getModules();
                if (modules == null) {
                    modules = new HashSet<>();
                }
                if (k8SCoreRuntime.isServiceAvailable(module.getSpec().getServiceName())) {
                    modules.add(new ModuleDescr(module.getMetadata().getName(), module.getKind(), module.getSpec().getServiceName()));
                    spec.setModules(modules);
                    application.setSpec(spec);
                    apps.put(application.getMetadata().getName(), application);
                    logger.info("> Application: " + appName + " updated with Service " + module.getMetadata().getName());
                } else {
                    logger.error("Service: " + module.getSpec().getServiceName() + " doesn't exist. ");
                }
            }
        } else {
            logger.error("> Orphan Service: " + module.getMetadata().getName());
        }
    }

    public void removeGatewayFromApp(Gateway gateway) {
        String appName = gateway.getMetadata().getLabels().get("app");
        if (appName != null && !appName.isEmpty()) {
            Application application = apps.get(appName);
            if (application != null) {
                ApplicationSpec spec = application.getSpec();
                spec.setGateway("");
                application.setSpec(spec);
                apps.put(application.getMetadata().getName(), application);
                logger.info(">> Gateway removed " + gateway.getMetadata().getName() + " from app " + appName);
            }
        }
    }


    public void removeRegistryFromApp(Registry service) {
        String appName = service.getMetadata().getLabels().get("app");
        if (appName != null && !appName.isEmpty()) {
            Application application = apps.get(appName);
            if (application != null) {
                ApplicationSpec spec = application.getSpec();
                spec.setRegistry("");
                application.setSpec(spec);
                apps.put(application.getMetadata().getName(), application);
                logger.info(">> Registry removed " + service.getMetadata().getName() + " from app " + appName);
            }
        }
    }

    public void removeServiceFromApp(CustomService service) {
        String appName = service.getMetadata().getLabels().get("app");
        if (appName != null && !appName.isEmpty()) {
            Application application = apps.get(appName);
            if (application != null) {
                ApplicationSpec spec = application.getSpec();
                Set<ModuleDescr> modules = spec.getModules();
                if (modules == null) {
                    modules = new HashSet<>();
                }
                modules.removeIf(m -> m.getKind().equals(service.getKind()) && m.getName().equals(service.getMetadata().getName()));
                spec.setModules(modules);
                application.setSpec(spec);
                apps.put(application.getMetadata().getName(), application);
                logger.info(">> Deleted Service " + service.getMetadata().getName() + " from app " + appName);
            }
        }
    }


    public List<String> getApps() {
        return apps.values().stream()
                .filter(app -> isAppHealthy(app))
                .map(a -> a.getMetadata().getName())
                .collect(Collectors.toList());
    }

    public void addApp(String appName, Application app) {
        apps.put(appName, app);
    }

    public Application removeApp(String appName) {
        return apps.remove(appName);
    }

    public Application getApp(String appName) {
        return apps.get(appName);
    }

    public String getAppUrl(String appName) {
        return appsUrls.get(appName);
    }

    public void addAppUrl(String appName, String url) {
        appsUrls.put(appName, url);
    }

    public Map<String, Application> getAppsMap() {
        return apps;
    }

    public Map<String, String> getAppsUrls() {
        return appsUrls;
    }
}
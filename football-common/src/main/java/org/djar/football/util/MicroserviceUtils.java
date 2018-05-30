package org.djar.football.util;

import org.springframework.boot.system.ApplicationPid;
import org.springframework.util.ClassUtils;

public class MicroserviceUtils {

    private MicroserviceUtils() {
    }

    public static String applicationId(Class mainClass) {
        // a makeshift: it should be a microservice's instance identifier like IP or Kubernetes POD name
        // remove possible suffix $$EnhancerBySpringCGLIB from class name
        return ClassUtils.getUserClass(mainClass).getSimpleName() + new ApplicationPid();
    }
}

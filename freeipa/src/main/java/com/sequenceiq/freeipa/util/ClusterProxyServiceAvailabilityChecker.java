package com.sequenceiq.freeipa.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.freeipa.entity.Stack;

@Component
public class ClusterProxyServiceAvailabilityChecker {

    // feature supported from 2.21
    private static final Versioned DNS_BASED_SERVICE_NAME_AFTER_VERSION = () -> "2.20.0";

    public boolean isDnsBasedServiceNameAvailable(Stack stack) {
        if (StringUtils.isNotBlank(stack.getAppVersion())) {
            Versioned currentVersion = () -> stack.getAppVersion();
            return new VersionComparator().compare(currentVersion, DNS_BASED_SERVICE_NAME_AFTER_VERSION) > 0;
        } else {
            return false;
        }
    }

}

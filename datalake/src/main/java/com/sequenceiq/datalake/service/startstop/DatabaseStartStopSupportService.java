package com.sequenceiq.datalake.service.startstop;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class DatabaseStartStopSupportService {

    @Inject
    private EnvironmentClientService environmentClientService;

    public boolean supportsDatabaseStartStop(SdxCluster sdxCluster) {
        if (sdxCluster.hasExternalDatabase() && Strings.isNotEmpty(sdxCluster.getDatabaseCrn())) {
            DetailedEnvironmentResponse environment = environmentClientService.getByCrn(sdxCluster.getEnvCrn());

            return CloudPlatform.AWS.name().equalsIgnoreCase(environment.getCloudPlatform());
        }
        return false;
    }
}

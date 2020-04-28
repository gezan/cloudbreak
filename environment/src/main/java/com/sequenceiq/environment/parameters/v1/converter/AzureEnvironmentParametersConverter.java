package com.sequenceiq.environment.parameters.v1.converter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.parameters.dao.domain.AzureParameters;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.environment.parameters.dto.AzureParametersDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto.Builder;

@Component
public class AzureEnvironmentParametersConverter extends BaseEnvironmentParametersConverter {

    @Override
    protected BaseParameters createInstance() {
        return new AzureParameters();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    protected void postConvert(BaseParameters baseParameters, Environment environment, ParametersDto parametersDto) {
        super.postConvert(baseParameters, environment, parametersDto);
        AzureParameters azureParameters = (AzureParameters) baseParameters;
        Optional<AzureParametersDto> awsParametersDto = Optional.of(parametersDto)
                .map(ParametersDto::getAzureParametersDto);
        azureParameters.setResourceGroupName(awsParametersDto
                .map(AzureParametersDto::getResourceGroupName)
                .orElse(null));
    }

    @Override
    protected void postConvertToDto(Builder builder, BaseParameters source) {
        super.postConvertToDto(builder, source);
        AzureParameters azureParameters = (AzureParameters) source;
        builder.withAzureParameters(AzureParametersDto.builder()
                .withResourceGroupName(azureParameters.getResourceGroupName())
                .build());
    }
}

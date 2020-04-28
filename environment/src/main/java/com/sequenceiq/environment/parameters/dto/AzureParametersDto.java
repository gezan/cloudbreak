package com.sequenceiq.environment.parameters.dto;

public class AzureParametersDto {

    private final String resourceGroupName;

    private AzureParametersDto(Builder builder) {
        resourceGroupName = builder.resourceGroupName;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String resourceGroupName;

        private Builder() {
        }

        public Builder withResourceGroupName(String resourceGroupName) {
            this.resourceGroupName = resourceGroupName;
            return this;
        }

        public AzureParametersDto build() {
            return new AzureParametersDto(this);
        }
    }
}

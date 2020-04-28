package com.sequenceiq.environment.parameters.dao.domain;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("AZURE")
public class AzureParameters extends BaseParameters {

    @Column(name = "resource_group_name")
    private String resourceGroupName;

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }
}

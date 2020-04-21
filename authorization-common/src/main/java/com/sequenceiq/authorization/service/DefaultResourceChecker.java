package com.sequenceiq.authorization.service;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.RD_READ;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.READ;

import java.util.Collection;
import java.util.Set;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;

public interface DefaultResourceChecker {

    Set<AuthorizationResourceAction> ALLOWED_ACTIONS = Set.of(DESCRIBE, READ, RD_READ);

    AuthorizationResourceType getResourceType();

    boolean isDefault(String resourceCrn);

    default boolean isAllowedAction(AuthorizationResourceAction action) {
        return ALLOWED_ACTIONS.contains(action);
    }

    CrnsByCategory getDefaultResourceCrns(Collection<String> resourceCrns);
}

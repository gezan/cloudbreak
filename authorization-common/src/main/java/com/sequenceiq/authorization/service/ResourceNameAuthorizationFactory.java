package com.sequenceiq.authorization.service;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.model.AuthorizationRule;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;

@Component
public class ResourceNameAuthorizationFactory extends TypedAuthorizationFactory<CheckPermissionByResourceName> {

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Inject
    private ResourceCrnAthorizationFactory resourceCrnAthorizationFactory;

    @Override
    public Optional<AuthorizationRule> doGetAuthorization(CheckPermissionByResourceName methodAnnotation, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        AuthorizationResourceAction action = methodAnnotation.action();
        String resourceName = commonPermissionCheckingUtils.getParameter(proceedingJoinPoint, methodSignature, ResourceName.class, String.class);
        return calcAuthorization(resourceName, action);
    }

    public Optional<AuthorizationRule> calcAuthorization(String resourceName, AuthorizationResourceAction action) {
        ResourceBasedCrnProvider resourceBasedCrnProvider = commonPermissionCheckingUtils.getResourceBasedCrnProvider(action);
        String resourceCrn = resourceBasedCrnProvider.getResourceCrnByResourceName(resourceName);
        if (StringUtils.isEmpty(resourceCrn)) {
            throw new NotFoundException(String.format("Could not find resourceCrn for resource by name: %s", resourceName));
        }
        return resourceCrnAthorizationFactory.calcAuthorization(resourceCrn, action);
    }

    @Override
    public Class<CheckPermissionByResourceName> supportedAnnotation() {
        return CheckPermissionByResourceName.class;
    }
}

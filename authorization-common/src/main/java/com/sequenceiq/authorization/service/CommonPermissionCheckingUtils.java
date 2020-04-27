package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;

@Component
public class CommonPermissionCheckingUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonPermissionCheckingUtils.class);

    @Inject
    private UmsAccountAuthorizationService umsAccountAuthorizationService;

    @Inject
    private UmsResourceAuthorizationService umsResourceAuthorizationService;

    @Inject
    private List<ResourceBasedCrnProvider> resourceBasedCrnProviders;

    @Inject
    private UmsRightProvider umsRightProvider;

    private final Map<AuthorizationResourceType, ResourceBasedCrnProvider> resourceBasedCrnProviderMap = new HashMap<>();

    @PostConstruct
    public void populateResourceBasedCrnProviderMap() {
        resourceBasedCrnProviders.forEach(resourceBasedCrnProvider ->
                resourceBasedCrnProviderMap.put(resourceBasedCrnProvider.getResourceType(), resourceBasedCrnProvider));
    }

    public ResourceBasedCrnProvider getResourceBasedCrnProvider(AuthorizationResourceAction action) {
        AuthorizationResourceType resourceType = umsRightProvider.getResourceType(action);
        return resourceBasedCrnProviderMap.get(resourceType);
    }

    public void checkPermissionForUser(AuthorizationResourceAction action, String userCrn) {
        umsAccountAuthorizationService.checkRightOfUser(userCrn, action);
    }

    public void checkPermissionForUserOnResource(AuthorizationResourceAction action, String userCrn, String resourceCrn) {
        umsResourceAuthorizationService.checkRightOfUserOnResource(userCrn, action, resourceCrn);
    }

    public void checkPermissionForUserOnResources(AuthorizationResourceAction action, String userCrn, Collection<String> resourceCrns) {
        umsResourceAuthorizationService.checkRightOfUserOnResources(userCrn, action, resourceCrns);
    }

    public Map<String, Boolean> getPermissionsForUserOnResources(AuthorizationResourceAction action, String userCrn, List<String> resourceCrns) {
        return umsResourceAuthorizationService.getRightOfUserOnResources(userCrn, action, resourceCrns);
    }

    public Object proceed(ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature, long startTime) {
        LOGGER.debug("Permission check took {} ms", System.currentTimeMillis() - startTime);
        try {
            Object proceed = proceedingJoinPoint.proceed();
            if (proceed == null) {
                LOGGER.debug("Return value is null, method signature: {}", methodSignature.toLongString());
            }
            return proceed;
        } catch (Error | RuntimeException unchecked) {
            throw unchecked;
        } catch (Throwable t) {
            throw new AccessDeniedException(t.getMessage(), t);
        }
    }

    Optional<Annotation> getClassAnnotation(Class<?> repositoryClass) {
        return Arrays.stream(repositoryClass.getAnnotations())
                .filter(a -> a.annotationType().equals(AuthorizationResource.class))
                .findFirst();
    }

    public Optional<Class<?>> getAuthorizationClass(ProceedingJoinPoint proceedingJoinPoint) {
        return proceedingJoinPoint.getTarget().getClass().isAnnotationPresent(AuthorizationResource.class)
                ? Optional.of(proceedingJoinPoint.getTarget().getClass()) : Optional.empty();
    }

    public boolean isAuthorizationDisabled(ProceedingJoinPoint proceedingJoinPoint) {
        return proceedingJoinPoint.getTarget().getClass().isAnnotationPresent(DisableCheckPermissions.class);
    }

    public <T> T getParameter(ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature, Class annotation, Class<T> target) {
        List<Parameter> parameters = Lists.newArrayList(methodSignature.getMethod().getParameters());
        List<Parameter> matchingParameters = parameters.stream()
                .filter(parameter -> parameter.isAnnotationPresent(annotation))
                .collect(Collectors.toList());
        if (matchingParameters.size() != 1) {
            throw new IllegalStateException(String.format("Your controller method %s should have one and only one parameter with the annotation %s",
                    methodSignature.getMethod().getName(), annotation.getSimpleName()));
        }
        Object result = proceedingJoinPoint.getArgs()[parameters.indexOf(matchingParameters.iterator().next())];
        if (!target.isInstance(result)) {
            throw new IllegalStateException(
                    String.format("The type of the annotated parameter does not match with the expected type %s", target.getSimpleName()));
        }
        return (T) result;
    }

}

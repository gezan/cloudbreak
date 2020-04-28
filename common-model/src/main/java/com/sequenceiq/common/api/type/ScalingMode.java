package com.sequenceiq.common.api.type;

/**
 * Specifies whether an instance group supports scaling option.
 * Instance group with auto scaling mode is both auto scalable and manually scalable.
 * None means the instance group is not scalable.
 * If unspecified, the instance group can be scaled manually. This is the current behaviour for backward compatibility.
 * Unless specified none, the instance groups can be scaled manually.
 */
public enum ScalingMode {
    MANUAL, AUTO, NONE, UNSPECIFIED;

    public boolean isAutoscalable() {
        return this == AUTO;
    }

    public boolean isManuallyScalable() {
        return this != NONE;
    }
}

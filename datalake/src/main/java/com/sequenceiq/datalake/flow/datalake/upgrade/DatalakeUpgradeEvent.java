package com.sequenceiq.datalake.flow.datalake.upgrade;

import com.sequenceiq.flow.core.FlowEvent;

public enum DatalakeUpgradeEvent implements FlowEvent {

    DATALAKE_UPGRADE_EVENT("DATALAKE_UPGRADE_EVENT"),
    DATALAKE_UPGRADE_IN_PROGRESS_EVENT("DATALAKE_UPGRADE_IN_PROGRESS_EVENT"),
    DATALAKE_UPGRADE_COULD_NOT_START_EVENT("DatalakeUpgradeCouldNotStartEvent"),
    DATALAKE_IMAGE_CHANGE_EVENT("DatalakeImageChangeEvent"),
    DATALAKE_UPGRADE_SUCCESS_EVENT("DatalakeUpgradeSuccessEvent"),
    DATALAKE_UPGRADE_FAILED_EVENT("DatalakeUpgradeFailedEvent"),
    DATALAKE_UPGRADE_FAILED_HANDLED_EVENT("DATALAKE_UPGRADE_FAILED_HANDLED_EVENT"),
    DATALAKE_UPGRADE_FINALIZED_EVENT("DATALAKE_UPGRADE_FINALIZED_EVENT");

    private final String event;

    DatalakeUpgradeEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}

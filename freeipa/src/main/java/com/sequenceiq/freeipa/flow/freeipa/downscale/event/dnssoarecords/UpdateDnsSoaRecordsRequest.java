package com.sequenceiq.freeipa.flow.freeipa.downscale.event.dnssoarecords;

import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class UpdateDnsSoaRecordsRequest extends AbstractCleanupEvent {

    public UpdateDnsSoaRecordsRequest(CleanupEvent cleanupEvent) {
        super(cleanupEvent);
    }
}

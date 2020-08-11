package com.sequenceiq.cloudbreak.structuredevent.event.cdp;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sequenceiq.cloudbreak.structuredevent.event.CDPStructuredEventDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = CDPStructuredEventDeserializer.class)
public abstract class CDPStructuredEvent implements Serializable {

    public static final String TYPE_FIELD = "type";

    public static final String SENT = "SENT";

    public static final long ZERO = 0L;

    private String type;

    private CDPOperationDetails operation;

    public CDPStructuredEvent() {
    }

    public CDPStructuredEvent(String type) {
        this.type = type;
    }

    public CDPStructuredEvent(String type, CDPOperationDetails operation) {
        this.type = type;
        this.operation = operation;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setOperation(CDPOperationDetails operation) {
        this.operation = operation;
    }

    public CDPOperationDetails getOperation() {
        return operation;
    }

    public abstract String getStatus();

    public abstract Long getDuration();
}

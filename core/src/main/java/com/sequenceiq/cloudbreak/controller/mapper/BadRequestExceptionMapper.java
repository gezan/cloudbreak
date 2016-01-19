package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.controller.json.ExceptionResult;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Provider
public class BadRequestExceptionMapper implements ExceptionMapper<BadRequestException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BadRequestExceptionMapper.class);

    @Override
    public Response toResponse(BadRequestException exception) {
        MDCBuilder.buildMdcContext();
        LOGGER.error(exception.getMessage(), exception);
        return Response.status(Response.Status.BAD_REQUEST).entity(new ExceptionResult(exception.getMessage()))
                .build();
    }
}
package com.sequenceiq.it.cloudbreak.dto.mock.endpoint;

import java.lang.reflect.Proxy;

import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.Method;
import com.sequenceiq.it.cloudbreak.dto.mock.MockUriNameParser;
import com.sequenceiq.it.cloudbreak.dto.mock.MockUriParameters;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.DefaultResponseConfigure;

public class EndpointProxyFactory {

    private EndpointProxyFactory() {

    }

    public static <E extends VerificationEndpoint<T>, T extends CloudbreakTestDto> E create(Class<E> endpoint, T testDto, MockedTestContext testContext) {
        return (E) Proxy.newProxyInstance(
                testDto.getClass().getClassLoader(),
                new Class[]{endpoint},
                (proxy, method, args) -> {
                    Method httpMethod = Method.build(method.getName());
                    MockUriParameters parameters = new MockUriNameParser(endpoint, method).getParameters();
                    return new DefaultResponseConfigure(httpMethod, parameters.getUri(), testDto, testContext.getExecuteQueryToMockInfrastructure());

                });
    }
}

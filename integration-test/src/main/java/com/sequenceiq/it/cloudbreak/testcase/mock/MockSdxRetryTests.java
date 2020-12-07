package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class MockSdxRetryTests extends AbstractMockTest {

    @Inject
    private SdxTestClient sdxTestClient;

    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "start an sdx cluster",
            then = "failed first and retry"
    )
    public void testRetry(MockedTestContext testContext) {
        testContext
                .given(EnvironmentNetworkTestDto.class)
                .given(EnvironmentTestDto.class).withNetwork().withCreateFreeIpa(false)
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .withCatalog(getImageCatalogMockServerSetup()
                        .getFreeIpaImageCatalogUrl())
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .mockSpi().launch().post()
                .thenReturn("This is a bad request error", 400, 1)
                .awaitForFlow(key(resourcePropertyProvider().getName()))
                .when(sdxTestClient.retry())
                .awaitForFlow(key(resourcePropertyProvider().getName()))
                .await(SdxClusterStatusResponse.RUNNING)
                .validate();
    }
}

package com.sequenceiq.cloudbreak.cloud.aws;

import static com.amazonaws.services.cloudformation.model.Capability.CAPABILITY_IAM;
import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.DELETE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsResourceConstants.ERROR_STATUSES;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.OnFailure;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsResult;
import com.amazonaws.services.ec2.model.Vpc;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.DefaultNetworkConnector;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.AwsBackoffSyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetFilterStrategy;
import com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetFilterStrategyType;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetRequest;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

@Service
public class AwsNetworkConnector extends DefaultNetworkConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNetworkConnector.class);

    private static final String CREATED_VPC = "CreatedVpc";

    private static final int NOT_FOUND = 400;

    @Value("${cb.aws.subnet.ha.different.az.min:2}")
    private int minSubnetCountInDifferentAz;

    @Value("${cb.aws.subnet.ha123.different.az.max:3}")
    private int maxSubnetCountInDifferentAz;

    @Inject
    private AwsNetworkCfTemplateProvider awsNetworkCfTemplateProvider;

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private AwsClient awsClient;

    @Inject
    private AwsPollTaskFactory awsPollTaskFactory;

    @Inject
    private AwsBackoffSyncPollingScheduler<Boolean> awsBackoffSyncPollingScheduler;

    @Inject
    private AwsSubnetRequestProvider awsSubnetRequestProvider;

    @Inject
    private AwsCreatedSubnetProvider awsCreatedSubnetProvider;

    @Inject
    private AwsTaggingService awsTaggingService;

    @Inject
    private Map<SubnetFilterStrategyType, SubnetFilterStrategy> subnetFilterStrategyMap;

    @Override
    public CreatedCloudNetwork createNetworkWithSubnets(NetworkCreationRequest networkRequest) {
        AwsCredentialView credentialView = new AwsCredentialView(networkRequest.getCloudCredential());
        AmazonCloudFormationRetryClient cloudFormationRetryClient = getCloudFormationRetryClient(credentialView, networkRequest.getRegion().value());
        List<SubnetRequest> subnetRequests = getCloudSubNets(networkRequest);
        String cfStackName = networkRequest.getStackName();
        try {
            cloudFormationRetryClient.describeStacks(new DescribeStacksRequest().withStackName(cfStackName));
            LOGGER.warn("AWS CloudFormation stack for Network with stack name: '{}' already exists. Attaching this one to the network.", cfStackName);
            return getCreatedNetworkWithPolling(networkRequest, credentialView, cloudFormationRetryClient, subnetRequests);
        } catch (AmazonServiceException e) {
            if (networkDoesNotExist(e)) {
                LOGGER.warn("{} occurred during describe AWS CloudFormation stack for Network with stack name: '{}'. "
                        + "Assuming the CF Stack does not exist, so creating a new one. Exception message: {}", e.getClass(), cfStackName, e.getMessage());
                String cloudFormationTemplate = createTemplate(networkRequest, subnetRequests);
                return createNewCfNetworkStack(networkRequest, credentialView, cloudFormationRetryClient, cloudFormationTemplate, subnetRequests);
            } else {
                throw new CloudConnectorException("Failed to create network.", e);
            }
        }
    }

    @Override
    public String getNetworkCidr(Network network, CloudCredential credential) {
        AwsCredentialView awsCredentialView = new AwsCredentialView(credential);
        AmazonEC2Client awsClientAccess = awsClient.createAccess(awsCredentialView, network.getStringParameter(AwsNetworkView.REGION));
        AwsNetworkView awsNetworkView = new AwsNetworkView(network);
        String existingVpc = awsNetworkView.getExistingVpc();
        DescribeVpcsResult describeVpcsResult = awsClientAccess.describeVpcs(new DescribeVpcsRequest().withVpcIds(existingVpc));
        List<String> vpcCidrs = describeVpcsResult.getVpcs().stream().map(Vpc::getCidrBlock).collect(Collectors.toList());
        if (vpcCidrs.isEmpty()) {
            throw new BadRequestException("VPC cidr could not fetch from AWS: " + existingVpc);
        }
        if (vpcCidrs.size() > 1) {
            LOGGER.info("More than one vpc cidrs for VPC {}. We will use the first one: {}", existingVpc, vpcCidrs.get(0));
        }
        return vpcCidrs.get(0);
    }

    @Override
    public SubnetSelectionResult filterSubnets(Collection<CloudSubnet> subnetMetas, SubnetSelectionParameters subnetSelectionParameters) {
        boolean preferPrivate = subnetSelectionParameters.isPreferPrivateIfExist() || subnetSelectionParameters.getTunnel().useCcm();

        SubnetFilterStrategyType subnetSelectorStrategyType = preferPrivate ?
                SubnetFilterStrategyType.MULTIPLE_PREFER_PRIVATE : SubnetFilterStrategyType.MULTIPLE_PREFER_PUBLIC;
        int azCount = subnetSelectionParameters.isHa() ? subnetCountInDifferentAzMin() : 1;
        return subnetFilterStrategyMap.get(subnetSelectorStrategyType).filter(subnetMetas, azCount);
    }

    @Override
    public int subnetCountInDifferentAzMin() {
        return minSubnetCountInDifferentAz;
    }

    @Override
    public int subnetCountInDifferentAzMax() {
        return maxSubnetCountInDifferentAz;
    }

    private boolean networkDoesNotExist(AmazonServiceException e) {
        return e.getStatusCode() == NOT_FOUND && e.getErrorMessage().contains("does not exist");
    }

    private List<SubnetRequest> getCloudSubNets(NetworkCreationRequest networkRequest) {
        AwsCredentialView awsCredential = new AwsCredentialView(networkRequest.getCloudCredential());
        AmazonEC2Client awsClientAccess = awsClient.createAccess(awsCredential, networkRequest.getRegion().value());
        return awsSubnetRequestProvider.provide(
                awsClientAccess,
                new ArrayList<>(networkRequest.getPublicSubnets()),
                new ArrayList<>(networkRequest.getPrivateSubnets()));
    }

    private AmazonCloudFormationRetryClient getCloudFormationRetryClient(AwsCredentialView credentialView, String region) {
        return awsClient.createCloudFormationRetryClient(credentialView, region);
    }

    private String createTemplate(NetworkCreationRequest networkRequest, List<SubnetRequest> subnetRequestList) {
        return awsNetworkCfTemplateProvider.provide(networkRequest.getEnvName(), networkRequest.getEnvId(),
                networkRequest.getNetworkCidr(), subnetRequestList, privateSubnetEnabled(networkRequest, subnetRequestList));
    }

    private boolean privateSubnetEnabled(NetworkCreationRequest networkRequest, List<SubnetRequest> subnetRequestList) {
        return subnetRequestList
                .stream()
                .filter(subnetRequest -> !Strings.isNullOrEmpty(subnetRequest.getPrivateSubnetCidr()))
                .findFirst()
                .isPresent() && networkRequest.isPrivateSubnetEnabled();
    }

    private CreatedCloudNetwork createNewCfNetworkStack(
            NetworkCreationRequest networkRequest,
            AwsCredentialView credentialView,
            AmazonCloudFormationRetryClient cloudFormationRetryClient,
            String cloudFormationTemplate, List<SubnetRequest> subnetRequests) {

        cloudFormationRetryClient.createStack(createStackRequest(networkRequest.getStackName(),
                cloudFormationTemplate,
                networkRequest.getTags(),
                networkRequest.getCreatorCrn()));
        LOGGER.debug("CloudFormation stack creation request sent with stack name: '{}' ", networkRequest.getStackName());
        return getCreatedNetworkWithPolling(networkRequest, credentialView, cloudFormationRetryClient, subnetRequests);
    }

    private CreatedCloudNetwork getCreatedNetworkWithPolling(NetworkCreationRequest networkRequest, AwsCredentialView credentialView,
            AmazonCloudFormationRetryClient cloudFormationRetryClient, List<SubnetRequest> subnetRequests) {
        PollTask<Boolean> pollTask = getNewNetworkPollTask(credentialView, networkRequest);
        try {
            awsBackoffSyncPollingScheduler.schedule(pollTask);
        } catch (RuntimeException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }
        return getCreatedCloudNetwork(cloudFormationRetryClient, networkRequest, subnetRequests);
    }

    private CreatedCloudNetwork getCreatedCloudNetwork(AmazonCloudFormationRetryClient cloudFormationRetryClient, NetworkCreationRequest networkRequest,
            List<SubnetRequest> subnetRequests) {
        Map<String, String> output = cfStackUtil.getOutputs(networkRequest.getStackName(), cloudFormationRetryClient);
        String vpcId = getCreatedVpc(output);
        Set<CreatedSubnet> subnets = awsCreatedSubnetProvider.provide(output, subnetRequests);
        return new CreatedCloudNetwork(networkRequest.getStackName(), vpcId, subnets);
    }

    private CreateStackRequest createStackRequest(String stackName, String cloudFormationTemplate, Map<String, String> tags, String creatorUser) {
        Collection<Tag> awsTags = awsTaggingService.prepareCloudformationTags(null, tags);
        return new CreateStackRequest()
                .withStackName(stackName)
                .withOnFailure(OnFailure.DO_NOTHING)
                .withTemplateBody(cloudFormationTemplate)
                .withTags(awsTags)
                .withCapabilities(CAPABILITY_IAM)
                .withParameters(getStackParameters(creatorUser));
    }

    private Collection<Parameter> getStackParameters(String creatorUser) {
        Collection<Parameter> parameters = new ArrayList<>();
        parameters.addAll(asList(
                getStackOwner("StackOwner", creatorUser),
                getStackOwner("stackowner", creatorUser)
        ));
        return parameters;
    }

    private Parameter getStackOwner(String referenceName, String referenceValue) {
        return new Parameter()
                .withParameterKey(referenceName)
                .withParameterValue(referenceValue);
    }

    private PollTask<Boolean> getNewNetworkPollTask(AwsCredentialView credentialView, NetworkCreationRequest networkRequest) {
        AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, networkRequest.getRegion().value());
        return awsPollTaskFactory
                .newAwsCreateNetworkStatusCheckerTask(cfClient, CREATE_COMPLETE, CREATE_FAILED, ERROR_STATUSES, networkRequest);
    }

    private PollTask<Boolean> getDeleteNetworkPollTask(AwsCredentialView credentialView, String region, String stackName) {
        AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, region);
        return awsPollTaskFactory.newAwsTerminateNetworkStatusCheckerTask(cfClient, DELETE_COMPLETE, DELETE_FAILED, ERROR_STATUSES, stackName);
    }

    private String getCreatedVpc(Map<String, String> output) {
        if (output.containsKey(CREATED_VPC)) {
            return output.get(CREATED_VPC);
        } else {
            String outputKeyNotFound = "Vpc could not be found in the CloudFormation stack output.";
            throw new CloudConnectorException(outputKeyNotFound);
        }
    }

    @Override
    public void deleteNetworkWithSubnets(NetworkDeletionRequest networkDeletionRequest) {
        if (!networkDeletionRequest.isExisting()) {
            AwsCredentialView credentialView = new AwsCredentialView(networkDeletionRequest.getCloudCredential());
            AmazonCloudFormationRetryClient cloudFormationRetryClient = getCloudFormationRetryClient(credentialView, networkDeletionRequest.getRegion());
            DeleteStackRequest deleteStackRequest = new DeleteStackRequest();
            deleteStackRequest.setStackName(networkDeletionRequest.getStackName());
            cloudFormationRetryClient.deleteStack(deleteStackRequest);

            LOGGER.debug("CloudFormation stack deletion request sent with stack name: '{}' ", networkDeletionRequest.getStackName());
            PollTask<Boolean> pollTask = getDeleteNetworkPollTask(credentialView, networkDeletionRequest.getRegion(), networkDeletionRequest.getStackName());
            try {
                awsBackoffSyncPollingScheduler.schedule(pollTask);
            } catch (RuntimeException | InterruptedException | ExecutionException | TimeoutException e) {
                throw new CloudConnectorException(e.getMessage(), e);
            }
        }
    }

    @Override
    public Platform platform() {
        return AwsConstants.AWS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return AwsConstants.AWS_VARIANT;
    }

}

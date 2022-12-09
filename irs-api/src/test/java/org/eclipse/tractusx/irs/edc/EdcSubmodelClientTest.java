/********************************************************************************
 * Copyright (c) 2021,2022
 *       2022: Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *       2022: ZF Friedrichshafen AG
 *       2022: ISTOS GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0. *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/
package org.eclipse.tractusx.irs.edc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.github.jknack.handlebars.internal.Files;
import io.github.resilience4j.retry.RetryRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.irs.component.GlobalAssetIdentification;
import org.eclipse.tractusx.irs.component.LinkedItem;
import org.eclipse.tractusx.irs.component.Relationship;
import org.eclipse.tractusx.irs.configuration.EdcConfiguration;
import org.eclipse.tractusx.irs.edc.model.NegotiationResponse;
import org.eclipse.tractusx.irs.exceptions.ContractNegotiationException;
import org.eclipse.tractusx.irs.exceptions.TimeoutException;
import org.eclipse.tractusx.irs.services.AsyncPollingService;
import org.eclipse.tractusx.irs.services.OutboundMeterRegistryService;
import org.eclipse.tractusx.irs.util.JsonUtil;
import org.eclipse.tractusx.irs.util.LocalTestDataConfigurationAware;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EdcSubmodelClientTest extends LocalTestDataConfigurationAware {

    private static final String ENDPOINT_ADDRESS = "http://localhost/urn:123456/submodel";

    @Mock
    private ContractNegotiationService contractNegotiationService;
    @Mock
    private EdcDataPlaneClient edcDataPlaneClient;
    private final EndpointDataReferenceStorage endpointDataReferenceStorage = new EndpointDataReferenceStorage();
    private final JsonUtil jsonUtil = new JsonUtil();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final TimeMachine clock = new TimeMachine();

    private final AsyncPollingService pollingService = new AsyncPollingService(clock, scheduler);

    @Spy
    private final EdcConfiguration config = new EdcConfiguration();

    private EdcSubmodelClient testee;

    @Mock
    private OutboundMeterRegistryService meterRegistry;
    private final RetryRegistry retryRegistry = RetryRegistry.ofDefaults();

    EdcSubmodelClientTest() throws IOException {
        super();
    }

    @BeforeEach
    void setUp() {
        config.setControlplane(new EdcConfiguration.ControlplaneConfig());
        config.getControlplane().setEndpoint(new EdcConfiguration.ControlplaneConfig.EndpointConfig());
        config.getControlplane().getEndpoint().setData("https://irs-consumer-controlplane.dev.demo.catena-x.net/data");
        config.getControlplane().setRequestTtl(Duration.ofMinutes(10));

        config.setSubmodel(new EdcConfiguration.SubmodelConfig());
        config.getSubmodel().setPath("/submodel");
        config.getSubmodel().setUrnPrefix("/urn");
        config.getSubmodel().setRequestTtl(Duration.ofMinutes(10));
        testee = new EdcSubmodelClientImpl(config, contractNegotiationService, edcDataPlaneClient,
                endpointDataReferenceStorage, jsonUtil, pollingService, meterRegistry, retryRegistry);
    }

    @Test
    void shouldRetrieveValidRelationship() throws Exception {
        // arrange
        when(contractNegotiationService.negotiate(any(), any())).thenReturn(
                NegotiationResponse.builder().contractAgreementId("agreementId").build());
        final EndpointDataReference ref = mock(EndpointDataReference.class);
        endpointDataReferenceStorage.put("agreementId", ref);
        final String assemblyPartRelationshipJson = readAssemblyPartRelationshipData();
        when(edcDataPlaneClient.getData(eq(ref), any())).thenReturn(assemblyPartRelationshipJson);

        // act
        final var result = testee.getRelationships(ENDPOINT_ADDRESS, RelationshipAspect.AssemblyPartRelationship);
        final List<Relationship> resultingRelationships = result.get(5, TimeUnit.SECONDS);

        // assert
        final List<Relationship> expectedRelationships = jsonUtil.fromString(assemblyPartRelationshipJson,
                RelationshipAspect.AssemblyPartRelationship.getSubmodelClazz()).asRelationships();
        assertThat(resultingRelationships).isNotNull().containsAll(expectedRelationships);
    }

    @Test
    void shouldTimeOut() throws Exception {
        // arrange
        when(contractNegotiationService.negotiate(any(), any())).thenReturn(
                NegotiationResponse.builder().contractAgreementId("agreementId").build());

        // act
        final var result = testee.getRelationships(ENDPOINT_ADDRESS, RelationshipAspect.AssemblyPartRelationship);
        clock.travelToFuture(Duration.ofMinutes(20));

        // assert
        assertThatThrownBy(result::get).isInstanceOf(ExecutionException.class)
                                       .hasCauseInstanceOf(TimeoutException.class);
    }

    @NotNull
    private String readAssemblyPartRelationshipData() throws IOException {
        final InputStream resourceAsStream = getClass().getResourceAsStream("/__files/assemblyPartRelationship.json");
        Objects.requireNonNull(resourceAsStream);
        return Files.read(resourceAsStream, StandardCharsets.UTF_8);
    }

    @Test
    void shouldReturnRelationshipsWhenRequestingWithCatenaXIdAndAssemblyPartRelationship() throws Exception {
        final String existingCatenaXId = "urn:uuid:15090ed9-0b5c-4761-ad71-c085cf84fa63";
        prepareTestdata(existingCatenaXId, "_assemblyPartRelationship");

        final List<Relationship> submodelResponse = testee.getRelationships("http://localhost/" + existingCatenaXId + "/submodel",
                RelationshipAspect.AssemblyPartRelationship).get(5, TimeUnit.SECONDS);

        assertThat(submodelResponse).isNotEmpty();
        assertThat(submodelResponse.get(0).getCatenaXId().getGlobalAssetId()).isEqualTo(existingCatenaXId);
        assertThat(submodelResponse).hasSize(32);
    }

    @Test
    void shouldReturnRelationshipsWhenRequestingWithCatenaXIdAndSingleLevelBomAsPlanned() throws Exception {
        final String catenaXId = "urn:uuid:aad27ddb-43aa-4e42-98c2-01e529ef127c";
        prepareTestdata(catenaXId, "_singleLevelBomAsPlanned");

        final List<Relationship> submodelResponse = testee.getRelationships("http://localhost/" + catenaXId + "/submodel",
                RelationshipAspect.SingleLevelBomAsPlanned).get(5, TimeUnit.SECONDS);

        assertThat(submodelResponse.get(0).getCatenaXId().getGlobalAssetId()).isEqualTo(catenaXId);
        assertThat(submodelResponse).hasSize(1);

        final List<String> childIds = submodelResponse.stream()
                                                      .map(Relationship::getLinkedItem)
                                                      .map(LinkedItem::getChildCatenaXId)
                                                      .map(GlobalAssetIdentification::getGlobalAssetId)
                                                      .collect(Collectors.toList());
        assertThat(childIds).containsAnyOf("urn:uuid:e5c96ab5-896a-482c-8761-efd74777ca97");
    }

    @Test
    void shouldReturnEmptyRelationshipsWhenRequestingWithCatenaXIdAndSingleLevelUsageAsBuilt() throws Exception {
        final String catenaXId = "urn:uuid:aad27ddb-43aa-4e42-98c2-01e529ef127c";
        prepareTestdata(catenaXId, "_singleLevelUsageAsBuilt");

        final List<Relationship> submodelResponse = testee.getRelationships("http://localhost/" + catenaXId + "/submodel",
                RelationshipAspect.SingleLevelUsageAsBuilt).get(5, TimeUnit.SECONDS);

        assertThat(submodelResponse).isEmpty();
    }

    @Test
    void shouldReturnEmptyRelationshipsWhenRequestingWithNotExistingCatenaXIdAndAssemblyPartRelationship()
            throws Exception {
        final String catenaXId = "urn:uuid:8a61c8db-561e-4db0-84ec-a693fc5ffdf6";
        prepareTestdata(catenaXId, "_assemblyPartRelationship");

        final List<Relationship> submodelResponse = testee.getRelationships("http://localhost/" + catenaXId + "/submodel",
                RelationshipAspect.AssemblyPartRelationship).get(5, TimeUnit.SECONDS);

        assertThat(submodelResponse).isEmpty();
    }

    @Test
    void shouldReturnRawSerialPartTypizationWhenExisting() throws Exception {
        final String existingCatenaXId = "urn:uuid:4132cd2b-cbe7-4881-a6b4-39fdc31cca2b";
        prepareTestdata(existingCatenaXId, "_serialPartTypization");

        final String submodelResponse = testee.getSubmodelRawPayload("http://localhost/" + existingCatenaXId + "/submodel")
                                              .get(5, TimeUnit.SECONDS);

        assertThat(submodelResponse).startsWith(
                "{\"localIdentifiers\":[{\"value\":\"BPNL00000003AYRE\",\"key\":\"manufacturerId\"}");
    }

    private void prepareTestdata(final String catenaXId, final String submodelDataSuffix)
            throws ContractNegotiationException, IOException {
        when(contractNegotiationService.negotiate(any(), any())).thenReturn(
                NegotiationResponse.builder().contractAgreementId("agreementId").build());
        final EndpointDataReference ref = mock(EndpointDataReference.class);
        endpointDataReferenceStorage.put("agreementId", ref);
        final SubmodelTestdataCreator submodelTestdataCreator = new SubmodelTestdataCreator(
                localTestDataConfiguration.cxTestDataContainer());
        final String data = jsonUtil.asString(
                submodelTestdataCreator.createSubmodelForId(catenaXId + submodelDataSuffix));
        when(edcDataPlaneClient.getData(eq(ref), any())).thenReturn(data);
    }
}

class TimeMachine extends Clock {

    private Instant currentTime = Instant.now();

    @Override
    public ZoneId getZone() {
        return ZoneId.systemDefault();
    }

    @Override
    public Clock withZone(final ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return currentTime;
    }

    public void travelToFuture(Duration timeToAdd) {
        currentTime = currentTime.plus(timeToAdd);
    }
}
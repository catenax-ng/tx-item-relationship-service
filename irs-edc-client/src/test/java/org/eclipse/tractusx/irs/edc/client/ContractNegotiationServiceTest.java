/********************************************************************************
 * Copyright (c) 2021,2022,2023
 *       2022: ZF Friedrichshafen AG
 *       2022: ISTOS GmbH
 *       2022,2023: Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *       2022,2023: BOSCH AG
 * Copyright (c) 2021,2022,2023 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.irs.edc.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.tractusx.irs.edc.client.exceptions.ContractNegotiationException;
import org.eclipse.tractusx.irs.edc.client.exceptions.EdcClientException;
import org.eclipse.tractusx.irs.edc.client.exceptions.UsagePolicyException;
import org.eclipse.tractusx.irs.edc.client.model.NegotiationId;
import org.eclipse.tractusx.irs.edc.client.model.NegotiationResponse;
import org.eclipse.tractusx.irs.edc.client.model.TransferProcessId;
import org.eclipse.tractusx.irs.edc.client.model.TransferProcessResponse;
import org.eclipse.tractusx.irs.edc.client.model.CatalogItem;
import org.eclipse.tractusx.irs.edc.client.policy.PolicyCheckerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContractNegotiationServiceTest {

    private static final String CONNECTOR_URL = "dummyConnectorUrl";

    @InjectMocks
    private ContractNegotiationService testee;
    @Mock
    private EdcControlPlaneClient edcControlPlaneClient;
    @Spy
    private EdcConfiguration config = new EdcConfiguration();
    @Mock
    private InMemoryCatalogCache catalogCache;

    @Mock
    private PolicyCheckerService policyCheckerService;

    @Test
    void shouldNegotiateSuccessfully() throws ContractNegotiationException, UsagePolicyException {
        // arrange
        final var assetId = "testTarget";
        final CatalogItem catalogItem = CatalogItem.builder().itemId(assetId).assetPropId(assetId).build();
        when(catalogCache.getCatalogItem(CONNECTOR_URL, assetId)).thenReturn(Optional.ofNullable(catalogItem));
        when(policyCheckerService.isValid(any())).thenReturn(Boolean.TRUE);
        when(edcControlPlaneClient.startNegotiations(any())).thenReturn(
                NegotiationId.builder().value("negotiationId").build());
        CompletableFuture<NegotiationResponse> response = CompletableFuture.completedFuture(
                NegotiationResponse.builder().contractAgreementId("agreementId").build());
        when(edcControlPlaneClient.getNegotiationResult(any())).thenReturn(response);
        when(edcControlPlaneClient.startTransferProcess(any())).thenReturn(
                TransferProcessId.builder().value("transferProcessId").build());
        when(edcControlPlaneClient.getTransferProcess(any())).thenReturn(
                CompletableFuture.completedFuture(TransferProcessResponse.builder().build()));

        // act
        NegotiationResponse result = testee.negotiate(CONNECTOR_URL, assetId);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getContractAgreementId()).isEqualTo("agreementId");
    }

    @Test
    void shouldThrowErrorWhenRetrievingNegotiationResult() {
        // arrange
        final var assetId = "testTarget";
        final CatalogItem catalogItem = CatalogItem.builder().itemId(assetId).assetPropId(assetId).build();
        when(catalogCache.getCatalogItem(CONNECTOR_URL, assetId)).thenReturn(Optional.ofNullable(catalogItem));
        when(policyCheckerService.isValid(any())).thenReturn(Boolean.TRUE);
        when(edcControlPlaneClient.startNegotiations(any())).thenReturn(
                NegotiationId.builder().value("negotiationId").build());
        CompletableFuture<NegotiationResponse> response = CompletableFuture.failedFuture(
                new RuntimeException("Test exception"));
        when(edcControlPlaneClient.getNegotiationResult(any())).thenReturn(response);

        // act & assert
        assertThatThrownBy(() -> testee.negotiate(CONNECTOR_URL, assetId)).isInstanceOf(EdcClientException.class);
    }

    @Test
    void shouldThrowErrorWhenContractOfferCouldNotBeFound() {
        // arrange
        final var assetId = "testTarget";
        when(catalogCache.getCatalogItem(CONNECTOR_URL, assetId)).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> testee.negotiate(CONNECTOR_URL, assetId)).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void shouldThrowErrorWhenPolicyCheckerReturnFalse() {
        // arrange
        final var assetId = "testTarget";
        final CatalogItem catalogItem = CatalogItem.builder().itemId(assetId).assetPropId(assetId).build();
        when(catalogCache.getCatalogItem(CONNECTOR_URL, assetId)).thenReturn(Optional.ofNullable(catalogItem));
        when(policyCheckerService.isValid(any())).thenReturn(Boolean.FALSE);

        // act & assert
        assertThatThrownBy(() -> testee.negotiate(CONNECTOR_URL, assetId)).isInstanceOf(EdcClientException.class);
    }

}
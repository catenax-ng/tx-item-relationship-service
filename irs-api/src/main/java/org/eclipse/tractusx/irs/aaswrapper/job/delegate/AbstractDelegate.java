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
package org.eclipse.tractusx.irs.aaswrapper.job.delegate;

import static org.eclipse.tractusx.irs.aaswrapper.job.ExtractDataFromProtocolInformation.extractAssetId;
import static org.eclipse.tractusx.irs.aaswrapper.job.ExtractDataFromProtocolInformation.extractSuffix;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.irs.aaswrapper.job.AASTransferProcess;
import org.eclipse.tractusx.irs.aaswrapper.job.ItemContainer;
import org.eclipse.tractusx.irs.component.JobParameter;
import org.eclipse.tractusx.irs.component.assetadministrationshell.Endpoint;
import org.eclipse.tractusx.irs.edc.client.EdcSubmodelFacade;
import org.eclipse.tractusx.irs.edc.client.ItemNotFoundInCatalogException;
import org.eclipse.tractusx.irs.edc.client.exceptions.EdcClientException;
import org.eclipse.tractusx.irs.registryclient.discovery.ConnectorEndpointsService;

/**
 * Abstract base class to process Shells, Submodels, Bpns and store them inside {@link ItemContainer}
 */
@RequiredArgsConstructor
@Slf4j
public abstract class AbstractDelegate {

    protected final AbstractDelegate nextStep;

    protected final int retryCount = RetryRegistry.ofDefaults().getDefaultConfig().getMaxAttempts();

    /**
     * @param itemContainerBuilder Collecting data from delegates
     * @param jobData              The job parameters used for filtering
     * @param aasTransferProcess   The transfer process which will be filled with childIds
     *                             for further processing
     * @param itemId               The id of the current item
     * @return The ItemContainer filled with Relationships, Shells, Submodels (if requested in jobData)
     * and Tombstones (if requests fail).
     */
    public abstract ItemContainer process(ItemContainer.ItemContainerBuilder itemContainerBuilder, JobParameter jobData,
            AASTransferProcess aasTransferProcess, String itemId);

    /**
     * Delegates processing to next step if exists or returns filled {@link ItemContainer}
     *
     * @param itemContainerBuilder Collecting data from delegates
     * @param jobData              The job parameters used for filtering
     * @param aasTransferProcess   The transfer process which will be filled with childIds
     *                             for further processing
     * @param itemId               The id of the current item
     * @return item container with filled data
     */
    protected ItemContainer next(final ItemContainer.ItemContainerBuilder itemContainerBuilder,
            final JobParameter jobData, final AASTransferProcess aasTransferProcess, final String itemId) {
        if (this.nextStep != null) {
            return this.nextStep.process(itemContainerBuilder, jobData, aasTransferProcess, itemId);
        }

        return itemContainerBuilder.build();
    }

    protected String requestSubmodelAsString(final EdcSubmodelFacade submodelFacade,
            final ConnectorEndpointsService connectorEndpointsService, final Endpoint endpoint, final String bpn)
            throws EdcClientException {
        final List<String> connectorEndpoints = connectorEndpointsService.fetchConnectorEndpoints(bpn);
        final ArrayList<String> submodelPayload = new ArrayList<>();
        for (final String connectorEndpoint : connectorEndpoints) {
            addSubmodelToList(submodelFacade, endpoint, submodelPayload, connectorEndpoint);
        }
        return submodelPayload.stream().findFirst().orElseThrow();
    }

    private void addSubmodelToList(final EdcSubmodelFacade submodelFacade, final Endpoint endpoint,
            final List<String> submodelPayload, final String connectorEndpoint) throws EdcClientException {
        try {
            submodelPayload.add(submodelFacade.getSubmodelRawPayload(connectorEndpoint,
                    extractSuffix(endpoint.getProtocolInformation().getHref()),
                    extractAssetId(endpoint.getProtocolInformation().getSubprotocolBody())));
        } catch (URISyntaxException e) {
            throw new EdcClientException(e);
        } catch (ItemNotFoundInCatalogException e) {
            log.info("Could not find asset in catalog. Requesting next endpoint.", e);
        }
    }

}
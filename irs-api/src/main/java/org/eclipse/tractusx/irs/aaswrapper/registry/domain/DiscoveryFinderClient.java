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
package org.eclipse.tractusx.irs.aaswrapper.registry.domain;

import static org.eclipse.tractusx.irs.configuration.RestTemplateConfig.DTR_REST_TEMPLATE;

import java.util.List;

import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 *
 */
public interface DiscoveryFinderClient {

    DiscoveryResponse findDiscoveryEndpoints(DiscoveryFinderRequest request);

    List<EdcDiscoveryResult> findConnectorEndpoints(String endpointAddress, List<String> bpns);

}

/**
 *
 */
@Service
class DiscoveryFinderClientImpl implements DiscoveryFinderClient {

    private final String discoveryFinderUrl;

    private final RestTemplate restTemplate;

    /* package */ DiscoveryFinderClientImpl(
            @Value("${digitalTwinRegistry.discoveryFinderUrl:}") final String discoveryFinderUrl,
            @Qualifier(DTR_REST_TEMPLATE) final RestTemplate restTemplate) {
        this.discoveryFinderUrl = discoveryFinderUrl;
        this.restTemplate = restTemplate;
    }

    @Override
    @Retry(name = "registry")
    public DiscoveryResponse findDiscoveryEndpoints(final DiscoveryFinderRequest request) {
        return restTemplate.postForObject(discoveryFinderUrl, request, DiscoveryResponse.class);
    }

    @Override
    @Retry(name = "registry")
    public List<EdcDiscoveryResult> findConnectorEndpoints(final String endpointAddress, final List<String> bpns) {
        final EdcDiscoveryResult[] edcDiscoveryResults = restTemplate.postForObject(endpointAddress, bpns,
                EdcDiscoveryResult[].class);

        return edcDiscoveryResults == null ? List.of() : List.of(edcDiscoveryResults);
    }
}

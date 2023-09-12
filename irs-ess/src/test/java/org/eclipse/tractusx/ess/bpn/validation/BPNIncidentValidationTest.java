/********************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/
package org.eclipse.tractusx.ess.bpn.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.tractusx.ess.service.SupplyChainImpacted;
import org.eclipse.tractusx.irs.component.GlobalAssetIdentification;
import org.eclipse.tractusx.irs.component.Job;
import org.eclipse.tractusx.irs.component.Jobs;
import org.eclipse.tractusx.irs.component.assetadministrationshell.AssetAdministrationShellDescriptor;
import org.eclipse.tractusx.irs.component.assetadministrationshell.IdentifierKeyValuePair;
import org.eclipse.tractusx.irs.component.assetadministrationshell.Reference;
import org.junit.jupiter.api.Test;

class BPNIncidentValidationTest {

    private static Jobs jobResult(final String parentId, final Map<String, String> cxIdBPNMap) {
        final List<AssetAdministrationShellDescriptor> shells = new ArrayList<>();
        cxIdBPNMap.forEach((s, s2) -> shells.add(createShell(s, s2)));
        final GlobalAssetIdentification globalAssetId = GlobalAssetIdentification.of(parentId);
        final Job job = Job.builder().globalAssetId(globalAssetId).build();
        return Jobs.builder().job(job).shells(shells).build();
    }

    private static AssetAdministrationShellDescriptor createShell(final String catenaXId, final String bpn) {
        return AssetAdministrationShellDescriptor.builder()
                                                 .globalAssetId(catenaXId)
                                                 .specificAssetIds(List.of(IdentifierKeyValuePair.builder()
                                                                                                 .name("manufacturerId")
                                                                                                 .value(bpn)
                                                                                                 .build()))
                                                 .build();
    }

    @Test
    void shouldReturnNoWhenBPNsDoNotContainShellBPNs() {
        // Arrange
        final List<String> bpns = List.of("BPNL000000XYZ123");
        String parentId = "urn:uuid:0733946c-59c6-41ae-9570-cb43a6e4c79e";
        String parentBPN = "BPNL00000003AYRE";
        final Map<String, String> cxIdBPNMap = Map.of(parentId, parentBPN,
                "urn:uuid:aad27ddb-43aa-4e42-98c2-01e529ef127c", "BPNL00000003AYRE",
                "urn:uuid:e5c96ab5-896a-482c-8761-efd74777ca97", "BPNL00000003AYRE",
                "urn:uuid:c7a2b803-f8fe-4b79-b6fc-967ce847c9a9", "BPNL00000003B3NX",
                "urn:uuid:4f7b1cf2-a598-4027-bc78-63f6d8e55699", "BPNL00000003B0Q0",
                "urn:uuid:68904173-ad59-4a77-8412-3e73fcafbd8b", "BPNL00000003AVTH");
        final Jobs jobs = jobResult(parentId, cxIdBPNMap);

        // Act
        final SupplyChainImpacted actual = BPNIncidentValidation.jobContainsIncidentBPNs(jobs.getShells(), bpns);

        // Assert
        assertThat(actual).isEqualTo(SupplyChainImpacted.NO);
    }

    @Test
    void shouldReturnYesWhenBPNsContainShellBPNs() {
        // Arrange
        final List<String> bpns = List.of("BPNL00000003B0Q0");
        String parentId = "urn:uuid:0733946c-59c6-41ae-9570-cb43a6e4c79e";
        String parentBPN = "BPNL00000003AYRE";
        final Map<String, String> cxIdBPNMap = Map.of(parentId, parentBPN,
                "urn:uuid:aad27ddb-43aa-4e42-98c2-01e529ef127c", "BPNL00000003AYRE",
                "urn:uuid:e5c96ab5-896a-482c-8761-efd74777ca97", "BPNL00000003AYRE",
                "urn:uuid:c7a2b803-f8fe-4b79-b6fc-967ce847c9a9", "BPNL00000003B3NX",
                "urn:uuid:4f7b1cf2-a598-4027-bc78-63f6d8e55699", "BPNL00000003B0Q0",
                "urn:uuid:68904173-ad59-4a77-8412-3e73fcafbd8b", "BPNL00000003AVTH");
        final Jobs jobs = jobResult(parentId, cxIdBPNMap);

        // Act
        final SupplyChainImpacted actual = BPNIncidentValidation.jobContainsIncidentBPNs(jobs.getShells(), bpns);

        // Assert
        assertThat(actual).isEqualTo(SupplyChainImpacted.YES);
    }

    @Test
    void shouldReturnYesWhenNoChildrenAndParentContainsBPN() {
        // Arrange
        final List<String> bpns = List.of("BPNL00000003AYRE");
        String parentId = "urn:uuid:0733946c-59c6-41ae-9570-cb43a6e4c79e";
        String parentBPN = "BPNL00000003AYRE";
        final Map<String, String> cxIdBPNMap = Map.of(parentId, parentBPN);
        final Jobs jobs = jobResult(parentId, cxIdBPNMap);

        // Act
        final SupplyChainImpacted actual = BPNIncidentValidation.jobContainsIncidentBPNs(jobs.getShells(), bpns);

        // Assert
        assertThat(actual).isEqualTo(SupplyChainImpacted.YES);
    }

    @Test
    void shouldReturnNoWhenNoChildrenAndParentDoesNotContainBPN() {
        // Arrange
        final List<String> bpns = List.of("BPNL00000003B0Q0");
        String parentId = "urn:uuid:0733946c-59c6-41ae-9570-cb43a6e4c79e";
        String parentBPN = "BPNL00000003AYRE";
        final Map<String, String> cxIdBPNMap = Map.of(parentId, parentBPN);
        final Jobs jobs = jobResult(parentId, cxIdBPNMap);

        // Act
        final SupplyChainImpacted actual = BPNIncidentValidation.jobContainsIncidentBPNs(jobs.getShells(), bpns);

        // Assert
        assertThat(actual).isEqualTo(SupplyChainImpacted.NO);
    }

    @Test
    void shouldReturnUnknownWhenJobContainsNoShells() {
        // Arrange
        final List<String> bpns = List.of("BPNL00000003B0Q0");
        String parentId = "urn:uuid:0733946c-59c6-41ae-9570-cb43a6e4c79e";
        final Map<String, String> cxIdBPNMap = Map.of();
        final Jobs jobs = jobResult(parentId, cxIdBPNMap);

        // Act
        final SupplyChainImpacted actual = BPNIncidentValidation.jobContainsIncidentBPNs(jobs.getShells(), bpns);

        // Assert
        assertThat(actual).isEqualTo(SupplyChainImpacted.UNKNOWN);
    }

    @Test
    void shouldReturnUnknownWhenJobContainsShellWithoutBPN() {
        // Arrange
        final List<String> bpns = List.of("BPNL00000003B0Q0");
        String parentId = "urn:uuid:0733946c-59c6-41ae-9570-cb43a6e4c79e";

        final var shellDescriptor = AssetAdministrationShellDescriptor.builder()
                                                                      .globalAssetId(parentId)
                                                                      .specificAssetIds(List.of())
                                                                      .build();
        final Jobs jobs = Jobs.builder()
                              .job(Job.builder().globalAssetId(GlobalAssetIdentification.of(parentId)).build())
                              .shells(List.of(shellDescriptor))
                              .build();

        // Act
        final SupplyChainImpacted actual = BPNIncidentValidation.jobContainsIncidentBPNs(jobs.getShells(), bpns);

        // Assert
        assertThat(actual).isEqualTo(SupplyChainImpacted.UNKNOWN);
    }
}
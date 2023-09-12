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
package org.eclipse.tractusx.ess.service;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.tractusx.ess.irs.IrsFacade;
import org.eclipse.tractusx.irs.component.GlobalAssetIdentification;
import org.eclipse.tractusx.irs.component.Job;
import org.eclipse.tractusx.irs.component.JobHandle;
import org.eclipse.tractusx.irs.component.Jobs;
import org.eclipse.tractusx.irs.component.PartChainIdentificationKey;
import org.eclipse.tractusx.irs.component.RegisterBpnInvestigationJob;
import org.eclipse.tractusx.irs.component.enums.JobState;
import org.eclipse.tractusx.irs.edc.client.model.notification.EdcNotification;
import org.eclipse.tractusx.irs.edc.client.model.notification.EdcNotificationHeader;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class EssServiceTest {

    private final IrsFacade irsFacade = mock(IrsFacade.class);
    private final BpnInvestigationJobCache bpnInvestigationJobCache = new InMemoryBpnInvestigationJobCache();
    private final EssRecursiveNotificationHandler recursiveNotificationHandler = mock(
            EssRecursiveNotificationHandler.class);
    private final EssService essService = new EssService(irsFacade, bpnInvestigationJobCache,
            recursiveNotificationHandler);

    @Test
    void shouldSuccessfullyStartJobAndReturnWithExtendedSubmodelList() {
        final String globalAssetId = UUID.randomUUID().toString();
        final List<String> bpns = List.of("BPNS000000000DDD");
        final UUID createdJobId = UUID.randomUUID();
        final var key = PartChainIdentificationKey.builder()
                                                  .globalAssetId(globalAssetId)
                                                  .bpn("BPNS0000000000DD")
                                                  .build();
        final RegisterBpnInvestigationJob request = RegisterBpnInvestigationJob.builder()
                                                                               .key(key)
                                                                               .incidentBpns(bpns)
                                                                               .build();
        final Jobs expectedResponse = Jobs.builder()
                                          .job(Job.builder()
                                                  .state(JobState.COMPLETED)
                                                  .id(createdJobId)
                                                  .globalAssetId(GlobalAssetIdentification.of(globalAssetId))
                                                  .build())
                                          .submodels(new ArrayList<>())
                                          .shells(new ArrayList<>())
                                          .build();

        when(irsFacade.startIrsJob(eq(key), any())).thenReturn(JobHandle.builder().id(createdJobId).build());
        when(irsFacade.getIrsJob(createdJobId.toString())).thenReturn(expectedResponse);

        final JobHandle jobHandle = essService.startIrsJob(request);
        final Jobs jobs = essService.getIrsJob(jobHandle.getId().toString());

        assertThat(jobHandle).isNotNull();
        assertThat(jobHandle.getId()).isNotNull();
        assertThat(jobs).isNotNull();
        assertThat(bpnInvestigationJobCache.findAll()).hasSize(1);
    }

    @Test
    void shouldUpdateJobSnapshotIfNotificationFound() {
        final String notificationId = UUID.randomUUID().toString();
        final String notificationId2 = UUID.randomUUID().toString();
        final UUID jobId = UUID.randomUUID();
        final EdcNotification edcNotification = EdcNotification.builder()
                                                               .header(EdcNotificationHeader.builder()
                                                                                            .notificationId(
                                                                                                    notificationId)
                                                                                            .originalNotificationId(
                                                                                                    notificationId)
                                                                                            .build())
                                                               .content(Map.of("result", "No"))
                                                               .build();
        final EdcNotification edcNotification2 = EdcNotification.builder()
                                                                .header(EdcNotificationHeader.builder()
                                                                                             .notificationId(
                                                                                                     notificationId2)
                                                                                             .originalNotificationId(
                                                                                                     notificationId2)
                                                                                             .build())
                                                                .content(Map.of("result", "Yes"))
                                                                .build();
        final BpnInvestigationJob bpnInvestigationJob = BpnInvestigationJob.create(
                                                                                   Jobs.builder().job(Job.builder().id(jobId).build()).build(), new ArrayList<>())
                                                                           .withNotifications(List.of(notificationId,
                                                                                   notificationId2));
        bpnInvestigationJobCache.store(jobId, bpnInvestigationJob);

        assertDoesNotThrow(() -> essService.handleNotificationCallback(edcNotification));
        assertThat(bpnInvestigationJobCache.findAll()).hasSize(1);

        final BpnInvestigationJob job = bpnInvestigationJobCache.findAll().get(0);
        final String supplyChainImpacted = (String) job.getJobSnapshot()
                                                       .getSubmodels()
                                                       .get(0)
                                                       .getPayload()
                                                       .get("supplyChainImpacted");
        assertThat(supplyChainImpacted).isEqualTo("No");

        assertDoesNotThrow(() -> essService.handleNotificationCallback(edcNotification2));
        assertThat(bpnInvestigationJobCache.findAll()).hasSize(1);
        final BpnInvestigationJob job2 = bpnInvestigationJobCache.findAll().get(0);
        assertThat(job2.getJobSnapshot().getSubmodels()).hasSize(1);
        final String supplyChainImpacted2 = (String) job.getJobSnapshot()
                                                        .getSubmodels()
                                                        .get(0)
                                                        .getPayload()
                                                        .get("supplyChainImpacted");
        assertThat(supplyChainImpacted2).isEqualTo("Yes");

    }

    @Test
    void shouldKeepJobInRunningIfNotificationIsOpen() {
        final String notificationId = UUID.randomUUID().toString();
        final UUID jobId = UUID.randomUUID();

        final BpnInvestigationJob bpnInvestigationJob = BpnInvestigationJob.create(
                                                                                   Jobs.builder().job(Job.builder().id(jobId).build()).build(), new ArrayList<>())
                                                                           .withNotifications(Collections.singletonList(
                                                                                   notificationId));
        bpnInvestigationJobCache.store(jobId, bpnInvestigationJob);

        assertThat(bpnInvestigationJobCache.findAll()).hasSize(1);
        final Jobs byJobId = essService.getIrsJob(jobId.toString());
        assertThat(byJobId.getJob().getState()).isEqualTo(JobState.RUNNING);
    }

    @Test
    void shouldThrowResponseStatusExceptionWhenIdDoesntExists() {
        final String jobIdNotExisting = UUID.randomUUID().toString();

        assertThrows(ResponseStatusException.class, () -> essService.getIrsJob(jobIdNotExisting));
    }

}

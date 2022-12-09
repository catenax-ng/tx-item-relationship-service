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
package org.eclipse.tractusx.irs.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.tractusx.irs.aaswrapper.job.AASTransferProcess;
import org.eclipse.tractusx.irs.aaswrapper.job.ItemContainer;
import org.eclipse.tractusx.irs.component.Job;
import org.eclipse.tractusx.irs.component.Jobs;
import org.eclipse.tractusx.irs.component.PageResult;
import org.eclipse.tractusx.irs.component.Relationship;
import org.eclipse.tractusx.irs.component.enums.JobState;
import org.eclipse.tractusx.irs.connector.job.JobStore;
import org.eclipse.tractusx.irs.connector.job.MultiTransferJob;
import org.eclipse.tractusx.irs.exceptions.EntityNotFoundException;
import org.eclipse.tractusx.irs.persistence.BlobPersistence;
import org.eclipse.tractusx.irs.persistence.BlobPersistenceException;
import org.eclipse.tractusx.irs.util.JsonUtil;
import org.eclipse.tractusx.irs.util.TestMother;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class IrsItemGraphQueryServiceTest {

    private final UUID jobId = UUID.randomUUID();
    private final TestMother generate = new TestMother();

    @Mock
    private JobStore jobStore;

    @Mock
    private BlobPersistence blobStore;

    @InjectMocks
    private IrsItemGraphQueryService testee;

    @Test
    void registerItemJobWithoutDepthShouldBuildFullTree() throws Exception {
        // given
        final var jobId = UUID.randomUUID();
        final AASTransferProcess transfer1 = generate.aasTransferProcess();
        givenTransferResultIsStored(transfer1);

        final AASTransferProcess transfer2 = generate.aasTransferProcess();
        givenTransferResultIsStored(transfer2);

        givenRunningJobHasFinishedTransfers(jobId, transfer1, transfer2);

        // when
        final Jobs jobs = testee.getJobForJobId(jobId, true);

        // then
        assertThat(jobs.getRelationships()).hasSize(2);
    }

    private void givenRunningJobHasFinishedTransfers(final UUID jobId, final AASTransferProcess... transfers) {
        final MultiTransferJob job = MultiTransferJob.builder()
                                                     .completedTransfers(Arrays.asList(transfers))
                                                     .job(generate.fakeJob(JobState.RUNNING))
                                                     .build();
        when(jobStore.find(jobId.toString())).thenReturn(Optional.of(job));
    }

    private void givenTransferResultIsStored(final AASTransferProcess transfer1) throws BlobPersistenceException {
        final Relationship relationship1 = generate.relationship();
        final ItemContainer itemContainer1 = ItemContainer.builder().relationship(relationship1).build();
        when(blobStore.getBlob(transfer1.getId())).thenReturn(Optional.of(toBlob(itemContainer1)));
    }

    private byte[] toBlob(final Object transfer) {
        return new JsonUtil().asString(transfer).getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void cancelJobById() {
        final Job job = generate.fakeJob(JobState.CANCELED);

        final MultiTransferJob multiTransferJob = MultiTransferJob.builder().job(job).build();

        when(jobStore.cancelJob(jobId.toString())).thenReturn(Optional.ofNullable(multiTransferJob));
        final Job canceledJob = testee.cancelJobById(jobId);

        assertNotNull(canceledJob);
        assertEquals(canceledJob.getState().name(), JobState.CANCELED.name());
    }

    @Test
    void cancelJobById_throwEntityNotFoundException() {
        when(jobStore.cancelJob(jobId.toString())).thenThrow(
                new EntityNotFoundException("No job exists with id " + jobId));

        assertThrows(EntityNotFoundException.class, () -> testee.cancelJobById(jobId));
    }

    @Test
    void shouldReturnFoundJobs() {
        final List<JobState> states = List.of(JobState.COMPLETED);
        final MultiTransferJob multiTransferJob = MultiTransferJob.builder().job(generate.fakeJob(JobState.COMPLETED)).build();
        when(jobStore.findByStates(states)).thenReturn(List.of(multiTransferJob));

        final PageResult jobs = testee.getJobsByJobState(states, Pageable.ofSize(10));

        assertNotNull(jobs);
        assertThat(jobs.content()).hasSize(1);
        assertThat(jobs.content().get(0).getId()).isNotNull();
        assertThat(jobs.content().get(0).getState()).isEqualTo(JobState.COMPLETED);
        assertThat(jobs.content().get(0).getStartedOn()).isNotNull();
        assertThat(jobs.content().get(0).getCompletedOn()).isNotNull();
        assertThat(jobs.pageSize()).isEqualTo(10);
        assertThat(jobs.pageNumber()).isEqualTo(0);
        assertThat(jobs.pageCount()).isEqualTo(1);
        assertThat(jobs.totalElements()).isEqualTo(1);
    }

}
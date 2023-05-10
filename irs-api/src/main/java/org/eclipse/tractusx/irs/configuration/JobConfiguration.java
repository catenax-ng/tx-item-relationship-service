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
package org.eclipse.tractusx.irs.configuration;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.eclipse.tractusx.irs.edc.client.EdcSubmodelFacade;
import org.eclipse.tractusx.irs.aaswrapper.job.AASRecursiveJobHandler;
import org.eclipse.tractusx.irs.aaswrapper.job.AASTransferProcess;
import org.eclipse.tractusx.irs.aaswrapper.job.AASTransferProcessManager;
import org.eclipse.tractusx.irs.aaswrapper.job.ItemDataRequest;
import org.eclipse.tractusx.irs.aaswrapper.job.ItemTreesAssembler;
import org.eclipse.tractusx.irs.aaswrapper.job.TreeRecursiveLogic;
import org.eclipse.tractusx.irs.aaswrapper.job.delegate.BpdmDelegate;
import org.eclipse.tractusx.irs.aaswrapper.job.delegate.DigitalTwinDelegate;
import org.eclipse.tractusx.irs.aaswrapper.job.delegate.RelationshipDelegate;
import org.eclipse.tractusx.irs.aaswrapper.job.delegate.SubmodelDelegate;
import org.eclipse.tractusx.irs.aaswrapper.registry.domain.DigitalTwinRegistryFacade;
import org.eclipse.tractusx.irs.bpdm.BpdmFacade;
import org.eclipse.tractusx.irs.common.OutboundMeterRegistryService;
import org.eclipse.tractusx.irs.connector.job.JobOrchestrator;
import org.eclipse.tractusx.irs.connector.job.JobStore;
import org.eclipse.tractusx.irs.connector.job.JobTTL;
import org.eclipse.tractusx.irs.persistence.BlobPersistence;
import org.eclipse.tractusx.irs.persistence.BlobPersistenceException;
import org.eclipse.tractusx.irs.persistence.MinioBlobPersistence;
import org.eclipse.tractusx.irs.semanticshub.SemanticsHubFacade;
import org.eclipse.tractusx.irs.services.MeterRegistryService;
import org.eclipse.tractusx.irs.services.validation.JsonValidatorService;
import org.eclipse.tractusx.irs.util.JsonUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Spring configuration for job-related beans.
 */
@Configuration
@SuppressWarnings({ "PMD.ExcessiveImports", "PMD.TooManyMethods" })
public class JobConfiguration {

    public static final int EXECUTOR_CORE_POOL_SIZE = 5;

    @Bean
    public OutboundMeterRegistryService outboundMeterRegistryService(final MeterRegistry meterRegistry, final RetryRegistry retryRegistry) {
        return new OutboundMeterRegistryService(meterRegistry, retryRegistry);
    }

    @Bean
    public JobOrchestrator<ItemDataRequest, AASTransferProcess> jobOrchestrator(
            final DigitalTwinDelegate digitalTwinDelegate, final BlobPersistence blobStore, final JobStore jobStore,
            final MeterRegistryService meterService, final ApplicationEventPublisher applicationEventPublisher,
            @Value("${irs.job.jobstore.ttl.failed:}") final Duration ttlFailedJobs,
            @Value("${irs.job.jobstore.ttl.completed:}") final Duration ttlCompletedJobs) {

        final var manager = new AASTransferProcessManager(digitalTwinDelegate, Executors.newCachedThreadPool(),
                blobStore);
        final var logic = new TreeRecursiveLogic(blobStore, new JsonUtil(), new ItemTreesAssembler());
        final var handler = new AASRecursiveJobHandler(logic);
        final JobTTL jobTTL = new JobTTL(ttlCompletedJobs, ttlFailedJobs);

        return new JobOrchestrator<>(manager, jobStore, handler, meterService, applicationEventPublisher, jobTTL);
    }

    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newScheduledThreadPool(EXECUTOR_CORE_POOL_SIZE);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Profile("!test")
    @Bean
    public BlobPersistence blobStore(final BlobstoreConfiguration config) throws BlobPersistenceException {
        return new MinioBlobPersistence(config.getEndpoint(), config.getAccessKey(), config.getSecretKey(),
                config.getBucketName());
    }

    @Bean
    public JsonUtil jsonUtil() {
        return new JsonUtil();
    }

    @Bean
    public TimedAspect timedAspect(final MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    @Bean
    public DigitalTwinDelegate digitalTwinDelegate(final BpdmDelegate bpdmDelegate,
            final DigitalTwinRegistryFacade digitalTwinRegistryFacade) {
        return new DigitalTwinDelegate(bpdmDelegate, digitalTwinRegistryFacade);
    }

    @Bean
    public BpdmDelegate bpdmDelegate(final RelationshipDelegate relationshipDelegate,
            final BpdmFacade bpdmFacade) {
        return new BpdmDelegate(relationshipDelegate, bpdmFacade);
    }

    @Bean
    public RelationshipDelegate relationshipDelegate(final SubmodelDelegate submodelDelegate,
            final EdcSubmodelFacade submodelFacade) {
        return new RelationshipDelegate(submodelDelegate, submodelFacade);
    }

    @Bean
    public SubmodelDelegate submodelDelegate(final EdcSubmodelFacade submodelFacade,
            final SemanticsHubFacade semanticsHubFacade, final JsonValidatorService jsonValidatorService) {
        return new SubmodelDelegate(submodelFacade, semanticsHubFacade, jsonValidatorService, jsonUtil());
    }

}

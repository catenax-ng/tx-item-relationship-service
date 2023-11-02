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
package org.eclipse.tractusx.irs.ess.service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eclipse.tractusx.irs.component.Job;
import org.eclipse.tractusx.irs.component.Jobs;
import org.eclipse.tractusx.irs.component.Submodel;
import org.eclipse.tractusx.irs.component.Summary;
import org.eclipse.tractusx.irs.component.enums.JobState;
import org.eclipse.tractusx.irs.edc.client.model.notification.EdcNotification;
import org.eclipse.tractusx.irs.edc.client.model.notification.ResponseNotificationContent;
import org.eclipse.tractusx.irs.util.JsonUtil;

/**
 * Object to store in cache
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BpnInvestigationJob {

    private static final String SUPPLY_CHAIN_ASPECT_TYPE = "supply_chain_impacted";

    private Jobs jobSnapshot;
    private List<String> incidentBpns;
    private List<String> unansweredNotificationIds;
    private List<EdcNotification<ResponseNotificationContent>> answeredNotifications;
    private JobState state;

    public BpnInvestigationJob(final Jobs jobSnapshot, final List<String> incidentBpns) {
        this(jobSnapshot, incidentBpns, new ArrayList<>(), new ArrayList<>(), JobState.RUNNING);
    }

    public BpnInvestigationJob update(final Jobs jobSnapshot, final SupplyChainImpacted newSupplyChain) {
        final Optional<SupplyChainImpacted> previousSupplyChain = getSupplyChainImpacted();

        final SupplyChainImpacted supplyChainImpacted = previousSupplyChain.map(
                prevSupplyChain -> prevSupplyChain.or(newSupplyChain)).orElse(newSupplyChain);

        this.jobSnapshot = extendJobWithSupplyChainSubmodel(jobSnapshot, supplyChainImpacted);
        this.jobSnapshot = extendSummary(this.jobSnapshot);
        this.jobSnapshot = updateLastModified(this.jobSnapshot, ZonedDateTime.now(ZoneOffset.UTC));
        return this;
    }

    public BpnInvestigationJob withUnansweredNotificationIds(final List<String> notifications) {
        this.unansweredNotificationIds.addAll(notifications);
        return this;
    }

    public BpnInvestigationJob withAnsweredNotification(final EdcNotification<ResponseNotificationContent> notification) {
        this.unansweredNotificationIds.remove(notification.getHeader().getOriginalNotificationId());
        notification.getContent().incrementHops();
        this.answeredNotifications.add(notification);
        return this;
    }

    public BpnInvestigationJob complete() {
        this.state = JobState.COMPLETED;
        return this;
    }

    /* package */ Optional<SupplyChainImpacted> getSupplyChainImpacted() {
        return this.getJobSnapshot()
                   .getSubmodels()
                   .stream()
                   .filter(sub -> SUPPLY_CHAIN_ASPECT_TYPE.equals(sub.getAspectType()))
                   .map(sub -> sub.getPayload().get("supplyChainImpacted"))
                   .map(Object::toString)
                   .map(SupplyChainImpacted::fromString)
                   .findFirst();
    }

    private Jobs extendJobWithSupplyChainSubmodel(final Jobs irsJob, final SupplyChainImpacted supplyChainImpacted) {
        final SupplyChainImpactedAspect.SupplyChainImpactedAspectBuilder supplyChainImpactedAspectBuilder = SupplyChainImpactedAspect.builder()
                                                                                                                                     .supplyChainImpacted(
                                                                                                                                             supplyChainImpacted);

        if (getUnansweredNotificationIds().isEmpty()) {
            final Optional<ResponseNotificationContent> incidentWithMinHopsValue = getAnsweredNotifications().stream()
                                                                                                             .map(EdcNotification::getContent)
                                                                                                             .filter(ResponseNotificationContent::thereIsIncident)
                                                                                                             .min(Comparator.comparing(
                                                                                                                     ResponseNotificationContent::getHops));

            final List<SupplyChainImpactedAspect.ImpactedSupplierFirstLevel> suppliersFirstLevel = incidentWithMinHopsValue.map(
                                                                                                               min -> new SupplyChainImpactedAspect.ImpactedSupplierFirstLevel("", min.getHops()))
                                                                                                       .stream()
                                                                                                       .toList();
            supplyChainImpactedAspectBuilder.impactedSuppliersOnFirstTier(Set.copyOf(suppliersFirstLevel));
        }

        final Submodel supplyChainImpactedSubmodel = Submodel.builder()
                                                             .aspectType(SUPPLY_CHAIN_ASPECT_TYPE)
                                                             .payload(new JsonUtil().asMap(supplyChainImpactedAspectBuilder.build()))
                                                             .build();

        return irsJob.toBuilder()
                     .clearSubmodels()
                     .submodels(Collections.singletonList(supplyChainImpactedSubmodel))
                     .build();
    }

    private Jobs updateLastModified(final Jobs irsJob, final ZonedDateTime lastModifiedOn) {
        final Job job = irsJob.getJob().toBuilder().completedOn(lastModifiedOn).lastModifiedOn(lastModifiedOn).build();
        return irsJob.toBuilder().job(job).build();
    }

    private Jobs extendSummary(final Jobs irsJob) {
        final Summary oldSummary = Optional.ofNullable(irsJob.getJob().getSummary()).orElse(Summary.builder().build());
        final NotificationSummary newSummary = new NotificationSummary(oldSummary.getAsyncFetchedItems(),
                oldSummary.getBpnLookups(),
                new NotificationItems(unansweredNotificationIds.size() + answeredNotifications.size(),
                        answeredNotifications.size()));
        final Job job = irsJob.getJob().toBuilder().summary(newSummary).build();
        return irsJob.toBuilder().job(job).build();
    }

}
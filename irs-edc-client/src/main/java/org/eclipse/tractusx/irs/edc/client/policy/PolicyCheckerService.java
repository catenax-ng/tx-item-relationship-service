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
package org.eclipse.tractusx.irs.edc.client.policy;

import java.util.List;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Constraint;
import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.tractusx.irs.edc.client.StringMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

/**
 * Check and validate Policy in Catalog fetch from EDC providers.
 */
@Slf4j
@Service
public class PolicyCheckerService {

    private final List<String> allowedPolicies;

    public PolicyCheckerService(@Value("${edc.catalog.policies.allowedNames}") final List<String> allowedPolicies) {
        this.allowedPolicies = allowedPolicies;
    }

    public boolean isValid(final Policy policy) {
        final List<PolicyDefinition> policyList = allowedPolicies.stream()
                                                                 .flatMap(this::addEncodedVersion)
                                                                 .map(this::createPolicy)
                                                                 .toList();
        log.info("Checking policy {} against allowed policies: {}", StringMapper.mapToString(policy),
                String.join(",", allowedPolicies));
        return policy.getPermissions()
                     .stream()
                     .anyMatch(permission -> policyList.stream()
                                                       .anyMatch(allowedPolicy -> isValid(permission, allowedPolicy)));
    }

    private boolean isValid(final Permission permission, final PolicyDefinition policyDefinition) {
        return permission.getAction().getType().equals(policyDefinition.getPermissionActionType())
                && permission.getConstraints().stream().anyMatch(constraint -> isValid(constraint, policyDefinition));
    }

    private boolean isValid(final Constraint constraint, final PolicyDefinition policyDefinition) {
        if (constraint instanceof AtomicConstraint atomicConstraint) {
            return AtomicConstraintValidator.builder()
                                            .atomicConstraint(atomicConstraint)
                                            .leftExpressionValue(policyDefinition.getLeftExpressionValue())
                                            .rightExpressionValue(policyDefinition.getRightExpressionValue())
                                            .expectedOperator(
                                                    Operator.valueOf(policyDefinition.getConstraintOperator()))
                                            .build()
                                            .isValid();
        }
        return false;
    }

    private PolicyDefinition createPolicy(final String policyName) {
        return PolicyDefinition.builder()
                               .permissionActionType("USE")
                               .constraintType("AtomicConstraint")
                               .leftExpressionValue("idsc:PURPOSE")
                               .rightExpressionValue(policyName)
                               .constraintOperator("EQ")
                               .build();
    }

    private Stream<String> addEncodedVersion(final String original) {
        return Stream.of(original, UriUtils.encode(original, "UTF-8"));
    }

}

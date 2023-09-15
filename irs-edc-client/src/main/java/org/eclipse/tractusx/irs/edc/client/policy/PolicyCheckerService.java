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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.OrConstraint;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.XoneConstraint;
import org.eclipse.tractusx.irs.edc.client.policy.model.Constraints;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

/**
 * Check and validate Policy in Catalog fetch from EDC providers.
 */
@Slf4j
@Service("irsEdcClientPolicyCheckerService")
@RequiredArgsConstructor
public class PolicyCheckerService {

    private final AcceptedPoliciesProvider policyStore;
    @Value("${irs-edc-client.catalog.policies.acceptedRightOperands:active}")
    private final List<String> acceptedRightOperands;
    @Value("${irs-edc-client.catalog.policies.acceptedLeftOperands:PURPOSE}")
    private final List<String> acceptedLeftOperands;

    public boolean isValid(final Policy actualPolicy) {
        final List<AcceptedPolicy> acceptedPolicyList = getValidStoredPolicyIds();
//        log.info("Checking actualPolicy {} against allowed policies: {}", StringMapper.mapToString(actualPolicy),
//                String.join(",", policyList.stream().map(PolicyDefinition::getRightExpressionValue).toList()));
        if (getValidStoredPolicyIds().contains("*")) {
            return true;
        }
        return acceptedPolicyList.stream().anyMatch(acceptedPolicy -> isValid(acceptedPolicy, actualPolicy));
//        return actualPolicy.getPermissions().stream().allMatch(permission -> isValid(permission, policyList));
    }

    private boolean isValid(final AcceptedPolicy acceptedPolicy, final Policy actualPolicy) {
        return actualPolicy.getPermissions().stream().anyMatch(actualPermission -> anyValid(acceptedPolicy, actualPermission));
    }

    private boolean anyValid(final AcceptedPolicy acceptedPolicy, final Permission actualPermission) {
        return acceptedPolicy.policy().getPermissions().stream().anyMatch(acceptedPermission -> validPermission(acceptedPermission, actualPermission));
    }

    private boolean validPermission(final org.eclipse.tractusx.irs.edc.client.policy.model.Permission acceptedPermission, final Permission actualPermission) {
        return actualPermission.getAction().getType().equals(acceptedPermission.getAction().toString())
                && actualPermission.getConstraints().stream().allMatch(actualConstraint -> validateConstraint(acceptedPermission.getConstraints(), actualConstraint));
    }

    private boolean validateConstraint(final List<Constraints> constraints, final Constraint actualConstraint) {
        if (actualConstraint instanceof AtomicConstraint atomicConstraint) {
            return existIn(constraints, atomicConstraint);
        } else if (actualConstraint instanceof AndConstraint andConstraint) {
            return andConstraint.getConstraints().stream().allMatch(actualConst -> existInAnd(constraints, actualConst));
        } else if (actualConstraint instanceof OrConstraint orConstraint) {
            return orConstraint.getConstraints().stream().anyMatch(actualConst -> existInOr(constraints, actualConst));
        }
        return false;
    }

    private boolean existInAnd(final List<Constraints> constraints, final Constraint actualConst) {
        return constraints.stream().anyMatch(constr -> isValidConstraint(constr.getAnd(), actualConst));
    }

    private boolean existInOr(final List<Constraints> constraints, final Constraint actualConst) {
        return constraints.stream().anyMatch(constr -> isValidConstraint(constr.getOr(), actualConst));
    }

    private boolean isValidConstraint(final List<org.eclipse.tractusx.irs.edc.client.policy.model.Constraint> acceptedConstraintList,
            final Constraint actualConst) {
        return acceptedConstraintList.stream().anyMatch(acceptedConstraint -> isSame(acceptedConstraint, actualConst));
    }

    private boolean isSame(final org.eclipse.tractusx.irs.edc.client.policy.model.Constraint acceptedConstraint, final Constraint actualConst) {
        if (actualConst instanceof AtomicConstraint atomicConstraint) {
            acceptedConstraint.getLeftOperand().equals(atomicConstraint.getLeftExpression().toString())
        }
    }

    private boolean validateAtomicConstraint(final org.eclipse.tractusx.irs.edc.client.policy.model.Constraint acceptedConstraint,
            final AtomicConstraint atomicConstraint) {
        return AtomicConstraintValidator.builder()
                                        .atomicConstraint(atomicConstraint)
                                        .leftExpressionValue(acceptedConstraint.getLeftOperand())
                                        .rightExpressionValue(acceptedConstraint.getRightOperand().stream().findFirst().orElseThrow())
                                        .expectedOperator(Operator.valueOf(acceptedConstraint.getOperator().name()))
                                        .build()
                                        .isValid();
    }

    private boolean existIn(final List<Constraints> constraints, final AtomicConstraint atomicConstraint) {

    }

    private List<PolicyDefinition> getAllowedPolicies() {
        final List<String> policyIds = new ArrayList<>();
        final List<PolicyDefinition> allowedPolicies = new ArrayList<>();
        acceptedRightOperands.forEach(rightOperand -> allowedPolicies.addAll(
                policyIds.stream().map(policy -> createPolicy(policy, rightOperand)).toList()));
        acceptedLeftOperands.forEach(leftOperand -> allowedPolicies.addAll(
                policyIds.stream().map(policy -> createPolicy(leftOperand, policy)).toList()));

        return allowedPolicies;
    }

    private List<AcceptedPolicy> getValidStoredPolicyIds() {
        return policyStore.getAcceptedPolicies()
                          .stream()
                          .filter(p -> p.validUntil().isAfter(OffsetDateTime.now()))
                          .toList();
    }

    private boolean isValid(final Permission permission, final List<PolicyDefinition> policyDefinitions) {
        final boolean permissionTypesMatch = policyDefinitions.stream()
                                                              .allMatch(
                                                                      policyDefinition -> policyDefinition.getPermissionActionType()
                                                                                                          .equals(permission.getAction()
                                                                                                                            .getType()));
        final boolean constraintsMatch = permission.getConstraints()
                                                   .stream()
                                                   .allMatch(constraint -> isValid(constraint, policyDefinitions));
        return permissionTypesMatch && constraintsMatch;
    }

    private boolean isValid(final Constraint constraint, final List<PolicyDefinition> policyDefinitions) {
        if (constraint instanceof AtomicConstraint atomicConstraint) {
            return validateAtomicConstraint(atomicConstraint, policyDefinitions);
        } else if (constraint instanceof AndConstraint andConstraint) {
            return andConstraint.getConstraints().stream().allMatch(constr -> isValid(constr, policyDefinitions));
        } else if (constraint instanceof OrConstraint orConstraint) {
            return orConstraint.getConstraints().stream().anyMatch(constr -> isValid(constr, policyDefinitions));
        } else if (constraint instanceof XoneConstraint xoneConstraint) {
            return xoneConstraint.getConstraints().stream().filter(constr -> isValid(constr, policyDefinitions)).count()
                    == 1;
        }
        return false;
    }

    private boolean validateAtomicConstraint(final AtomicConstraint atomicConstraint,
            final PolicyDefinition policyDefinition) {
        return AtomicConstraintValidator.builder()
                                        .atomicConstraint(atomicConstraint)
                                        .leftExpressionValue(policyDefinition.getLeftExpressionValue())
                                        .rightExpressionValue(policyDefinition.getRightExpressionValue())
                                        .expectedOperator(Operator.valueOf(policyDefinition.getConstraintOperator()))
                                        .build()
                                        .isValid();
    }

    private boolean validateAtomicConstraint(final AtomicConstraint atomicConstraint,
            final List<PolicyDefinition> policyDefinitions) {
        return policyDefinitions.stream()
                                .anyMatch(policyDefinition -> validateAtomicConstraint(atomicConstraint,
                                        policyDefinition));
    }

    private PolicyDefinition createPolicy(final String leftExpression, final String rightExpression) {
        return PolicyDefinition.builder()
                               .permissionActionType("USE")
                               .constraintType("AtomicConstraint")
                               .leftExpressionValue(leftExpression)
                               .rightExpressionValue(rightExpression)
                               .constraintOperator("EQ")
                               .build();
    }

    private Stream<String> addEncodedVersion(final String original) {
        return Stream.of(original, UriUtils.encode(original, "UTF-8"));
    }

}

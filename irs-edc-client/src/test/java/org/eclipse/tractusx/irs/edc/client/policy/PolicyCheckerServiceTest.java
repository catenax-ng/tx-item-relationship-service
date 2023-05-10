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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.junit.jupiter.api.Test;

class PolicyCheckerServiceTest {

    private final PolicyCheckerService policyCheckerService = new PolicyCheckerService(List.of("ID 3.0 Trace"));

    @Test
    void shouldConfirmValidPolicy() {
        // given
        Policy policy = Policy.Builder.newInstance()
                                      .permission(Permission.Builder.newInstance()
                                                                    .action(Action.Builder.newInstance()
                                                                                          .type("USE")
                                                                                          .build())
                                                                    .constraint(AtomicConstraint.Builder.newInstance()
                                                                                                        .leftExpression(
                                                                                                                new LiteralExpression(
                                                                                                                        "idsc:PURPOSE"))
                                                                                                        .rightExpression(
                                                                                                                new LiteralExpression(
                                                                                                                        "ID 3.0 Trace"))

                                                                                                        .operator(
                                                                                                                Operator.EQ)
                                                                                                        .build())
                                                                    .build()).build();
        // when
        boolean result = policyCheckerService.isValid(policy);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void shouldRejectWrongPolicy() {
        // given
        Policy policy = Policy.Builder.newInstance()
                                      .permission(Permission.Builder.newInstance()
                                                                    .action(Action.Builder.newInstance()
                                                                                          .type("USE")
                                                                                          .build())
                                                                    .constraint(AtomicConstraint.Builder.newInstance()
                                                                                                        .leftExpression(
                                                                                                                new LiteralExpression(
                                                                                                                        "idsc:PURPOSE"))
                                                                                                        .rightExpression(
                                                                                                                new LiteralExpression(
                                                                                                                        "Wrong_Trace"))

                                                                                                        .operator(
                                                                                                                Operator.EQ)
                                                                                                        .build())
                                                                    .build()).build();
        // when
        boolean result = policyCheckerService.isValid(policy);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void shouldConfirmValidPolicyEvenEncodingVersion() {
        // given
        Policy policy = Policy.Builder.newInstance()
                                      .permission(Permission.Builder.newInstance()
                                                                    .action(Action.Builder.newInstance()
                                                                                          .type("USE")
                                                                                          .build())
                                                                    .constraint(AtomicConstraint.Builder.newInstance()
                                                                                                        .leftExpression(
                                                                                                                new LiteralExpression(
                                                                                                                        "idsc:PURPOSE"))
                                                                                                        .rightExpression(
                                                                                                                new LiteralExpression(
                                                                                                                        "ID%203.0%20Trace"))

                                                                                                        .operator(
                                                                                                                Operator.EQ)
                                                                                                        .build())
                                                                    .build()).build();
        // when
        boolean result = policyCheckerService.isValid(policy);

        // then
        assertThat(result).isTrue();
    }

}
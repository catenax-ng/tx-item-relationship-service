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
package org.eclipse.tractusx.irs.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.irs.data.StringMapper.mapFromString;

import java.util.Map;
import java.util.Set;

import org.eclipse.tractusx.irs.aaswrapper.job.IntegrityAspect;
import org.eclipse.tractusx.irs.aaswrapper.job.ItemContainer;
import org.eclipse.tractusx.irs.component.Submodel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DataIntegrityServiceTest {

    private DataIntegrityService dataIntegrityService;

    private String publicKey = "-----BEGIN PUBLIC KEY-----\n"
            + "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA6h9Uz0RH9xwlb/rqSws2\n"
            + "JccIs817v1wfgLDGDY36YwzKVBNm+5IUJvCwLeMRZJ6otUFYQUUInQgM6MgW90z7\n"
            + "k9SjUB7UxiSqZFfwIF6uanN0HisEo5dCfGSzLQBIrg1nw/CPKGf8Vn5kZ/+5aBl3\n"
            + "dLu8pht+29yALFNZxThmnUx2rmTB9fH0SKtgY9HxBzQlbYuiwhqgbpnWTgCQb+F5\n"
            + "6Q3TnvXyG3DkOMYvzdle/QLPm+ywSf1FKp/qOaX483yVN5GpkWkQJwA5fsBGFkmT\n"
            + "7hmisXEyTiIps5yR/scAOjj7evQk6+FqRksPr8mOwYwBU7F6LjnJRtg9WeHZn0CA\n"
            + "GQIDAQAB\n"
            + "-----END PUBLIC KEY-----";

    @BeforeEach
    void setUp() {
        dataIntegrityService = new DataIntegrityService(publicKey);
    }

    @Test
    void shouldValidateDataChainIntegrityWhenHashesMatch() {
        // Arrange
        final IntegrityAspect.Reference reference = new IntegrityAspect.Reference("testType",
                "3d9a14153459ef617d86116297fb63d37948bffc9247bb969b63af29fe9aac6f", "xxx");
        final IntegrityAspect.ChildData childData = new IntegrityAspect.ChildData("child-id", Set.of(reference));
        final IntegrityAspect integrityAspect = new IntegrityAspect("parent-id", Set.of(childData));
        final String payload = "{\"test\":\"test\"}";
        final Submodel submodel = Submodel.builder()
                                          .aspectType("testType")
                                          .catenaXId("child-id")
                                          .identification("child-identifier")
                                          .payload(mapFromString(payload, Map.class))
                                          .build();
        final ItemContainer build = ItemContainer.builder().submodel(submodel).integrity(integrityAspect).build();

        // Act
        final boolean b = dataIntegrityService.chainDataIntegrityIsValid(build);

        // Assert
        assertThat(b).isTrue();
    }

    @Test
    void shouldFailDataChainValidationWhenPresentedHashIsWrong() {
        // Arrange
        final IntegrityAspect.Reference reference = new IntegrityAspect.Reference("testType", "wrongHash", null);
        final IntegrityAspect.ChildData childData = new IntegrityAspect.ChildData("child-id", Set.of(reference));
        final IntegrityAspect integrityAspect = new IntegrityAspect("parent-id", Set.of(childData));
        final String payload = "{\"test\":\"test\"}";
        final Submodel submodel = Submodel.builder()
                                          .aspectType("testType")
                                          .catenaXId("child-id")
                                          .identification("child-identifier")
                                          .payload(mapFromString(payload, Map.class))
                                          .build();
        final ItemContainer build = ItemContainer.builder().submodel(submodel).integrity(integrityAspect).build();

        // Act
        final boolean b = dataIntegrityService.chainDataIntegrityIsValid(build);

        // Assert
        assertThat(b).isFalse();
    }
}
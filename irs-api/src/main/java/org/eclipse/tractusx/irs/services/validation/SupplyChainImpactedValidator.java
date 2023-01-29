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
package org.eclipse.tractusx.irs.services.validation;

import java.util.List;

import org.eclipse.tractusx.irs.component.Submodel;
import org.springframework.stereotype.Service;

@Service
public class SupplyChainImpactedValidator {

    private static final String ASPECT_TYPE_PATTERN = "supply_chain_impacted";

    public void validateNumberOfSubmodels(List<Submodel> submodels) throws IllegalStateException {
        final long numberOfSubmodels = submodels.stream()
                                                .filter(submodel -> submodel.getAspectType()
                                                                            .contains(ASPECT_TYPE_PATTERN))
                                                .count();
        if (numberOfSubmodels > 1) {
            throw new IllegalStateException("SupplychainImpacted response is in illegal state. "
                    + "The expected number of SupplychainImpacted submodels in job response is 1, "
                    + "actually there are " + numberOfSubmodels);
        }
    }

}

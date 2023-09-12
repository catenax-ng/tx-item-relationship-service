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
package org.eclipse.tractusx.irs.policystore.config;

import org.eclipse.tractusx.irs.common.persistence.BlobPersistence;
import org.eclipse.tractusx.irs.common.persistence.BlobPersistenceException;
import org.eclipse.tractusx.irs.common.persistence.MinioBlobPersistence;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Config values for the policy store.
 */
@Configuration
public class PolicyConfiguration {

    public static final String POLICY_BLOB_PERSISTENCE = "PolicyStorePersistence";

    @Profile("!test")
    @Bean(POLICY_BLOB_PERSISTENCE)
    public BlobPersistence blobStore(final PolicyBlobstoreConfiguration config) throws BlobPersistenceException {
        return new MinioBlobPersistence(config.getEndpoint(), config.getAccessKey(), config.getSecretKey(),
                config.getBucketName(), config.getDaysToLive());
    }
}

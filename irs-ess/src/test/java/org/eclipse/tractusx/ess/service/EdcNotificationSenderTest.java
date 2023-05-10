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
package org.eclipse.tractusx.ess.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.tractusx.irs.edc.client.EdcSubmodelFacade;
import org.eclipse.tractusx.irs.edc.client.exceptions.EdcClientException;
import org.eclipse.tractusx.irs.edc.client.model.notification.EdcNotification;
import org.eclipse.tractusx.irs.edc.client.model.notification.EdcNotificationHeader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EdcNotificationSenderTest {

    private final EdcSubmodelFacade edcSubmodelFacade = mock(EdcSubmodelFacade.class);
    private final String localBpn = "BPNS000000000AAA";
    private final String essLocalEdcEndpoint = "example.com/url";

    private final EdcNotificationSender sender = new EdcNotificationSender(edcSubmodelFacade, localBpn, essLocalEdcEndpoint);

    @Captor
    ArgumentCaptor<EdcNotification> notificationCaptor;

    @Test
    void shouldSendEdcNotificationWithSuccess() throws EdcClientException {
        // given
        final EdcNotification edcNotification = prepareNotification("notification-id");
        when(edcSubmodelFacade.sendNotification(anyString(), anyString(), notificationCaptor.capture())).thenReturn(
                () -> true);

        // when
        sender.sendEdcNotification(edcNotification, SupplyChainImpacted.NO);

        // then
        assertThat(notificationCaptor.getValue().getContent()).containsEntry("result", SupplyChainImpacted.NO.getDescription());

    }

    private EdcNotification prepareNotification(String notificationId) {
        final EdcNotificationHeader header = EdcNotificationHeader.builder()
                                                                  .notificationId(notificationId)
                                                                  .senderEdc("senderEdc")
                                                                  .senderBpn("senderBpn")
                                                                  .recipientBpn("recipientBpn")
                                                                  .replyAssetId("ess-response-asset")
                                                                  .replyAssetSubPath("")
                                                                  .notificationType("ess-supplier-request")
                                                                  .build();

        return EdcNotification.builder().header(header).build();
    }
}
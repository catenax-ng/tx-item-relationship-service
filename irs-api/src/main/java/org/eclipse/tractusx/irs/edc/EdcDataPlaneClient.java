package org.eclipse.tractusx.irs.edc;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.irs.edc.model.TransferProcessResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class EdcDataPlaneClient {

    private static final Pattern RESPONSE_PATTERN = Pattern.compile("\\{\"data\":\"(?<embeddedData>.*)\"\\}");

    private final RestTemplate simpleRestTemplate;

    public String getData(EndpointDataReference dataReference, String subUrl) {
        final String url = getUrl(dataReference.getEndpoint(), subUrl);

        final String response = simpleRestTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(null, headers(dataReference)),
                String.class).getBody();

        return extractData(response);
    }

    private String getUrl(String connectorUrl, String subUrl) {
        var url = connectorUrl;
        if (subUrl != null && !subUrl.isEmpty()) {
            url = url.endsWith("/") ? url + subUrl : url + "/" + subUrl;
        }
        return url;
    }

    private HttpHeaders headers(EndpointDataReference dataReference) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.add(dataReference.getAuthKey(), dataReference.getAuthCode());
        return headers;
    }

    private String extractData(String response) {
        Matcher dataMatcher = RESPONSE_PATTERN.matcher(response);
        while (dataMatcher.matches()) {
            response = dataMatcher.group("embeddedData");
            response = response.replace("\\\"", "\"").replace("\\\\", "\\");
            dataMatcher = RESPONSE_PATTERN.matcher(response);
        }
        return response;
    }

}

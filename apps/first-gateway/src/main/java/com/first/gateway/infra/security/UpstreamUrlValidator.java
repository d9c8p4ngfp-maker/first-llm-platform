package com.first.gateway.infra.security;

import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

public final class UpstreamUrlValidator {

    private UpstreamUrlValidator() {}

    public static void validate(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new GatewayException(GatewayError.INVALID_REQUEST, "baseUrl required");
        }
        try {
            URI uri = new URI(baseUrl);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new GatewayException(GatewayError.INVALID_REQUEST, "baseUrl has no valid host");
            }
            InetAddress addr = InetAddress.getByName(host);
            if (addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress()) {
                throw new GatewayException(GatewayError.INVALID_REQUEST,
                    "baseUrl must not target internal/private network: " + host);
            }
            if (addr.getHostAddress().startsWith("0.")
                || addr.getHostAddress().equals("255.255.255.255")) {
                throw new GatewayException(GatewayError.INVALID_REQUEST,
                    "baseUrl must not target internal/private network: " + host);
            }
        } catch (GatewayException ex) {
            throw ex;
        } catch (UnknownHostException ex) {
            throw new GatewayException(GatewayError.INVALID_REQUEST, "baseUrl host not resolvable: " + ex.getMessage());
        } catch (Exception ex) {
            throw new GatewayException(GatewayError.INVALID_REQUEST, "baseUrl invalid: " + ex.getMessage());
        }
    }
}

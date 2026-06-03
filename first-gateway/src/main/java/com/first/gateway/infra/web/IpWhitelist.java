package com.first.gateway.infra.web;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public final class IpWhitelist {

    private IpWhitelist() {}

    public static boolean matches(String clientIp, List<String> rules) {
        if (clientIp == null || clientIp.isBlank() || rules == null || rules.isEmpty()) {
            return false;
        }
        for (String rule : rules) {
            if (rule == null || rule.isBlank()) {
                continue;
            }
            String trimmed = rule.trim();
            if (trimmed.contains("/")) {
                if (matchesCidr(clientIp, trimmed)) {
                    return true;
                }
            } else if (trimmed.equals(clientIp)) {
                return true;
            }
        }
        return false;
    }

    static boolean matchesCidr(String clientIp, String cidr) {
        String[] parts = cidr.split("/", 2);
        if (parts.length != 2) {
            return false;
        }
        try {
            int prefixLength = Integer.parseInt(parts[1].trim());
            byte[] network = InetAddress.getByName(parts[0].trim()).getAddress();
            byte[] address = InetAddress.getByName(clientIp).getAddress();
            if (network.length != address.length) {
                return false;
            }
            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (network[i] != address[i]) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xFF << (8 - remainingBits);
            return (network[fullBytes] & mask) == (address[fullBytes] & mask);
        } catch (NumberFormatException | UnknownHostException ex) {
            return false;
        }
    }
}

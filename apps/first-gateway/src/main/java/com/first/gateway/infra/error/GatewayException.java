package com.first.gateway.infra.error;

public class GatewayException extends RuntimeException {

    private final GatewayError error;
    private final String detail;
    private final String internalDetail;

    public GatewayException(GatewayError error) {
        super(error.getDefaultMessage());
        this.error = error;
        this.detail = null;
        this.internalDetail = null;
    }

    public GatewayException(GatewayError error, String detail) {
        super(detail != null ? detail : error.getDefaultMessage());
        this.error = error;
        this.detail = detail;
        this.internalDetail = null;
    }

    private GatewayException(GatewayError error, String detail, String internalDetail) {
        super(internalDetail != null ? internalDetail : (detail != null ? detail : error.getDefaultMessage()));
        this.error = error;
        this.detail = detail;
        this.internalDetail = internalDetail;
    }

    public static GatewayException withInternal(GatewayError error, String internalDetail) {
        return new GatewayException(error, null, internalDetail);
    }

    public GatewayError getError() {
        return error;
    }

    public String getDetail() {
        return detail;
    }

    public String getInternalDetail() {
        return internalDetail;
    }
}

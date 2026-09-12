package com.hmdm.rest.json.view.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;

@ApiModel(description = "OIDC provider details needed by client")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OIDCDetailsView {
    private String clientId;
    private String redirectUri;
    private String authorizeUrl;
    private String scope;
    private String state;
    private String responseType;

    public OIDCDetailsView(
            String clientId,
            String redirectUri,
            String authorizeUrl,
            String scope,
            String state,
            String responseType) {
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.authorizeUrl = authorizeUrl;
        this.scope = scope;
        this.state = state;
        this.responseType = responseType;
    }

    public String getClientId() {
        return clientId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getAuthorizeUrl() {
        return authorizeUrl;
    }

    public String getScope() {
        return scope;
    }

    public String getState() {
        return state;
    }

    public String getResponseType() {
        return responseType;
    }
}

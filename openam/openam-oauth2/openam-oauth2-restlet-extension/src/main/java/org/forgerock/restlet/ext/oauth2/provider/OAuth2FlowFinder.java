/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.restlet.ext.oauth2.provider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.forgerock.openam.oauth2.OAuth2;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.restlet.ext.oauth2.flow.AbstractFlow;
import org.forgerock.restlet.ext.oauth2.flow.AuthorizationCodeServerResource;
import org.forgerock.restlet.ext.oauth2.flow.ClientCredentialsServerResource;
import org.forgerock.restlet.ext.oauth2.flow.ErrorServerResource;
import org.forgerock.restlet.ext.oauth2.flow.ImplicitGrantServerResource;
import org.forgerock.restlet.ext.oauth2.flow.PasswordServerResource;
import org.forgerock.restlet.ext.oauth2.flow.RefreshTokenServerResource;
import org.forgerock.restlet.ext.oauth2.flow.SAML20BearerServerResource;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.resource.Finder;
import org.restlet.resource.ServerResource;

/**
 * <p/>
 * If it request for Authorization Endpoint then the response_type [code,token]
 * <p/>
 * If it request for Token Endpoint then the grant_type
 * [authorization_code,password
 * ,client_credentials,refresh_token,urn:ietf:params:
 * oauth:grant-type:saml2-bearer]
 */
public class OAuth2FlowFinder extends Finder {

    private final OAuth2.EndpointType endpointType;

    /**
     * Constructor.
     * 
     * @param context
     *            The context.
     */
    public OAuth2FlowFinder(Context context, OAuth2.EndpointType endpointType) {
        super(context, ErrorServerResource.class);
        this.endpointType = endpointType;
    }

    private volatile Map<String, Class<? extends AbstractFlow>> flowServerResources =
            new ConcurrentHashMap<String, Class<? extends AbstractFlow>>(6);

    /**
     * Creates a new instance of the {@link ServerResource} subclass designated
     * by the "targetClass" property. The default behavior is to invoke the
     * {@link #create(Class, org.restlet.Request, org.restlet.Response)} with
     * the "targetClass" property as a parameter.
     * 
     * @param request
     *            The request to handle.
     * @param response
     *            The response to update.
     * @return The created resource or ErrorServerResource.
     */
    public ServerResource create(Request request, Response response) {
        /*
         * If an authorization request is missing the "response_type" parameter,
         * or if the response type is not understood, the authorization server
         * MUST return an error response as described in Section 4.1.2.1.
         */
        switch (endpointType) {
        case AUTHORIZATION_ENDPOINT: {
            return create(findTargetFlow(request, OAuth2.Params.RESPONSE_TYPE), request, response);
        }
        case TOKEN_ENDPOINT: {
            return create(findTargetFlow(request, OAuth2.Params.GRANT_TYPE), request, response);
        }
        default: {
            return create(findTargetFlow(request, null), request, response);
        }
        }
    }

    public AbstractFlow create(Class<? extends AbstractFlow> targetClass, Request request,
            Response response) {
        AbstractFlow result = null;
        if (targetClass != null) {
            try {
                // Invoke the default constructor
                result = targetClass.newInstance();
                result.setEndpointType(endpointType);
            } catch (Exception e) {
                getLogger().log(Level.WARNING,
                        "Exception while instantiating the target server resource.", e);
                OAuthProblemException.OAuthError.SERVER_ERROR.handle(request, e.getMessage())
                        .pushException();
                result = new ErrorServerResource();
                result.setEndpointType(endpointType);
            }
        }
        return result;
    }

    protected Class<? extends AbstractFlow> findTargetFlow(Request request, String propertyName) {
        Class<? extends AbstractFlow> targetClass = null;
        if (propertyName != null) {
            String type = OAuth2Utils.getRequestParameter(request, propertyName, String.class);
            if (type instanceof String) {
                targetClass = flowServerResources.get(type);
                if (targetClass == null) {
                    if (OAuth2.EndpointType.AUTHORIZATION_ENDPOINT.equals(endpointType)) {
                        /*targetClass =
                                OAuthProblemException.OAuthError.UNSUPPORTED_RESPONSE_TYPE.handle(
                                        request, "Type is not supported: " + type).pushException();
                        */
                        OAuthProblemException.OAuthError.UNSUPPORTED_RESPONSE_TYPE.handle(
                                request, "Type is not supported: " + type);
                    } else {
                        /*targetClass =
                                OAuthProblemException.OAuthError.UNSUPPORTED_GRANT_TYPE.handle(
                                        request, "Type is not supported: " + type).pushException();
                        */
                        OAuthProblemException.OAuthError.UNSUPPORTED_RESPONSE_TYPE.handle(
                                request, "Type is not supported: " + type);
                    }
                }
            } else {
                /*
                targetClass =
                        OAuthProblemException.OAuthError.NOT_FOUND.handle(request,
                                "Type is not set").pushException();
                */
                OAuthProblemException.OAuthError.UNSUPPORTED_RESPONSE_TYPE.handle(
                        request, "Type is not supported: " + type);
            }
        } else {
            /*
            targetClass =
                    OAuthProblemException.OAuthError.NOT_FOUND.handle(request, "Type is not set")
                            .pushException();
            */
        }
        return targetClass;
    }

    public OAuth2FlowFinder supportAuthorizationCode() {
        flowServerResources.put(OAuth2.AuthorizationEndpoint.CODE,
                AuthorizationCodeServerResource.class);
        flowServerResources.put(OAuth2.TokeEndpoint.AUTHORIZATION_CODE,
                AuthorizationCodeServerResource.class);
        flowServerResources
                .put(OAuth2.TokeEndpoint.REFRESH_TOKEN, RefreshTokenServerResource.class);
        return this;
    }

    public OAuth2FlowFinder supportImplicit() {
        flowServerResources.put(OAuth2.AuthorizationEndpoint.TOKEN,
                ImplicitGrantServerResource.class);
        return this;
    }

    public OAuth2FlowFinder supportClientCredentials() {
        flowServerResources.put(OAuth2.TokeEndpoint.CLIENT_CREDENTIALS,
                ClientCredentialsServerResource.class);
        return this;
    }

    public OAuth2FlowFinder supportPassword() {
        flowServerResources.put(OAuth2.TokeEndpoint.PASSWORD, PasswordServerResource.class);
        flowServerResources
                .put(OAuth2.TokeEndpoint.REFRESH_TOKEN, RefreshTokenServerResource.class);
        return this;
    }

    public OAuth2FlowFinder supportSAML20() {
        flowServerResources.put(OAuth2.TokeEndpoint.SAML2_BEARER, SAML20BearerServerResource.class);
        return this;
    }

}

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.apim.mediators.oauth.client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.apim.mediators.oauth.client.domain.TokenResponse;

/**
 * OAuth client implementation.
 */
public class OAuthClient {

    private static final String UTF_8 = "UTF-8";
    private static final String HTTP_POST = "POST";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CONTENT_LENGTH = "Content-Length";
    private static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    private static final Set<String> supportedGrants = new HashSet<>(Arrays.asList("password", "client_credentials"));

    private static final Gson gson = new GsonBuilder().create();
    private static final Log log = LogFactory.getLog(OAuthClient.class);

    /**
     * Generate OAuth token using password grant type.
     * 
     * @param url       token endpoint URL
     * @param apiKey    api consumer key
     * @param apiSecret api consumer secret
     * @param username  username
     * @param password  password
     * @param grantType password or client_credentials
     * @return
     * @throws IOException
     */
    public static TokenResponse generateToken(String url, String apiKey, String apiSecret, String username,
            String password, String grantType, String scope) throws IOException {

        if (!supportedGrants.contains(grantType)) {
            if (log.isDebugEnabled()) {
                log.debug("No supported Grants found for Token endpoint = " + url + " & grant type = " + grantType);
            }
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug("Initializing token generation request: [token-endpoint] " + url);
        }

        HttpURLConnection connection = null;
        String urlEncodedParams = null;

        URL url_ = new URL(url);
        connection = (HttpURLConnection) url_.openConnection();
        connection.setDoOutput(true);

        // Set query parameters
        if (grantType.equals("password")) {
            urlEncodedParams = "?grant_type=" + URLEncoder.encode("password", UTF_8) + "&username="
                    + URLEncoder.encode(username, UTF_8) + "&password=" + URLEncoder.encode(password, UTF_8);
            if (scope != null && !scope.isEmpty()) {
                urlEncodedParams += "&scope=" + URLEncoder.encode(scope, UTF_8);
            }
        } else if (grantType.equals("client_credentials")) {
            urlEncodedParams = "?grant_type=" + URLEncoder.encode("client_credentials", UTF_8);
            if (scope != null && !scope.isEmpty()) {
                urlEncodedParams += "&scope=" + URLEncoder.encode(scope, UTF_8);
            }
        }

        // Set HTTP method
        connection.setRequestMethod(HTTP_POST);
        // Set authorization header
        String credentials = Base64.getEncoder().encodeToString((apiKey + ":" + apiSecret).getBytes());
        connection.setRequestProperty(AUTHORIZATION_HEADER, "Basic " + credentials);
        connection.setRequestProperty(CONTENT_TYPE_HEADER, APPLICATION_X_WWW_FORM_URLENCODED);

        if (urlEncodedParams != null) {
            connection.setRequestProperty(CONTENT_LENGTH, Integer.toString(urlEncodedParams.getBytes().length));

            DataOutputStream writer = new DataOutputStream(connection.getOutputStream());
            writer.writeBytes(urlEncodedParams);
            writer.flush();
            writer.close();
        }

        if (log.isDebugEnabled()) {
            log.debug("Requesting access token from the Token endpoint : " + url);
        }

        int responseCode = connection.getResponseCode();

        if (log.isDebugEnabled()) {
            log.debug("Response code recieved from the Token endpoint = " + responseCode);
        }

        if (responseCode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            if (log.isDebugEnabled()) {
                log.debug("Response: [status-code] " + responseCode + " [message] " + response.toString());
            }
            return gson.fromJson(response.toString(), TokenResponse.class);
        }

        return null;
    }
}

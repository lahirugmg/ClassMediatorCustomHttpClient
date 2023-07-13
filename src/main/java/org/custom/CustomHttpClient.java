package org.custom;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import org.apache.axiom.om.OMElement;
import org.apache.http.HttpResponse;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.utils.CarbonUtils;

import javax.net.ssl.SSLContext;

public class CustomHttpClient extends AbstractMediator {

    @Override
    public boolean mediate(MessageContext messageContext) {

        try {

            String keyStorePath = CarbonUtils.getServerConfiguration()
                    .getFirstProperty(APIConstants.TRUST_STORE_LOCATION);
            String keyStorePassword = CarbonUtils.getServerConfiguration()
                    .getFirstProperty(APIConstants.TRUST_STORE_PASSWORD);

            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(Files.newInputStream(Paths.get(keyStorePath)), keyStorePassword.toCharArray());



            // Build SSL context
            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(trustStore).build();
            // Create an HttpClient instance
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLContext(sslContext)
                    .build();

            // Create an HttpPost object with the URL
            HttpPost httpPost = new HttpPost("https://localhost:7001");
            // Set the request headers
            httpPost.setHeader("Content-Type", "text/plain");

            org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext).getAxis2MessageContext();
            String requestBody = axis2MessageContext.getEnvelope().getBody().getText();

            // Set the request body
            StringEntity stringEntity = new StringEntity(requestBody);
            httpPost.setEntity(stringEntity);

            // Execute the request
            HttpResponse response = httpClient.execute(httpPost);

            // Get the response status code
            int statusCode = response.getStatusLine().getStatusCode();

            axis2MessageContext.setProperty("HTTP_SC",statusCode);
            // Get the response body
            HttpEntity responseEntity = response.getEntity();
            responseEntity.getContent();
            String responseBody = EntityUtils.toString(responseEntity);

            OMElement textElement = OMAbstractFactory.getOMFactory().createOMElement("text", null);
            textElement.setText(responseBody);
            axis2MessageContext.getEnvelope().getBody().addChild(textElement);

            // Close the HttpClient
            httpClient.close();
        } catch (Exception e) {
            log.error("Exception occured in the CustomHttpClient class mediator", e);
            throw new SynapseException("Exception occured in the CustomHttpClient class mediator");
        }
        return true;
    }
}
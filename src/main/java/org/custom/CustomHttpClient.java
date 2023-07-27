package org.custom;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import org.apache.axiom.om.OMElement;
import org.apache.http.HttpResponse;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.utils.CarbonUtils;
import javax.net.ssl.SSLContext;
import javax.xml.namespace.QName;


public class CustomHttpClient extends AbstractMediator implements ManagedLifecycle {

    private static final Log log = LogFactory.getLog(CustomHttpClient.class);

    private String maxTotal="10";

    private String defaultMaxPerRoute="10";

    private String connectionTimeout = "30000";

    private String connectionRequestTimeout = "30000";

    private String socketTimeout = "60000";

    private CloseableHttpClient httpClient;
    @Override
    public boolean mediate(MessageContext messageContext) {

        try {

            log.info("Custom mediation started ...");

            // Create an HttpPost object with the URL
            HttpPost httpPost = new HttpPost("https://run.mocky.io/v3/494114d0-05ef-49f6-847b-02a5ff0bb88d");
            // Set the request headers
            httpPost.setHeader("Content-Type", "text/plain");

            org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext).getAxis2MessageContext();
            String requestBody = axis2MessageContext.getEnvelope().getBody().getFirstElement().getText();

            // Set the request body
            StringEntity stringEntity = new StringEntity(requestBody);
            httpPost.setEntity(stringEntity);

            log.info("Sending backend request ... \n URI: " + httpPost.getURI()
                    + " \n Content length: " + requestBody.length() );

            String headersString = " ";
            for (Header header: httpPost.getAllHeaders()) {
                headersString = " " + header.toString();
            }
            log.info("Headers: " + headersString);
            // Execute the request
            HttpResponse response = httpClient.execute(httpPost);

            log.info("Backend response received ...");
            // Get the response status code
            int statusCode = response.getStatusLine().getStatusCode();

            axis2MessageContext.setProperty("HTTP_SC",statusCode);
            // Get the response body
            HttpEntity responseEntity = response.getEntity();
            responseEntity.getContent();
            log.info("Reading backend response message ...");
            String responseBody = EntityUtils.toString(responseEntity);
            axis2MessageContext.getEnvelope().getBody().getFirstElement().detach();
            // Define the namespace URI and prefix
            String namespaceURI = "http://ws.apache.org/commons/ns/payload";
            String namespacePrefix = "xmlns";

            log.info("Writing client response message ...");
            // Create an OMNamespace with the namespace URI and prefix
            OMNamespace namespace = OMAbstractFactory.getOMFactory().createOMNamespace(namespaceURI, namespacePrefix);
            OMElement textElement = OMAbstractFactory.getOMFactory().createOMElement("text", namespace);
            textElement.setText(responseBody);
            axis2MessageContext.getEnvelope().getBody().addChild(textElement);
            log.info("Custom mediation completed ...");

        } catch (Exception e) {
            handleException("Exception occured in the CustomHttpClient class mediator",e);
        }
        return true;
    }

    private static SSLConnectionSocketFactory createSocketFactory() {
        SSLContext sslContext;

        String keyStorePath = CarbonUtils.getServerConfiguration()
                .getFirstProperty(APIConstants.TRUST_STORE_LOCATION);
        String keyStorePassword = CarbonUtils.getServerConfiguration()
                .getFirstProperty(APIConstants.TRUST_STORE_PASSWORD);
        try {
            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(new FileInputStream(keyStorePath), keyStorePassword.toCharArray());
            sslContext = SSLContexts.custom().loadTrustMaterial(trustStore).build();
            return new SSLConnectionSocketFactory(sslContext);
        } catch (KeyStoreException e) {
            handleException("Failed to read from Key Store", e);
        } catch (IOException e) {
            handleException("Key Store not found in " + keyStorePath, e);
        } catch (CertificateException e) {
            handleException("Failed to read Certificate", e);
        } catch (NoSuchAlgorithmException e) {
            handleException("Failed to load Key Store from " + keyStorePath, e);
        } catch (KeyManagementException e) {
            handleException("Failed to load key from" + keyStorePath, e);
        }

        return null;
    }

    private static PoolingHttpClientConnectionManager getPoolingHttpClientConnectionManager(){

        PoolingHttpClientConnectionManager poolManager;
        SSLConnectionSocketFactory socketFactory = createSocketFactory();
        org.apache.http.config.Registry<ConnectionSocketFactory> socketFactoryRegistry =
                    RegistryBuilder.<ConnectionSocketFactory>create()
                            .register(APIConstants.HTTPS_PROTOCOL, socketFactory).build();
        poolManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        return poolManager;
    }

    @Override
    public void init(SynapseEnvironment synapseEnvironment) {

        PoolingHttpClientConnectionManager pool = null;
        try {
            pool = getPoolingHttpClientConnectionManager();

            pool.setMaxTotal(Integer.parseInt(maxTotal));
            pool.setDefaultMaxPerRoute(Integer.parseInt(defaultMaxPerRoute));

            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(Integer.parseInt(connectionTimeout))
                    .setConnectionRequestTimeout(Integer.parseInt(connectionRequestTimeout))
                    .setSocketTimeout(Integer.parseInt(socketTimeout)).build();

            HttpClientBuilder clientBuilder = HttpClients.custom().setConnectionManager(pool)
                    .setDefaultRequestConfig(config);

            // Create an HttpClient instance
            httpClient = clientBuilder.build();
        } catch (Exception e) {
            handleException("CustomHttpClient class mediator initialisation failed ",e);
        }
    }

    @Override
    public void destroy() {
        try {
            httpClient.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void handleException(String msg, Throwable t) throws SynapseException {
        log.error(msg,t);
        throw new SynapseException(msg, t);
    }

    public String getDefaultMaxPerRoute() {
        return defaultMaxPerRoute;
    }

    public void setDefaultMaxPerRoute(String defaultMaxPerRoute) {
        this.defaultMaxPerRoute = defaultMaxPerRoute;
    }

    public String getMaxTotal() {
        return maxTotal;
    }

    public void setMaxTotal(String maxTotal) {
        this.maxTotal = maxTotal;
    }

    public String getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(String connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public String getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    public void setConnectionRequestTimeout(String connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    public String getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(String socketTimeout) {
        this.socketTimeout = socketTimeout;
    }
}

package wzjtech.test.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRestClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.client.reactive.ReactiveRestClients;
import org.springframework.data.elasticsearch.config.AbstractReactiveElasticsearchConfiguration;
import org.springframework.util.StringUtils;

import javax.net.ssl.*;

@Configuration
public class ElasticConfig extends AbstractReactiveElasticsearchConfiguration {
  @Autowired
  ReactiveElasticsearchRestClientProperties properties;

  @Override
  @Bean
  public ReactiveElasticsearchClient reactiveElasticsearchClient() {

    HostnameVerifier allHostsValid = (String hostname, SSLSession session) -> true;

    // Install the all-trusting host verifier
    HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    SSLContext ctx = null;
    try {
      //make the 'localhost' be credible
      javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier((hostname, sslSession) -> true);

      // setup ssl context to ignore certificate errors
      ctx = SSLContext.getInstance("TLS");
      X509TrustManager tm = new X509TrustManager() {

        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
                                       String authType) throws java.security.cert.CertificateException {
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
                                       String authType) throws java.security.cert.CertificateException {
        }

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
          return null;
        }
      };
      ctx.init(null, new TrustManager[]{tm}, null);
      SSLContext.setDefault(ctx);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    final var config = ClientConfiguration.builder()
        .connectedTo(properties.getEndpoints().toArray(new String[]{}));

    if (properties.isUseSsl()) {
      config.usingSsl(ctx, allHostsValid);
    }
    var username = properties.getUsername();
    var pwd = properties.getPassword();
    if (StringUtils.hasText(username) && StringUtils.hasText(pwd)) {
      config.withBasicAuth(username, pwd);
    }

    if (properties.getConnectionTimeout() != null) {
      config.withConnectTimeout(properties.getConnectionTimeout());
    }

    if (properties.getSocketTimeout() != null) {
      config.withSocketTimeout(properties.getSocketTimeout());
    }

    return ReactiveRestClients.create(config.build());

  }
}

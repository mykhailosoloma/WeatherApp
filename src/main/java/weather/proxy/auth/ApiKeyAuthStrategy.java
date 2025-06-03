package weather.proxy.auth;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.logging.Logger;

public class ApiKeyAuthStrategy implements AuthorizationStrategy {
    private static final Logger LOGGER = Logger.getLogger(ApiKeyAuthStrategy.class.getName());

    private String apiKey;
    private final String paramName;

    public ApiKeyAuthStrategy(String apiKey, String paramName) {
        this.apiKey = apiKey;
        this.paramName = paramName;
    }

    @Override
    public HttpRequest.Builder authorize(HttpRequest.Builder requestBuilder) {
        if (apiKey == null || apiKey.isEmpty()) {
            LOGGER.warning("API key is empty or null");
            return requestBuilder;
        }

        URI originalUri = requestBuilder.build().uri();
        String originalQuery = originalUri.getQuery();

        String newQuery;
        if (originalQuery == null || originalQuery.isEmpty()) {
            newQuery = paramName + "=" + apiKey;
        } else if (originalQuery.contains(paramName + "=")) {

            newQuery = originalQuery.replaceAll(paramName + "=[^&]*", paramName + "=" + apiKey);
        } else {

            newQuery = originalQuery + "&" + paramName + "=" + apiKey;
        }

        try {
            URI newUri = new URI(
                originalUri.getScheme(),
                originalUri.getAuthority(),
                originalUri.getPath(),
                newQuery,
                originalUri.getFragment()
            );

            return HttpRequest.newBuilder(newUri);
        } catch (Exception e) {
            LOGGER.severe("Failed to update URI with API key: " + e.getMessage());
            return requestBuilder;
        }
    }

    @Override
    public boolean updateCredentials() {

        return false;
    }

    @Override
    public boolean isValid() {
        return apiKey != null && !apiKey.isEmpty();
    }

}

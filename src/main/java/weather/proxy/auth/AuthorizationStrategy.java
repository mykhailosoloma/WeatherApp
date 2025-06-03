package weather.proxy.auth;

import java.net.http.HttpRequest;

public interface AuthorizationStrategy {

    HttpRequest.Builder authorize(HttpRequest.Builder requestBuilder);

    boolean updateCredentials();

    boolean isValid();
}
package com.khanh.fooddelivery.restaurant_service.security;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties; import com.khanh.fooddelivery.restaurant_service.exception.*; import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value; import org.springframework.http.HttpHeaders; import org.springframework.security.oauth2.jwt.Jwt; import org.springframework.stereotype.Component; import org.springframework.web.client.*; import java.util.UUID;
@Component @RequiredArgsConstructor public class CurrentUserProviderImpl implements CurrentUserProvider {
 private final RestClient.Builder restClientBuilder; @Value("${clients.user-service.base-url}") private String baseUrl;
 public UUID getCurrentUserId(Jwt jwt){String claim=jwt.getClaimAsString("user_id");if(claim!=null&&!claim.isBlank())return parse(claim);try{UserEnvelope body=restClientBuilder.baseUrl(baseUrl).build().get().uri("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION,"Bearer "+jwt.getTokenValue()).retrieve().body(UserEnvelope.class);if(body==null||body.data()==null||body.data().id()==null)throw new AppException(ErrorCode.UNAUTHENTICATED,"Unable to resolve internal user id");return body.data().id();}catch(RestClientException e){throw new AppException(ErrorCode.UNAUTHENTICATED,"Unable to resolve internal user id from user-service");}}
 private UUID parse(String value){try{return UUID.fromString(value);}catch(IllegalArgumentException e){throw new AppException(ErrorCode.UNAUTHENTICATED,"Invalid user_id claim");}}
 @JsonIgnoreProperties(ignoreUnknown=true) record UserEnvelope(UserData data){} @JsonIgnoreProperties(ignoreUnknown=true) record UserData(UUID id){}
}

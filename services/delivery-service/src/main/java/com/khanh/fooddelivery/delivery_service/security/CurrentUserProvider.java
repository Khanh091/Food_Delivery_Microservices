package com.khanh.fooddelivery.delivery_service.security;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public interface CurrentUserProvider { UUID getCurrentUserId(Jwt jwt); }

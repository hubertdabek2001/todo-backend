package com.example.todolist.config;

import com.example.todolist.security.WebSocketAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99) // Wymuszamy, by nasz interceptor odpalił się przed filtrami Spring Security
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // "/topic" - na ten przedrostek nasłuchuje aplikacja mobilna (np. /topic/list/123)
        config.enableSimpleBroker("/topic");
        // "/app" - prefix dla wiadomości wysyłanych z aplikacji DO serwera (na razie go nie używamy intensywnie)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Główny endpoint, do którego uderzy React Native.
        // AllowedOriginPatterns("*") jest kluczowe dla apek mobilnych!
        registry.addEndpoint("/ws-todo")
                .setAllowedOriginPatterns("*");

    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Wpięcie naszej klasy zabezpieczającej (czyta JWT)
        registration.interceptors(webSocketAuthInterceptor);
    }
}
package com.herman.castservice.service

import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.socket.server.standard.ServerEndpointExporter

@Component
class WebSocketConfig {
    @Bean
    fun serverEndpointExporter(): ServerEndpointExporter {
        return ServerEndpointExporter()
    }

}
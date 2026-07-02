package com.dermacare.demo.config;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import com.dermacare.demo.dto.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

	@Override
	public Mono<Void> handle(ServerWebExchange exchange, Throwable ex)  {
		byte[] bytes = null;
		 exchange.getResponse()
         .setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);

 exchange.getResponse()
         .getHeaders()
         .setContentType(MediaType.APPLICATION_JSON);

 Response response = new Response();
 response.setSuccess(false);
 response.setStatus(503);
 response.setMessage("Requested service is unavailable");
 response.setData(null);
try {
   bytes = new ObjectMapper()
         .writeValueAsBytes(response);
}catch(Exception e) {}

 DataBuffer buffer =
         exchange.getResponse()
                 .bufferFactory()
                 .wrap(bytes);

 return exchange.getResponse()
         .writeWith(Mono.just(buffer));
}}
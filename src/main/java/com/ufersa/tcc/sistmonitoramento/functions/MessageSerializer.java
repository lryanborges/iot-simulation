package com.ufersa.tcc.sistmonitoramento.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ufersa.tcc.sistmonitoramento.datagram.Message;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MessageSerializer {
    private static final ObjectMapper objectMapper = new ObjectMapper(); // Reutilize a instância do ObjectMapper

    public static byte[] serializeMessage(Message<String> message) throws JsonProcessingException {
        // Converte o objeto Message<String> para uma String JSON
        String jsonString = objectMapper.writeValueAsString(message);
        // Converte a String JSON para um array de bytes usando UTF-8
        return jsonString.getBytes(StandardCharsets.UTF_8);
    }

    public static Message<String> deserializeMessage(byte[] jsonBytes) throws IOException {
        // Converte o array de bytes para uma String JSON
        String jsonString = new String(jsonBytes, StandardCharsets.UTF_8);
        // Converte a String JSON de volta para o objeto Message<String>
        return objectMapper.readValue(jsonString, Message.class);
    }

}

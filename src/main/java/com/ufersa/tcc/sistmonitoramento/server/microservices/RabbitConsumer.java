package com.ufersa.tcc.sistmonitoramento.server.microservices;

import com.rabbitmq.client.*;
import com.ufersa.tcc.sistmonitoramento.datagram.Message;
import com.ufersa.tcc.sistmonitoramento.functions.MessageSerializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RabbitConsumer {

    private final static String QUEUE_NAME = "iot_logs";

    public static void main(String[] args) {
        try {
            // Configuração da conexão
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost"); // ou IP do broker
            factory.setUsername("guest");
            factory.setPassword("guest");

            // Conexão e canal
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            System.out.println("[RabbitMQ] Aguardando mensagens da fila: " + QUEUE_NAME);

            // Callback para processamento de mensagens
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                byte[] body = delivery.getBody();
                try {
                    // Desserializa a mensagem recebida
                    Message<String> message = MessageSerializer.deserializeMessage(body);

                    // Aqui você pode salvar, imprimir ou enviar pro banco
                    System.out.println("=== Mensagem recebida ===");
                    System.out.println("De: " + message.getSourceIp() + ":" + message.getSourcePort());
                    System.out.println("Para: " + message.getDestinationIp() + ":" + message.getDestinationPort());
                    System.out.println("Conteúdo: " + message.getMessage());
                    System.out.println("=========================");

                    // Se quiser salvar em arquivo ou banco, chame o método aqui
                    // ex: Storage.save(message);

                } catch (IOException e) {
                    System.err.println("Erro ao desserializar mensagem: " + e.getMessage());
                    e.printStackTrace();
                }
            };

            // Inicia o consumo
            channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {});

        } catch (Exception e) {
            System.err.println("Erro no consumidor RabbitMQ: " + e.getMessage());
            e.printStackTrace();
        }
    }
}


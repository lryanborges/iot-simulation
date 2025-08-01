package com.ufersa.tcc.sistmonitoramento.server;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.ufersa.tcc.sistmonitoramento.Env;
import com.ufersa.tcc.sistmonitoramento.datagram.Message;
import com.ufersa.tcc.sistmonitoramento.functions.Functions;
import com.ufersa.tcc.sistmonitoramento.functions.LogNormalizer;
import com.ufersa.tcc.sistmonitoramento.functions.MessageSerializer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IoTHandle implements Runnable {

    boolean conexion = true;
    ObjectInputStream input;
    ObjectOutputStream authOutput;
    ObjectOutputStream storageOutput;
    InetAddress iotAddress;

    private final static String QUEUE_NAME = "iot_logs";
    private Channel rabbitChannel;
    private String rabbitConsumerTag;

    private int weight;
    private final List<Double> temperatureBuffer = new ArrayList<>();
    private final List<Double> pressureBuffer = new ArrayList<>();
    private final List<Double> humidityBuffer = new ArrayList<>();
    private final List<Double> windBuffer = new ArrayList<>();

    private double temperatureMean = 0.0;
    private double pressureMean = 0.0;
    private double humidityMean = 0.0;
    private double windMean = 0.0;

    private int logsCounter = 0;
    private int alarmsCounter = 0;

    private int lowTempCounter = 0;
    private int highTempCounter = 0;
    private int lowPressCounter = 0;
    private int highPressCounter = 0;
    private int lowHumidCounter = 0;
    private int highHumidCounter = 0;
    private int lowWindCounter = 0;
    private int highWindCounter = 0;

    private static Pattern pattern;

    public IoTHandle (ObjectInputStream iotInput, ObjectOutputStream authOutput, ObjectOutputStream storageOutput, InetAddress iotAddress) {
        this.input = iotInput;
        this.authOutput = authOutput;
        this.storageOutput = storageOutput;
        this.iotAddress = iotAddress;

        pattern = Pattern.compile(
                "\\[.*?\\]\\s+Temp:\\s+([\\d,]+)°C\\s+\\|\\s+Umid:\\s+([\\d,]+)%\\s+\\|\\s+Press:\\s+([\\d,]+)\\s+hPa\\s+\\|\\s+Vento:\\s+([\\d,]+)\\s+m/s"
        );
    }

    @Override
    public void run() {

        new Thread(this::consumeFromRabbitMQ).start();

        try {
            while(conexion) {
                Message<String> receivedMsg = (Message<String>) input.readObject();
                String msg = receivedMsg.getMessage();

                System.out.println("--------------------------------------------------");
                System.out.println("Content: " + msg);
                System.out.println("Source: " + receivedMsg.getSourceIp());
                System.out.println("Via: " + Functions.getIP(iotAddress));
                System.out.println("--------------------------------------------------");

                // pra padronização do estilo do log (padronização definidan a função normalize)
                receivedMsg.setMessage(LogNormalizer.normalize(msg));

                // consumir do rabbitmq é pelo método lá embaixo

                if(receivedMsg.getDestinationIp().equals(Env.analyticsHost) && receivedMsg.getDestinationPort() == 9001){
                    authOutput.writeObject(receivedMsg);
                    authOutput.flush();
                } else if(receivedMsg.getDestinationIp().equals(Env.storageHost) && receivedMsg.getDestinationPort() == 9002){
                    storageOutput.writeObject(receivedMsg);
                    storageOutput.flush();
                } else {
                    System.out.println("Destinatário desconhecido");
                }

                if(msg.equals("disconnect") || Functions.turnOffProxy()) {
                    conexion = false;

                    try {
                        if (rabbitChannel != null && rabbitConsumerTag != null) {
                            rabbitChannel.basicCancel(rabbitConsumerTag);
                            System.out.println("[RabbitMQ] Consumo cancelado devido ao desligamento do proxy.");
                        }
                    } catch (IOException e) {
                        System.err.println("Erro ao cancelar consumo do RabbitMQ");
                        e.printStackTrace();
                    }
                }

            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void consumeFromRabbitMQ() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(Env.proxyHost);
            factory.setUsername("guest");
            factory.setPassword("guest");

            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);

            this.rabbitChannel = channel;

            System.out.println("[RabbitMQ] Consumidor iniciado...");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                byte[] body = delivery.getBody();

                try {
                    // desserializa a mensagem do RabbitMQ
                    Message<String> msg = MessageSerializer.deserializeMessage(body);

                    // pra padronização do estilo do log (padronização definida na função normalize)
                    msg.setMessage(LogNormalizer.normalize(msg.getMessage()));

                    // pegar os dados da msg e ver se tá na faixa normal
                    Matcher matcher = pattern.matcher(msg.getMessage());

                    boolean alarm = false;
                    if (matcher.find()) {
                        double temperatura = Double.parseDouble(matcher.group(1).replace(",", "."));
                        double umidade = Double.parseDouble(matcher.group(2).replace(",", "."));
                        double pressao = Double.parseDouble(matcher.group(3).replace(",", "."));
                        double vento = Double.parseDouble(matcher.group(4).replace(",", "."));

                        calcMean(temperatura, pressao, umidade, vento);

                        logsCounter++;

                        if (temperatura < 22 || temperatura > 35) {
                            alarm = true;
                            alarmsCounter++;
                            if(temperatura < 22) {
                                lowTempCounter++;
                            } else {
                                highTempCounter++;
                            }
                        }
                        if (umidade < 40 || umidade > 80) {
                            alarm = true;
                            alarmsCounter++;
                            if(umidade < 40) {
                                lowHumidCounter++;
                            } else {
                                highHumidCounter++;
                            }
                        }
                        if (pressao < 1000 || pressao > 1020) {
                            alarm = true;
                            alarmsCounter++;
                            if (pressao < 1000) {
                                lowPressCounter++;
                            } else {
                                highPressCounter++;
                            }
                        }
                        if (vento < 2 || vento > 10) {
                            alarm = true;
                            alarmsCounter++;
                            if (vento < 2) {
                                lowWindCounter++;
                            } else {
                                highWindCounter++;
                            }
                        }

                    } else {
                        System.err.println("❌ Não foi possível extrair os dados do log.");
                    }

                    // se disparar o alarme, também envia pro server q armazena os logs alarmados
                    if (alarm && msg.getDestinationPort() != 9001) { // mudar dps tmb pra em relação ao ip
                        authOutput.writeObject(msg);
                        authOutput.flush();
                    }

                    if(msg.getDestinationIp().equals(Env.storageHost) && msg.getDestinationPort() == 9002){
                        storageOutput.writeObject(msg);
                        storageOutput.flush();
                    }

                    System.out.println("Mensagem recebida via RabbitMQ: " + msg.getMessage());

                } catch (Exception e) {
                    System.err.println("Erro ao desserializar mensagem do RabbitMQ");
                    e.printStackTrace();
                }
            };

            this.rabbitConsumerTag = channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {});

        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    private void calcMean(double temperature, double pressure, double humidity, double wind) {
        temperatureBuffer.add(temperature);
        pressureBuffer.add(pressure);
        humidityBuffer.add(humidity);
        windBuffer.add(wind);

        if(temperatureBuffer.size() == 100) {
            double sum = 0;
            for (double t : temperatureBuffer) {
                sum += t;
            }
            double newMean = sum / 100.0;

            // Atualiza a média global com peso proporcional ao número de blocos
            temperatureMean = (
                    (temperatureMean * weight) + newMean
            ) / (weight + 1);

            temperatureBuffer.clear();
        }

        if(pressureBuffer.size() == 100) {
            double sum = 0;
            for (double t : pressureBuffer) {
                sum += t;
            }
            double newMean = sum / 100.0;

            // Atualiza a média global com peso proporcional ao número de blocos
            pressureMean = (
                    (pressureMean * weight) + newMean
            ) / (weight + 1);

            pressureBuffer.clear();
        }

        if(humidityBuffer.size() == 100) {
            double sum = 0;
            for (double t : humidityBuffer) {
                sum += t;
            }
            double newMean = sum / 100.0;

            // Atualiza a média global com peso proporcional ao número de blocos
            humidityMean = (
                    (humidityMean * weight) + newMean
            ) / (weight + 1);

            humidityBuffer.clear();
        }

        if(windBuffer.size() == 100) {
            double sum = 0;
            for (double t : windBuffer) {
                sum += t;
            }
            double newMean = sum / 100.0;

            // Atualiza a média global com peso proporcional ao número de blocos
            windMean = (
                    (windMean * weight) + newMean
            ) / (weight + 1);

            windBuffer.clear();

            weight++; // no ultimo, aumenta o peso (número de iterações de 100 tmb)
        }

    }

    public synchronized double getAlarmsPercent() {
        if (logsCounter == 0) return 0.0;
        double percent = ((double) alarmsCounter / logsCounter) * 100;
        return Math.round(percent * 100.0) / 100.0; // pra ficar 2 casas decimais
    }

    public double getTemperatureMean() {
        return temperatureMean;
    }

    public double getPressureMean() {
        return pressureMean;
    }

    public double getHumidityMean() {
        return humidityMean;
    }

    public double getWindMean() {
        return windMean;
    }

    public int getLogsCounter() {
        return logsCounter;
    }

    public int getAlarmsCounter() {
        return alarmsCounter;
    }

    public int getLowTempCounter() {
        return lowTempCounter;
    }

    public int getHighTempCounter() {
        return highTempCounter;
    }

    public int getLowPressCounter() {
        return lowPressCounter;
    }

    public int getHighPressCounter() {
        return highPressCounter;
    }

    public int getLowHumidCounter() {
        return lowHumidCounter;
    }

    public int getHighHumidCounter() {
        return highHumidCounter;
    }

    public int getLowWindCounter() {
        return lowWindCounter;
    }

    public int getHighWindCounter() {
        return highWindCounter;
    }

}

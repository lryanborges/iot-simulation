package com.ufersa.tcc.sistmonitoramento.server;

import com.ufersa.tcc.sistmonitoramento.Env;
import com.ufersa.tcc.sistmonitoramento.datagram.Message;
import com.ufersa.tcc.sistmonitoramento.functions.Functions;
import com.ufersa.tcc.sistmonitoramento.server.firewall.NLP.Analyzer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReverseProxy implements Runnable {

    boolean conexion = true;

    Analyzer analyzer = new Analyzer();

    ServerSocket serverSocket;
    ObjectInputStream clientInput;
    ObjectInputStream iotInput;

    Socket authSocket;
    ObjectOutputStream authOutput;

    Socket storageSocket;
    ObjectOutputStream storageOutput;

    IoTHandle iotHandle;

    private final int PORT = 10000;

    @Override
    public void run() {
        Functions.initExecutionTimer();

        try {
            serverSocket = new ServerSocket(PORT);

            Socket connectedSocket = serverSocket.accept();
            clientInput = new ObjectInputStream(connectedSocket.getInputStream());
            InetAddress address = connectedSocket.getInetAddress();

            authSocket = new Socket();
            authSocket.bind(new InetSocketAddress(Env.analyticsHost, 9010));
            authSocket.connect(new InetSocketAddress(Env.proxyHost, 9001));
            authOutput = new ObjectOutputStream(authSocket.getOutputStream());

            storageSocket = new Socket();
            storageSocket.bind(new InetSocketAddress(Env.storageHost, 9020));
            storageSocket.connect(new InetSocketAddress(Env.proxyHost, 9002));
            storageOutput = new ObjectOutputStream(storageSocket.getOutputStream());

            Socket iotConnectedSocket = serverSocket.accept();
            iotInput = new ObjectInputStream(iotConnectedSocket.getInputStream());
            InetAddress iotAddress = iotConnectedSocket.getInetAddress();
            iotHandle = new IoTHandle(iotInput, authOutput, storageOutput, iotAddress);
            new Thread(iotHandle).start();

            new ProxyListener(serverSocket).start(); // p ficar ouvindo resposta dos servers (storage e authenticator)

            while (conexion) {
                Message<String> receivedMsg = (Message<String>) clientInput.readObject();
                String msg = receivedMsg.getMessage();

                System.out.println("--------------------------------------------------");
                System.out.println("Content: " + msg);
                System.out.println("Source: " + receivedMsg.getSourceIp());
                System.out.println("Via: " + Functions.getIP(address));
                analyzer.analyze(msg);
                System.out.println("--------------------------------------------------");

                /*Pattern padraoData = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}\\b"); // yyyy-MM-dd
                Matcher matcher = padraoData.matcher(msg);
                List<LocalDate> dates = new ArrayList<>();
                while (matcher.find()) {
                    LocalDate data = LocalDate.parse(matcher.group());
                    dates.add(data);
                }

                Message<List> request = new Message<>();
                request.setSourceIp(Env.localhost);
                request.setMessage(dates);
                request.setSourcePort(PORT);
                request.setDestinationIp(Env.localhost);
                request.setDestinationPort(9002);
                */

                if(receivedMsg.getDestinationIp().equals(Env.analyticsHost) && receivedMsg.getDestinationPort() == 9001){
                    authOutput.writeObject(receivedMsg);
                    authOutput.flush();
                } else if(receivedMsg.getDestinationIp().equals(Env.storageHost)&& receivedMsg.getDestinationPort() == 9002){
                    storageOutput.writeObject(receivedMsg);
                    storageOutput.flush();
                } else {
                    System.out.println("Destinatário desconhecido");
                }

                if(msg.equals("disconnect") || Functions.turnOffProxy()) {
                    conexion = false;
                }
            }

            generateDashboard(iotHandle);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            generateDashboard(iotHandle);
            /*try {
                storageOutput.close();
                storageSocket.close();
                authOutput.close();
                authSocket.close();
                clientInput.close();
                iotInput.close();
                serverSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }*/
        }

    }

    public static void generateDashboard(IoTHandle iotHandle) {
        System.out.println("------------------------------------------------------");
        System.out.println("\t\tDASHBOARD - RESULTADOS PÓS-EXECUÇÃO");
        System.out.println("------------------------------------------------------");
        System.out.println("⏳ Tempo de Execução: " + Functions.getElapsedTime());
        System.out.println("📊 Média de Temperatura: " + String.format("%.2f", iotHandle.getTemperatureMean()) + "ºC");
        System.out.println("📊 Média de Pressão Atmosférica: " + String.format("%.2f", iotHandle.getPressureMean()) + " hPa" );
        System.out.println("📊 Média de Umidade do Ar: " + String.format("%.2f", iotHandle.getHumidityMean()) + "%");
        System.out.println("📊 Média de Velociade do Vento: " + String.format("%.2f", iotHandle.getWindMean()) + " m/s");
        System.out.println("------------------------------------------------------");
        System.out.println("\uD83D\uDCE6 Total de Registros Processados: " + iotHandle.getLogsCounter());
        System.out.println("\uD83D\uDEA8 Alertas Detectados: " + iotHandle.getAlarmsCounter() + "(" + iotHandle.getAlarmsPercent() + "%)");
        System.out.println("\uD83C\uDF21\uFE0F Temperatura: ");
        System.out.println("\t- < 22ºC: " + iotHandle.getLowTempCounter());
        System.out.println("\t- > 35ºC: " + iotHandle.getHighTempCounter());
        System.out.println("\uD83D\uDCC8 Pressão Atmosférica: ");
        System.out.println("\t- < 1000 hPa: " + iotHandle.getLowPressCounter());
        System.out.println("\t- > 1020 hPa: " + iotHandle.getHighPressCounter());
        System.out.println("\uD83D\uDCA7 Umidade Relativa do Ar: ");
        System.out.println("\t- < 40%: " + iotHandle.getLowHumidCounter());
        System.out.println("\t- > 80%: " + iotHandle.getHighHumidCounter());
        System.out.println("\uD83D\uDCA8 Velocidade do Vento: ");
        System.out.println("\t- < 2 m/s: " + iotHandle.getLowWindCounter());
        System.out.println("\t- > 10 m/s: " + iotHandle.getHighWindCounter());
        System.out.println("------------------------------------------------------");
        Functions.getFileInfo();
        System.out.println("------------------------------------------------------");
    }

    public static void main(String[] args) {
        new Thread(new ReverseProxy()).start();
    }

}

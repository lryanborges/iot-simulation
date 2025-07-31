package com.ufersa.tcc.sistmonitoramento.server.microservices;

import com.ufersa.tcc.sistmonitoramento.Env;
import com.ufersa.tcc.sistmonitoramento.datagram.Message;
import com.ufersa.tcc.sistmonitoramento.functions.Functions;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Authenticator implements Runnable {

    boolean connexion = true;

    ServerSocket serverSocket;
    ObjectInputStream input;

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(9001);
            Socket connectedSocket = serverSocket.accept();
            input = new ObjectInputStream(connectedSocket.getInputStream());

            while (connexion) {
                Message<String> receivedMsg = (Message<String>) input.readObject();
                String msg = receivedMsg.getMessage();

                // verificação p ver se quem enviou a mensagem foi um dispositivo IoT
                if (receivedMsg.getSourceIp().equals(Env.localhost) && receivedMsg.getSourcePort() >= 6000 && receivedMsg.getSourcePort() <= 6100 ) {
                    System.out.println(receivedMsg.getSourceIp() + ":" + receivedMsg.getSourcePort() + " -> " + msg);
                } else {
                    System.out.println("Recebida do Cliente: " + msg);
                }

                if(msg.equals("disconnect")) {
                    connexion = false;
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                input.close();
                serverSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static void main(String[] args) {
        new Thread(new Authenticator()).start();
    }
}

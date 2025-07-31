package com.ufersa.tcc.sistmonitoramento.client;

import com.ufersa.tcc.sistmonitoramento.Env;
import com.ufersa.tcc.sistmonitoramento.datagram.Message;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client implements Runnable {

    boolean connexion = true;

    Socket socket;
    ObjectOutputStream output;

    private final int PORT = 7001;
    private final int SERVER_PORT = 30001;

    private int destinationPort = 9001;

    @Override
    public void run() {
        try {
            new ClientListener(SERVER_PORT).start();

            socket = new Socket();
            socket.bind(new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), PORT));
            socket.connect(new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), 20002));

            output = new ObjectOutputStream(socket.getOutputStream());

            Scanner scan = new Scanner(System.in);
            while(connexion) {
                System.out.print("Digite sua mensagem: ");
                String msg = scan.nextLine();

                if(msg.equals("9002")) {
                    destinationPort = 9002;
                }

                Message<String> messageToSend = new Message<String>(msg);
                messageToSend.setSourceIp(InetAddress.getLocalHost().getHostAddress());
                messageToSend.setSourcePort(socket.getLocalPort());
                messageToSend.setDestinationIp(Env.localhost);
                messageToSend.setDestinationPort(destinationPort);
                output.writeObject(messageToSend);
                output.flush();

                if(msg.equals("disconnect")) {
                    connexion = false;
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (output != null) {
                    output.close();
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) {
        new Thread(new Client()).start();
    }

}

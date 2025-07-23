    package com.ufersa.tcc.sistmonitoramento.client;

    import com.ufersa.tcc.sistmonitoramento.Env;
    import com.ufersa.tcc.sistmonitoramento.datagram.Message;
    import com.ufersa.tcc.sistmonitoramento.functions.LogGenerator;
    import org.apache.juli.logging.Log;

    import java.io.ObjectOutputStream;
    import java.net.InetAddress;
    import java.net.InetSocketAddress;
    import java.net.Socket;
    import java.util.Scanner;
    import java.util.concurrent.Executors;
    import java.util.concurrent.ScheduledExecutorService;
    import java.util.concurrent.TimeUnit;

    public class IoTDevice implements Runnable {

        boolean connexion = true;
        private final int port;
        private final int iotType;

        Socket socket;
        ObjectOutputStream output;

        LogGenerator logGenerator;

        public IoTDevice(int port, int iotType) {
            this.port = port;
            this.iotType = iotType;
            logGenerator = new LogGenerator();
        }

        @Override
        public void run() {
            try {
                socket = new Socket();
                socket.bind(new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), port));
                socket.connect(new InetSocketAddress(Env.internalFirewallHost, 20001));

                output = new ObjectOutputStream(socket.getOutputStream());
                Scanner scan = new Scanner(System.in);

                    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                    Runnable logTask = () -> {
                        String msg = logGenerator.generateClimateLog(iotType);

                        Message<String> messageToSend = new Message<String>(msg);
                        try {
                            messageToSend.setSourceIp(InetAddress.getLocalHost().getHostAddress());
                            messageToSend.setSourcePort(socket.getLocalPort());
                            messageToSend.setDestinationIp(Env.storageHost);
                            messageToSend.setDestinationPort(9002);
                            output.writeObject(messageToSend);
                            output.flush();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                        if(msg.equals("disconnect")) {
                            connexion = false;
                        }
                    };

                    scheduler.scheduleAtFixedRate(logTask, 0, 5, TimeUnit.SECONDS);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /*public static void main(String[] args) {
            new Thread(new IoTDevice(6001, 1)).start();
        }*/

    }

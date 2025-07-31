    package com.ufersa.tcc.sistmonitoramento.server.firewall;

    import com.rabbitmq.client.Channel;
    import com.rabbitmq.client.Connection;
    import com.rabbitmq.client.ConnectionFactory;
    import com.ufersa.tcc.sistmonitoramento.Env;
    import com.ufersa.tcc.sistmonitoramento.datagram.Message;
    import com.ufersa.tcc.sistmonitoramento.functions.Functions;
    import com.ufersa.tcc.sistmonitoramento.functions.MessageSerializer;

    import java.io.IOException;
    import java.io.ObjectInputStream;
    import java.io.ObjectOutputStream;
    import java.net.InetAddress;
    import java.net.ServerSocket;
    import java.net.Socket;
    import java.util.ArrayList;
    import java.util.List;

    public class PackageFilter implements Runnable {

        private static boolean firewallOn = true;

        int PORT;
        int PORTTOCONNECT;

        boolean connexion = true;

        List<Rule> rules = new ArrayList<Rule>();

        ServerSocket serverSocket;

        Socket socket;
        ObjectOutputStream output;

        boolean useRabbitMQ = false;
        private Channel channel;
        private Connection connection;
        private final static String QUEUE_NAME = "iot_logs";

        public PackageFilter(int port, int portToConnect, boolean rabbitmq) {
            PORT = port;
            PORTTOCONNECT = portToConnect;
            this.useRabbitMQ = rabbitmq;

            if (useRabbitMQ) {
                setupRabbitMQ();
            }
        }

        @Override
        public void run() {

            applyDefaultRules();

            try {
                serverSocket = new ServerSocket(PORT);

                socket = new Socket(Env.localhost, PORTTOCONNECT); // mudar endereço pro do proxy reverso
                output = new ObjectOutputStream(socket.getOutputStream());

                while(firewallOn) {
                    Socket connectedSocket = serverSocket.accept();
                    new Thread( () -> {

                        try {
                            ObjectInputStream input = new ObjectInputStream(connectedSocket.getInputStream());
                            InetAddress address = connectedSocket.getInetAddress();
                            String ip = Functions.getIP(address);

                            if(ip.equals(Env.localhost)) {
                                while(connexion) {
                                    Message<String> receivedMsg = (Message<String>) input.readObject();

                                    boolean allowed = false;
                                    for(Rule rule : rules) {
                                        if(rule.getSourceIp().equals(receivedMsg.getSourceIp())) {
                                            if(rule.getSourcePort() == receivedMsg.getSourcePort() || rule.getSourcePort() == -1) {
                                                if(rule.getDestinationIp().equals(receivedMsg.getDestinationIp())) {
                                                    if(rule.getDestinationPort() == receivedMsg.getDestinationPort() || rule.getDestinationPort() == -1) {
                                                        if(rule.isAllow()) {
                                                            allowed = true;
                                                            break;
                                                        } else {
                                                            System.out.println("Mensagem impedida pelo firewall");
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    StringBuilder sb = new StringBuilder();
                                    sb.append("--------------------------------------------------\n");
                                    sb.append("Source: " + receivedMsg.getSourceIp() + ", port " + receivedMsg.getSourcePort() + "\n");
                                    sb.append("Via: " + Functions.getIP(address) + "\n");
                                    sb.append("Destination: " + receivedMsg.getDestinationIp() + ", port " + receivedMsg.getDestinationPort() + "\n");
                                    sb.append("Next Jump: " + socket.getInetAddress().getHostAddress() + "\n");
                                    sb.append("Allowed: " + allowed + "\n");
                                    sb.append("--------------------------------------------------\n");

                                    System.out.println(sb.toString());

                                    if(allowed) {
                                        if(useRabbitMQ) { // se for bom esperar na fila de mensagens (caso dos iotdevices)

                                            byte[] messageBytes = MessageSerializer.serializeMessage(receivedMsg);
                                            sendToRabbit(messageBytes);

                                        } else { // se a msg for p ser enviada direta
                                            output.writeObject(receivedMsg);
                                            output.flush();
                                        }

                                        if(receivedMsg.equals("disconnect")) {
                                            connexion = false;
                                        }
                                    }

                                }
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                    }).start();

                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                /*try {
                    output.close();
                    socket.close();
                    input.close();
                    serverSocket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }*/
            }
        }

        private void applyDefaultRules() {
            // -1 é o código que eu defini pra 'any'
            rules.add(new Rule(Env.localhost, 7002, Env.localhost, -1, false));
            rules.add(new Rule("localhost", -1, "localhost", -1, true));
            rules.add(new Rule(Env.localhost, 7001, Env.localhost, -1, true));
            rules.add(new Rule(Env.localhost, 6001, Env.localhost, -1, true));
            for (int i = 2; i <= 100; i++) { // pra setar pra permitir os iotdevices do 6002 ao 6100 (99 outros q faltavam)
                rules.add(new Rule(Env.localhost, 6000 + i, Env.localhost, -1, true));
            }
            rules.add(new Rule(Env.localhost, 15000, Env.localhost, -1, true));
        }

        private void setupRabbitMQ() {
            try {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost("localhost"); // ou outro endereço
                factory.setUsername("guest");
                factory.setPassword("guest");

                connection = factory.newConnection();
                channel = connection.createChannel();
                channel.queueDeclare(QUEUE_NAME, true, false, false, null);

                System.out.println("[RabbitMQ] Conexão iniciada.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void sendToRabbit(byte[] msgBytes) {
            if (!useRabbitMQ) return;
            try {
                channel.basicPublish("", QUEUE_NAME, null, msgBytes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static void main(String[] args) {
            //new Thread(new PackageFilter(20002, 10000, false)).start(); // externo (pros clientes)
            new Thread(new PackageFilter(20001, 10000, true)).start(); // interno (pros dispositivos IoT)
        }

    }

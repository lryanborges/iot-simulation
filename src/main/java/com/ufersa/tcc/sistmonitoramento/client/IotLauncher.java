package com.ufersa.tcc.sistmonitoramento.client;

public class IotLauncher {

    public static void main(String[] args) {
        for (int i = 1; i <= 100; i++) {
            int iotType = 1 + (i % 4);

            new Thread(new IoTDevice(6000 + i, iotType)).start();

            System.out.println("Dispositivo IoT nº " + i + " iniciado.");
        }
    }

}

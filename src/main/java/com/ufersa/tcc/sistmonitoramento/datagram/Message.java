package com.ufersa.tcc.sistmonitoramento.datagram;

import java.io.Serializable;

public class Message<T> implements Serializable {

    private String sourceIp;
    private int sourcePort;
    private String destinationIp;
    private int destinationPort;
    private T message;

    public Message() { }

    public Message(T msg) {
        this.message = msg;
    }

    public Message(String sourceIp, String destinationIp, T msg) {
        this.sourceIp = sourceIp;
        this.destinationIp = destinationIp;
        this.message = msg;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
    }

    public String getDestinationIp() {
        return destinationIp;
    }

    public void setDestinationIp(String destinationIp) {
        this.destinationIp = destinationIp;
    }

    public int getDestinationPort() {
        return destinationPort;
    }

    public void setDestinationPort(int destinationPort) {
        this.destinationPort = destinationPort;
    }

    public T getMessage() {
        return message;
    }

    public void setMessage(T message) {
        this.message = message;
    }
}

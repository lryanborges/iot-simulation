package com.ufersa.tcc.sistmonitoramento;

import com.ufersa.tcc.sistmonitoramento.server.ReverseProxy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SistmonitoramentoApplication {

	public static void main(String[] args) {
		new Thread(new ReverseProxy()).start();

		System.out.println("ENTROU");

		SpringApplication.run(SistmonitoramentoApplication.class, args);
	}

}

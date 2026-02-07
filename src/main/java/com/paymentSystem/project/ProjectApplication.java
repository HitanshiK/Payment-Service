package com.paymentSystem.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProjectApplication {

	public static void main(String[] args) {
		try{
			SpringApplication.run(ProjectApplication.class, args);
			System.out.println("Docker is up");
			System.out.println("MY SQL and Redis connected");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getCause());
		}
	}

}

//reset pin logic


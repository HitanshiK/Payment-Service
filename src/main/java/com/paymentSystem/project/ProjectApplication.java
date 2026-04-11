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

//to do next ->
//refund flow -> user initiated   -> refunds are generally system initiated

//reset pin logic
//create custom Lock repository
// send rs 1 for first time payment                      user experience
//show amount in words at the time of entering


/** How does the G1 garbage collector work, and how would you tune it for a low-latency payment service processing 50,000 transactions per minute **/
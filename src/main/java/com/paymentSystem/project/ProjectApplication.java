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

//create project structure

// start with entity models
//user
//payments
// ledgers
// fx rate
//wallet
//idempotency key
//external bank payment


//├── config          → Security, Redis, DB configs
//├── controller      → REST APIs
//├── service         → Business logic
//├── repository      → JPA repositories
//├── entity          → DB entities   //models
//├── dto             → Request/Response objects
//├── ledger          → Ledger engine logic
//├── payment         → Payment processing logic
//├── wallet          → Wallet logic
//├── auth            → JWT + PIN logic
//├── events          → Event classes + listeners
//├── exception       → Custom exceptions
//└── util            → Helpers (Idempotency, etc.)


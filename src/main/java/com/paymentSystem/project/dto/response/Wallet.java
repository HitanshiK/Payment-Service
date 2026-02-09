package com.paymentSystem.project.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class Wallet {
    private  User User;
    List<Items> wallets = new ArrayList<>();

    @Data
    @NoArgsConstructor
   public static class Items {
        private long id;
        private String currency;
        private String status;
        private double balance;
        private double perTransLimit;
        private boolean deafault;

        public Items (com.paymentSystem.project.entity.Wallet wallet){
            this.id = wallet.getId();
            this.balance = wallet.getBalance();
            this.currency = wallet.getCurrency().toString();
            this.status = wallet.getStatus().toString();
            this.deafault = wallet.isDefault();
            this.perTransLimit = wallet.getPerTransLimit();
        }
    }


    //static because the class doesnot need wallet object to be created
    @Data
    @NoArgsConstructor
    public  static class User {
        private long id;
        private String name;
        private String email;
        private String mobile;
    }
}


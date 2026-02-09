package com.paymentSystem.project.controllers;

import com.paymentSystem.project.dto.request.AddWalletRequest;
import com.paymentSystem.project.dto.response.AddWalletResponse;
import com.paymentSystem.project.dto.response.Wallet;
import com.paymentSystem.project.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    @Autowired
    WalletService walletService;

    @PostMapping("/{user}")
    public AddWalletResponse createWallet(@PathVariable String user, AddWalletRequest request){
       return walletService.createWallet(user,request);
    }

    @GetMapping("/{user}/all")
    public Wallet fetchAllWallets (@PathVariable String user){
        return walletService.fetchAllWallets(user);
    }
    // fetch wallet balance
    @GetMapping("/{wallet}/balance")
    public double fetchBalance (@PathVariable Long walletId){
        return walletService.fetchWalletBalance(walletId);
    }
}

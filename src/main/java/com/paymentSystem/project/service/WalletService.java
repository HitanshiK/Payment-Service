package com.paymentSystem.project.service;

import com.paymentSystem.project.dto.request.AddWalletRequest;
import com.paymentSystem.project.dto.response.AddWalletResponse;
import com.paymentSystem.project.entity.User;
import com.paymentSystem.project.entity.Wallet;
import com.paymentSystem.project.enums.Status;
import com.paymentSystem.project.repos.UserRepository;
import com.paymentSystem.project.repos.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**Future scope
 * Security compliance
 */
@Service
public class WalletService {
    /// a wallet can be frozen only by system triggers and @System role and will be reversed by system itself

    @Autowired
    UserRepository userRepository;
    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository){
        this.walletRepository = walletRepository;
    }

    public AddWalletResponse createWallet(String userId, AddWalletRequest request){
        Long userIdInLong = Long.parseLong(userId);
        User user = userRepository.findById(userIdInLong)
                .orElseThrow(()->new RuntimeException("User not found"));

        if(user.getStatus().equals(Status.INACTIVE)){
            throw new RuntimeException("USER_INACTIVE");
        }

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setCurrency(request.getCurrency());
        wallet.setPerTransLimit(request.getPerTransLimit());
        wallet.setDefault(request.isDefault());
        walletRepository.save(wallet);

        AddWalletResponse response = new AddWalletResponse();
        response.setUserId(wallet.getUser().getId());
        response.setWalletId(wallet.getId());
        response.setCurrency(wallet.getCurrency().toString());
        response.setCreatedAt(wallet.getCreatedAt());
        return response;
    }

    public com.paymentSystem.project.dto.response.Wallet fetchAllWallets (String userId){
        Long userIdInLong = Long.parseLong(userId);
        User user = userRepository.findById(userIdInLong)
                .orElseThrow(()->new RuntimeException("User not found"));

        if(user.getStatus().equals(Status.INACTIVE)){
            throw new RuntimeException("USER_INACTIVE");
        }

        List<Wallet> wallets = walletRepository.findByUser(userIdInLong);
        com.paymentSystem.project.dto.response.Wallet response = new com.paymentSystem.project.dto.response.Wallet();
        com.paymentSystem.project.dto.response.Wallet.User userDetails = new com.paymentSystem.project.dto.response.Wallet.User();
        userDetails.setId(user.getId());
        userDetails.setName(user.getName());
        userDetails.setEmail(user.getEmail());
        userDetails.setMobile(user.getMobile());
        response.setUser(userDetails);

        for (Wallet wallet : wallets){
            response.getWallets().add(new com.paymentSystem.project.dto.response.Wallet.Items(wallet));
        }

        return response;
    }

    public double fetchWalletBalance (Long walletId){
        Wallet wallet = walletRepository.findById(walletId).orElseThrow(()->  new RuntimeException("Wallet not found"));

        if (!wallet.getStatus().equals(Status.ACTIVE)){
            throw new RuntimeException("WALLET STATUS INVALID");
        }

        return wallet.getBalance();
    }
}

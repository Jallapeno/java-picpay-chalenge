package com.picpaysimplify.services;

import com.picpaysimplify.domain.transaction.Transaction;
import com.picpaysimplify.domain.user.User;
import com.picpaysimplify.dtos.TransactionDTO;
import com.picpaysimplify.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class TransactionService {
    @Autowired
    private UserService userService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RestTemplate restTemplate;

    public void createTransaction(TransactionDTO transactionDTO) throws Exception {
        User sender = this.userService.findUserById(transactionDTO.senderId());
        User receiver = this.userService.findUserById(transactionDTO.receiverId());

        this.userService.validateTransaction(sender, transactionDTO.value());

        boolean isAuthorized = this.authorizeTransaction(sender, transactionDTO.value());
        if(!isAuthorized) {
            throw new Exception("Transaction not authorized");
        }

        Transaction newTransaction = new Transaction();
        newTransaction.setAmount(transactionDTO.value());
        newTransaction.setSender(sender);
        newTransaction.setReceiver(receiver);
        newTransaction.setTimestamp(LocalDateTime.now());

        sender.setBalance(sender.getBalance().subtract(transactionDTO.value()));
        receiver.setBalance(receiver.getBalance().add(transactionDTO.value()));

        this.transactionRepository.save(newTransaction);
        this.userService.createUser(sender);
        this.userService.createUser(receiver);
    }

    public boolean authorizeTransaction(User sender, BigDecimal value) {
        ResponseEntity<Map> authorizationResponse =restTemplate.getForEntity("https://util.devi.tools/api/v2/authorize", Map.class);
        if(authorizationResponse.getStatusCode().is2xxSuccessful()) {
            String status = (String) authorizationResponse.getBody().get("status");
            return "success".equalsIgnoreCase(status);
        } else return false;
    }
}

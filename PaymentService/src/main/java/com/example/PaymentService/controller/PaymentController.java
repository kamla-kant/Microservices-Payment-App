package com.example.PaymentService.controller;

import com.example.PaymentService.model.PaymentRequest;
import com.example.PaymentService.model.PaymentResponse;
import com.example.PaymentService.service.PaymentService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
@Log4j2
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<Long> doPayment(@RequestBody PaymentRequest paymentRequest){
        return new ResponseEntity<>(paymentService.doPayment(paymentRequest), HttpStatus.OK);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentDetailsByOrderId(@PathVariable long orderId){
        return new ResponseEntity<>(
                paymentService.getPaymentDetailsByOrderId(orderId),
                HttpStatus.OK);
    }

}

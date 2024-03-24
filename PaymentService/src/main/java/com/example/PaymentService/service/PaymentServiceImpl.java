package com.example.PaymentService.service;

import com.example.PaymentService.entity.TransactionDetails;
import com.example.PaymentService.model.PaymentMode;
import com.example.PaymentService.model.PaymentRequest;
import com.example.PaymentService.model.PaymentResponse;
import com.example.PaymentService.repository.TransactionDetailsRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Log4j2
public class PaymentServiceImpl implements PaymentService{

    @Autowired
    private TransactionDetailsRepository transactionDetailsRepository;

    @Override
    public long doPayment(PaymentRequest paymentRequest) {
        log.info("Recording payment details: {}", paymentRequest);
        TransactionDetails transactionDetails = TransactionDetails.builder()
                .amount(paymentRequest.getAmount())
                .paymentDate(Instant.now())
                .paymentMode(paymentRequest.getPaymentMode().name())
                .orderId(paymentRequest.getOrderId())
                .referenceNumber(paymentRequest.getReferenceNumber())
                .paymentStatus("SUCCESS")
                .build();
        transactionDetailsRepository.save(transactionDetails);
        log.info("Transaction completed with id : {}", transactionDetails.getId());
        return transactionDetails.getId();
    }

    @Override
    public PaymentResponse getPaymentDetailsByOrderId(long orderId) {
        log.info("Getting payment details for the Order Id: {}", orderId);

        TransactionDetails transactionDetails
                = transactionDetailsRepository.findByOrderId(Long.valueOf(orderId));

        PaymentResponse paymentResponse
                = PaymentResponse.builder()
                .paymentId(transactionDetails.getId())
                .paymentMode(PaymentMode.valueOf(transactionDetails.getPaymentMode()))
                .paymentDate(transactionDetails.getPaymentDate())
                .orderId(transactionDetails.getOrderId())
                .status(transactionDetails.getPaymentStatus())
                .amount(transactionDetails.getAmount())
                .build();

        return paymentResponse;
    }
}

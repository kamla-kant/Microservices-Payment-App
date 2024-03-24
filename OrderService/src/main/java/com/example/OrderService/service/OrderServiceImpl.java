package com.example.OrderService.service;

import com.example.OrderService.entity.Order;
import com.example.OrderService.exception.CustomException;
import com.example.OrderService.external.client.PaymentService;
import com.example.OrderService.external.client.ProductService;
import com.example.OrderService.external.request.PaymentRequest;
import com.example.OrderService.external.response.PaymentResponse;
import com.example.OrderService.external.response.ProductResponse;
import com.example.OrderService.model.OrderRequest;
import com.example.OrderService.model.OrderResponse;
import com.example.OrderService.repository.OrderRepository;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
@Log4j2
public class OrderServiceImpl implements OrderService{

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public long placeOrder(OrderRequest orderRequest) {

        //order entity->Save the data with status order created
        //Product Service -> Block Products(Reduce Quantity)
        //Payment service -> Payment -> success -> Complete -> else Canceled
        log.info("Placing order request: {}", orderRequest);
        //CALLING ANOTHER SVC USING FEIGN CLIENT
        productService.reduceQuantity(orderRequest.getProductId(), orderRequest.getQuantity());
        log.info("Creating order with status CREATED");

        Order order = Order.builder()
                .orderDate(Instant.now())
                .orderStatus("CREATED")
                .amount(orderRequest.getTotalAmount())
                .productId(orderRequest.getProductId())
                .quantity(orderRequest.getQuantity())
                .build();

        order = orderRepository.save(order);

        log.info("Calling payment service to complete payment");

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderId(order.getId())
                .amount(orderRequest.getTotalAmount())
                .paymentMode(orderRequest.getPaymentMode())
                .build();

        String orderStatus = null;
        try {
            paymentService.doPayment(paymentRequest);
            log.info("Payment done successfully. Changing order status to PLACED");
            orderStatus = "PLACED";
        } catch (Exception e) {
            log.info("Error occurred in payment. Changing order status to FAILED");
            orderStatus = "PAYMENT_FAILED";
        }

        order.setOrderStatus(orderStatus);
        orderRepository.save(order);

        log.info("Order placed successfully with order Id: {}", order.getId());


        return order.getId();
    }

    @Override
    public OrderResponse getOrderDetails(long orderId) {
        log.info("Getting order details for order id: {}",orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(()-> new CustomException("Order not found with order id: "+orderId, "NOT_FOUND",404));

        log.info("Invoking payment service to fetch the payment details for orderId: {}",orderId);

        //CALLING ANOTHER SVC USING REST TEMPLATE
        ProductResponse productResponse = restTemplate.getForObject(
                "http://PRODUCT-SERVICE/product/"+order.getProductId()
                ,ProductResponse.class);

        log.info("Getting payment info from payment service");
        PaymentResponse paymentResponse = restTemplate.getForObject(
                "http://PAYMENT-SERVICE/payment/"+order.getId(), PaymentResponse.class);


        OrderResponse.ProductDetails productDetails = OrderResponse.ProductDetails.builder()
                .price(productResponse.getPrice())
                .productId(productResponse.getProductId())
                .productName(productResponse.getProductName())
                .quantity(productResponse.getQuantity())
                .build();

        OrderResponse.PaymentDetails paymentDetails = OrderResponse.PaymentDetails.builder()
                .paymentId(paymentResponse.getPaymentId())
                .paymentDate(paymentResponse.getPaymentDate())
                .orderId(paymentResponse.getOrderId())
                .amount(paymentResponse.getAmount())
                .status(paymentResponse.getStatus())
                .build();

        OrderResponse orderResponse = OrderResponse.builder()
                .orderId(order.getId())
                .orderDate(order.getOrderDate())
                .orderStatus(order.getOrderStatus())
                .amount(order.getAmount())
                .productDetails(productDetails)
                .paymentDetails(paymentDetails)
                .build();
        return orderResponse;
    }
}

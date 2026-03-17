package com.revature.revpay.controller;

import com.revature.revpay.model.User;
import com.revature.revpay.service.MoneyRequestService;
import com.revature.revpay.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/requests")
public class MoneyRequestController {

    private final MoneyRequestService moneyRequestService;
    private final UserService userService;
    private final com.revature.revpay.service.PaymentMethodService paymentMethodService;

    public MoneyRequestController(MoneyRequestService moneyRequestService, UserService userService, com.revature.revpay.service.PaymentMethodService paymentMethodService) {
        this.moneyRequestService = moneyRequestService;
        this.userService = userService;
        this.paymentMethodService = paymentMethodService;
    }

    @GetMapping("/send")
    public String sendPage(Authentication auth, Model model) {
        model.addAttribute("user", userService.findByEmailOrPhone(auth.getName()));
        return "transactions/request";
    }

    @PostMapping("/send")
    public String sendRequest(Authentication auth,
            @RequestParam String requestee,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String purpose,
            RedirectAttributes ra) {
        User user = userService.findByEmailOrPhone(auth.getName());
        String result = moneyRequestService.sendRequest(user.getUserId(), requestee, amount, purpose);
        if ("SUCCESS".equals(result)) {
            ra.addFlashAttribute("successMsg", "Money request sent!");
        } else {
            ra.addFlashAttribute("errorMsg", switch (result) {
                case "USER_NOT_FOUND" -> "User not found.";
                case "CANNOT_REQUEST_SELF" -> "Cannot request money from yourself.";
                default -> "Request failed.";
            });
        }
        return "redirect:/requests/outgoing";
    }

    @GetMapping("/incoming")
    public String incoming(Authentication auth, Model model) {
        User user = userService.findByEmailOrPhone(auth.getName());
        model.addAttribute("user", user);
        model.addAttribute("requests", moneyRequestService.getIncoming(user.getUserId()));
        model.addAttribute("paymentMethods", paymentMethodService.getByUser(user.getUserId()));
        return "transactions/money-requests";
    }

    @GetMapping("/outgoing")
    public String outgoing(Authentication auth, Model model) {
        User user = userService.findByEmailOrPhone(auth.getName());
        model.addAttribute("user", user);
        model.addAttribute("requests", moneyRequestService.getOutgoing(user.getUserId()));
        return "transactions/money-requests-out";
    }

    @PostMapping("/{id}/accept")
    public String accept(@PathVariable Long id, Authentication auth,
            @RequestParam(required = false) Long methodId,
            @RequestParam(required = false) String pin,
            RedirectAttributes ra) {
        
        if (auth == null) {
            org.apache.logging.log4j.LogManager.getLogger(MoneyRequestController.class).error("Unauthorized access attempt to accept request {}: auth is null", id);
            return "redirect:/auth/login";
        }
        
        User user = userService.findByEmailOrPhone(auth.getName());
        org.apache.logging.log4j.LogManager.getLogger(MoneyRequestController.class).info("User {} is accepting money request {}", user.getUserId(), id);

        if (pin == null || pin.isBlank()) {
            ra.addFlashAttribute("errorMsg", "Please enter your Transaction PIN.");
            return "redirect:/requests/incoming";
        }

        if (!userService.verifyTransactionPin(user.getUserId(), pin)) {
            ra.addFlashAttribute("errorMsg", "Invalid Transaction PIN. Authentication failed.");
            return "redirect:/requests/incoming";
        }

        String result = moneyRequestService.acceptRequest(id, user.getUserId());
        if ("SUCCESS".equals(result)) {
            org.apache.logging.log4j.LogManager.getLogger(MoneyRequestController.class).info("Request {} accepted successfully by user {}", id, user.getUserId());
            ra.addFlashAttribute("successMsg", "Request accepted and payment sent.");
        } else {
            org.apache.logging.log4j.LogManager.getLogger(MoneyRequestController.class).warn("Request {} acceptance failed for user {}: {}", id, user.getUserId(), result);
            ra.addFlashAttribute("errorMsg", "Failed: " + result);
        }
        return "redirect:/requests/incoming";
    }

    @PostMapping("/{id}/decline")
    public String decline(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        User user = userService.findByEmailOrPhone(auth.getName());
        moneyRequestService.declineRequest(id, user.getUserId());
        ra.addFlashAttribute("successMsg", "Request declined.");
        return "redirect:/requests/incoming";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        User user = userService.findByEmailOrPhone(auth.getName());
        moneyRequestService.cancelRequest(id, user.getUserId());
        ra.addFlashAttribute("successMsg", "Request cancelled.");
        return "redirect:/requests/outgoing";
    }
}

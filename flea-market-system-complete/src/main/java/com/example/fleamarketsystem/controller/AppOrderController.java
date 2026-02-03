package com.example.fleamarketsystem.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.fleamarketsystem.entity.AppOrder;
import com.example.fleamarketsystem.entity.Cart;
import com.example.fleamarketsystem.entity.CartItem;
import com.example.fleamarketsystem.entity.Item;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.repository.AppOrderRepository;
import com.example.fleamarketsystem.repository.CartItemRepository;
import com.example.fleamarketsystem.repository.CartRepository;
import com.example.fleamarketsystem.service.AppOrderService;
import com.example.fleamarketsystem.service.CartService;
import com.example.fleamarketsystem.service.ItemService;
import com.example.fleamarketsystem.service.UserService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

@Controller
@RequestMapping("/orders")
public class AppOrderController {

	private final AppOrderService appOrderService;
	private final UserService userService;
	private final ItemService itemService;
	private final CartRepository cartRepository;
	private final AppOrderRepository appOrderRepository;
	private final CartItemRepository cartItemRepository;
	private final CartService cartService;

	@Value("${stripe.public.key}")
	private String stripePublicKey;

	public AppOrderController(AppOrderService appOrderService, UserService userService, ItemService itemService,
			CartRepository cartRepository, CartItemRepository cartItemRepository, CartService cartService,
			AppOrderRepository appOrderRepository) {
		this.appOrderService = appOrderService;
		this.userService = userService;
		this.itemService = itemService;
		this.cartRepository = cartRepository;
		this.cartItemRepository = cartItemRepository;
		this.cartService = cartService;
		this.appOrderRepository = appOrderRepository;
	}

	@PostMapping("/initiate-purchase") // New endpoint to initiate purchase and get client secret
	public String initiatePurchase(
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam("itemId") Long itemId,
			RedirectAttributes redirectAttributes) {
		User buyer = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("Buyer not found"));
		try {
			PaymentIntent paymentIntent = appOrderService.initiatePurchase(itemId, buyer);
			redirectAttributes.addFlashAttribute("clientSecret", paymentIntent.getClientSecret());
			redirectAttributes.addFlashAttribute("itemId", itemId);
			return "redirect:/orders/confirm-payment";
		} catch (IllegalStateException | IllegalArgumentException | StripeException e) {
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
			return "redirect:/items/" + itemId; // Redirect back to item detail with error
		}
	}

	@GetMapping("/confirm-payment") // Page to confirm payment with Stripe Elements
	public String confirmPayment(@ModelAttribute("clientSecret") String clientSecret,
			@ModelAttribute("itemId") Long itemId, Model model) {
		if (clientSecret == null || itemId == null) {
			return "redirect:/items"; // Redirect if no payment intent data
		}
		model.addAttribute("clientSecret", clientSecret);
		model.addAttribute("itemId", itemId);
		model.addAttribute("stripePublicKey", stripePublicKey);
		return "payment_confirmation";
	}

	@GetMapping("/complete-purchase") // Endpoint called by Stripe.js after payment is confirmed on client-side
	public String completePurchase(
			@RequestParam("paymentIntentId") String paymentIntentId,
			RedirectAttributes redirectAttributes) {
		try {
			appOrderService.completePurchase(paymentIntentId);
			redirectAttributes.addFlashAttribute("successMessage", "商品を購入しました！");
			// Redirect to review page after successful purchase
			// You might need to pass the order ID to the review page
			// For now, let's assume the latest order is the one just completed
			// In a real app, you'd get the order ID from the payment intent metadata or a more robust way
			return appOrderService.getLatestCompletedOrderId()
					.map(orderId -> "redirect:/reviews/new/" + orderId)
					.orElseGet(() -> {
						redirectAttributes.addFlashAttribute("errorMessage", "購入は完了しましたが、評価ページへのリダイレクトに失敗しました。");
						return "redirect:/my-page/orders";
					});
		} catch (StripeException | IllegalStateException e) {
			redirectAttributes.addFlashAttribute("errorMessage", "決済処理中にエラーが発生しました: " + e.getMessage());
			return "redirect:/items"; // Redirect to item list or a generic error page
		}
	}

	// Stripe Webhook endpoint (conceptual - needs proper security and implementation)
	@PostMapping("/stripe-webhook")
	public void handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
		// In a real application, you would verify the webhook signature
		// and process events like payment_intent.succeeded, payment_intent.payment_failed, etc.
		System.out.println("Received Stripe Webhook: " + payload);
		// Example: if (event.getType().equals("payment_intent.succeeded")) { ... }
	}

	@PostMapping("/{id}/ship")
	public String shipOrder(@PathVariable("id") Long orderId, RedirectAttributes redirectAttributes) {
		try {
			appOrderService.markOrderAsShipped(orderId);
			redirectAttributes.addFlashAttribute("successMessage", "商品を発送済みにしました。");
		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
		}
		return "redirect:/my-page/sales";
	}

	@PostMapping("/cart/purchase")
	public String purchaseFromCart(
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam("cartItemId") Long cartItemId,
			RedirectAttributes redirectAttributes) {

		User buyer = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		try {
			appOrderService.purchaseFromCart(cartItemId, buyer);
			redirectAttributes.addFlashAttribute("successMessage", "商品を購入しました");
			return "redirect:/my-page/orders";
		} catch (IllegalStateException | IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
			return "redirect:/cart";
		}
	}

	@PostMapping("/purchase")
	public String purchaseCart(@AuthenticationPrincipal UserDetails userDetails) throws Exception {

		User buyer = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		Cart cart = cartRepository.findByUser(buyer)
				.orElseThrow(() -> new RuntimeException("カートが存在しません"));

		List<CartItem> cartItems = cartItemRepository.findByCart(cart);

		if (cartItems.isEmpty()) {
			throw new RuntimeException("カートが空です");
		}

		for (CartItem cartItem : cartItems) {
			Item item = cartItem.getItem();

			AppOrder order = new AppOrder();
			order.setItem(item);
			order.setBuyer(buyer);
			order.setPrice(item.getPrice());
			order.setStatus("購入済");

			appOrderRepository.save(order);
		}

		// カートを空にする
		cartItemRepository.deleteAll(cartItems);

		return "redirect:/order/complete";
	}

}

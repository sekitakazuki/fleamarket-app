package com.example.fleamarketsystem.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.fleamarketsystem.entity.CartItem;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.service.AppOrderService;
import com.example.fleamarketsystem.service.CartService;
import com.example.fleamarketsystem.service.UserService;

@Controller
@RequestMapping("/cart")
public class CartController {

	private final CartService cartService;
	private final UserService userService;
	private final AppOrderService appOrderService;

	public CartController(CartService cartService, UserService userService, AppOrderService appOrderService) {
		this.cartService = cartService;
		this.userService = userService;
		this.appOrderService = appOrderService;
	}

	@GetMapping
	public String viewCart(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		User user = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));
		List<CartItem> cartItems = cartService.getCartItems(user);

		boolean removeAny = cartItems.removeIf(item -> {
			if (!"出品中".equals(item.getItem().getStatus())) {
				cartService.removeItemFromCart(user, item.getId());
				return true;
			}
			return false;
		});
		if (removeAny) {
			model.addAttribute("infoMessage", "売却済みの商品がカートから削除されました。");
		}

		model.addAttribute("cartItems", cartItems);
		return "cart";
	}

	@PostMapping("/add")
	public String addToCart(
			@RequestParam("itemId") Long itemId,
			@AuthenticationPrincipal UserDetails userDetails,
			RedirectAttributes redirectAttributes) {
		User user = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));
		System.out.println(itemId);
		try {
			cartService.addItemToCart(user, itemId);
			redirectAttributes.addFlashAttribute("successMessage", "カートに追加しました。");
		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
		}
		return "redirect:/cart";
	}

	@PostMapping("/remove")
	public String removeFromCart(
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam("cartItemId") Long cartItemId,
			RedirectAttributes redirectAttributes) {
		User user = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		cartService.removeItemFromCart(user, cartItemId);
		redirectAttributes.addFlashAttribute("successMessage", "カートから削除しました");

		return "redirect:/cart";
	}

	@PostMapping("/checkout")
	public String checkout(@AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request,
			RedirectAttributes ra) {
		User user = userService.getUserByEmail(userDetails.getUsername()).orElseThrow();

		try {
			List<CartItem> currentItems = cartService.getCartItems(user);
			boolean hasSoldItem = currentItems.stream()
					.anyMatch(ci -> !"出品中".equals(ci.getItem().getStatus()));

			if (hasSoldItem) {
				ra.addFlashAttribute("errorMessage", "売り切れの商品が含まれています。カートを更新してください。");
				return "redirect:/cart";
			}
			// StripeのURLを取得
			String stripeUrl = appOrderService.initiateCartPurchase(user, request);

			// 外部サイト(Stripe)へのリダイレクト
			return "redirect:" + stripeUrl;

		} catch (Exception e) {
			ra.addFlashAttribute("errorMessage", "決済の準備に失敗しました: " + e.getMessage());
			return "redirect:/cart";
		}
	}

	@GetMapping("/success")
	public String success(@RequestParam("session_id") String sessionId, RedirectAttributes ra) {
		try {
			// 注文確定・カート削除処理
			appOrderService.completeCartPurchase(sessionId);

			ra.addFlashAttribute("successMessage", "決済が完了しました。ありがとうございます！");
			return "redirect:/my-page/orders";

		} catch (Exception e) {
			ra.addFlashAttribute("errorMessage", "確定処理中にエラーが発生しました。");
			return "redirect:/cart";
		}
	}

}

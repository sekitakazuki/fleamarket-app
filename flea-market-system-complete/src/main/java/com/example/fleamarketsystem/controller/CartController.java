package com.example.fleamarketsystem.controller;

import java.util.List;

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
import com.example.fleamarketsystem.service.CartService;
import com.example.fleamarketsystem.service.UserService;

@Controller
@RequestMapping("/cart")
public class CartController {

	private final CartService cartService;
	private final UserService userService;

	public CartController(CartService cartService, UserService userService) {
		this.cartService = cartService;
		this.userService = userService;
	}

	@GetMapping
	public String viewCart(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		User user = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));
		List<CartItem> cartItems = cartService.getCartItems(user);

		System.out.println(cartItems);

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

}

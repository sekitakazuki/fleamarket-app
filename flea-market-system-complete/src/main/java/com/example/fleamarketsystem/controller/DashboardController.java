package com.example.fleamarketsystem.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.repository.UserRepository;
import com.example.fleamarketsystem.service.AppOrderService;
import com.example.fleamarketsystem.service.ItemService;

@Controller
public class DashboardController {

	private final UserRepository userRepository;
	private final ItemService itemService;
	private final AppOrderService appOrderService;

	public DashboardController(UserRepository userRepository, ItemService itemService,
			AppOrderService appOrderService) {
		this.userRepository = userRepository;
		this.itemService = itemService;
		this.appOrderService = appOrderService;
	}

	@GetMapping("/dashboard")
	public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		User currentUser = userRepository.findByEmailIgnoreCase(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		if ("ADMIN".equals(currentUser.getRole())) {
			model.addAttribute("recentItems", itemService.getAllItems());
			model.addAttribute("recentOrders", appOrderService.getAllOrders());
			return "admin_dashboard";
		} else {
			return "redirect:/items";
		}
	}
}

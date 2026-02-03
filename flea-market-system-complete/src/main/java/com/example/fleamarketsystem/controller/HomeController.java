// src/main/java/com/example/fleamarketsystem/controller/HomeController.java
package com.example.fleamarketsystem.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	@GetMapping("/")
	public String home(Authentication auth) {
		// 未ログインでも /items に逃がす
		if (auth == null || !auth.isAuthenticated()) {
			return "redirect:/items";
		}
		boolean isAdmin = auth.getAuthorities().stream()
				.anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
		return isAdmin ? "redirect:/admin/users" : "redirect:/items";
	}
}

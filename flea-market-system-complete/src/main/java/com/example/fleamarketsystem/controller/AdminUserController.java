// src/main/java/com/example/fleamarketsystem/controller/AdminUserController.java
package com.example.fleamarketsystem.controller;

import java.util.Comparator;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.repository.UserRepository;
import com.example.fleamarketsystem.service.AdminUserService;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

	private final AdminUserService service;
	private final UserRepository users;

	public AdminUserController(AdminUserService service, UserRepository users) {
		this.service = service;
		this.users = users;
	}

	@GetMapping
	public String list(@RequestParam(value = "q", required = false) String q,
			@RequestParam(value = "sort", required = false, defaultValue = "id") String sort,
			Model model) {
		List<User> list = service.listAllUsers();

		if (StringUtils.hasText(q)) {
			String qq = q.toLowerCase();
			list = list.stream().filter(u -> (u.getName() != null && u.getName().toLowerCase().contains(qq)) ||
					(u.getEmail() != null && u.getEmail().toLowerCase().contains(qq))).toList();
		}

		list = switch (sort) {
		case "name" -> list.stream().sorted(Comparator.comparing(User::getName,
				Comparator.nullsLast(String::compareToIgnoreCase))).toList();
		case "email" -> list.stream().sorted(Comparator.comparing(User::getEmail,
				Comparator.nullsLast(String::compareToIgnoreCase))).toList();
		case "banned" -> list.stream().sorted(Comparator.comparing(User::isBanned).reversed()).toList();
		default -> list;
		};

		model.addAttribute("users", list);
		model.addAttribute("q", q);
		model.addAttribute("sort", sort);
		return "admin/users/list";
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Model model) {
		User user = service.findUser(id);
		Double avg = service.averageRating(id);
		long complaints = service.complaintCount(id);
		model.addAttribute("user", user);
		model.addAttribute("avgRating", avg);
		model.addAttribute("complaintCount", complaints);
		model.addAttribute("complaints", service.complaints(id));
		return "admin/users/detail";
	}

	@PostMapping("/{id}/ban")
	public String ban(@PathVariable Long id,
			@RequestParam("reason") String reason,
			@RequestParam(value = "disableLogin", defaultValue = "true") boolean disableLogin,
			Authentication auth) {
		Long adminId = users.findByEmailIgnoreCase(auth.getName()).map(User::getId).orElse(null);
		service.banUser(id, adminId, reason, disableLogin);
		return "redirect:/admin/users/" + id + "?banned";
	}

	@PostMapping("/{id}/unban")
	public String unban(@PathVariable Long id) {
		service.unbanUser(id);
		return "redirect:/admin/users/" + id + "?unbanned";
	}
}

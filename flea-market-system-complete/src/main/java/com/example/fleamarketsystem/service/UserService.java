package com.example.fleamarketsystem.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.repository.UserRepository;

@Service
public class UserService {
	private final UserRepository repo;

	public UserService(UserRepository repo) {
		this.repo = repo;
	}

	public List<User> getAllUsers() {
		return repo.findAll();
	}

	public Optional<User> getUserById(Long id) {
		return repo.findById(id);
	}

	public Optional<User> getUserByEmail(String email) {
		return repo.findByEmail(email);
	}

	@Transactional
	public User saveUser(User user) {
		return repo.save(user);
	}

	@Transactional
	public void deleteUser(Long id) {
		repo.deleteById(id);
	}

	@Transactional
	public void toggleUserEnabled(Long userId) {
		User u = repo.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
		u.setEnabled(!u.isEnabled());
		repo.save(u);
	}
}

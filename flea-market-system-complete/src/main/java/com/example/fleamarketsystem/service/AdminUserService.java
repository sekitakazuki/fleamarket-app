// src/main/java/com/example/fleamarketsystem/service/AdminUserService.java
package com.example.fleamarketsystem.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.entity.UserComplaint;
import com.example.fleamarketsystem.repository.UserComplaintRepository;
import com.example.fleamarketsystem.repository.UserRepository;

@Service
public class AdminUserService {

	private final UserRepository userRepository;
	private final UserComplaintRepository complaintRepository;

	public AdminUserService(UserRepository userRepository, UserComplaintRepository complaintRepository) {
		this.userRepository = userRepository;
		this.complaintRepository = complaintRepository;
	}

	public List<User> listAllUsers() {
		return userRepository.findAll();
	}

	public User findUser(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("User not found: " + id));
	}

	public Double averageRating(Long userId) {
		Double avg = userRepository.averageRatingForUser(userId);
		return (avg == null) ? 0.0 : avg;
	}

	public long complaintCount(Long userId) {
		return complaintRepository.countByReportedUserId(userId);
	}

	public List<UserComplaint> complaints(Long userId) {
		return complaintRepository.findByReportedUserIdOrderByCreatedAtDesc(userId);
	}

	@Transactional
	public void banUser(Long targetUserId, Long adminUserId, String reason, boolean alsoDisableLogin) {
		User u = findUser(targetUserId);
		u.setBanned(true);
		u.setBanReason(reason);
		u.setBannedAt(LocalDateTime.now());
		u.setBannedByAdminId(adminUserId == null ? null : adminUserId.intValue());
		if (alsoDisableLogin)
			u.setEnabled(false);
		userRepository.save(u);
	}

	@Transactional
	public void unbanUser(Long targetUserId) {
		User u = findUser(targetUserId);
		u.setBanned(false);
		u.setBanReason(null);
		u.setBannedAt(null);
		u.setBannedByAdminId(null);
		u.setEnabled(true); // 任意
		userRepository.save(u);
	}
}

package com.example.fleamarketsystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.fleamarketsystem.entity.Review;
import com.example.fleamarketsystem.entity.User;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
	List<Review> findBySeller(User seller);

	Optional<Review> findByOrderId(Long orderId);

	List<Review> findByReviewer(User reviewer); // Add this line
}
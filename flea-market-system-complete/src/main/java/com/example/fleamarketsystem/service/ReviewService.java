package com.example.fleamarketsystem.service;

import com.example.fleamarketsystem.entity.AppOrder;
import com.example.fleamarketsystem.entity.Review;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.repository.AppOrderRepository;
import com.example.fleamarketsystem.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.OptionalDouble;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final AppOrderRepository appOrderRepository;

    public ReviewService(ReviewRepository reviewRepository, AppOrderRepository appOrderRepository) {
        this.reviewRepository = reviewRepository;
        this.appOrderRepository = appOrderRepository;
    }

    @Transactional
    public Review submitReview(Long orderId, User reviewer, int rating, String comment) {
        AppOrder order = appOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found."));

        if (!order.getBuyer().getId().equals(reviewer.getId())) {
            throw new IllegalStateException("Only the buyer can review this order.");
        }

        if (reviewRepository.findByOrderId(orderId).isPresent()) {
            throw new IllegalStateException("This order has already been reviewed.");
        }

        Review review = new Review();
        review.setOrder(order);
        review.setReviewer(reviewer);
        review.setSeller(order.getItem().getSeller());
        review.setItem(order.getItem());
        review.setRating(rating);
        review.setComment(comment);

        return reviewRepository.save(review);
    }

    public List<Review> getReviewsBySeller(User seller) {
        return reviewRepository.findBySeller(seller);
    }

    public OptionalDouble getAverageRatingForSeller(User seller) {
        return reviewRepository.findBySeller(seller).stream()
                .mapToInt(Review::getRating)
                .average();
    }

    public List<Review> getReviewsByReviewer(User reviewer) {
        return reviewRepository.findByReviewer(reviewer);
    }
}

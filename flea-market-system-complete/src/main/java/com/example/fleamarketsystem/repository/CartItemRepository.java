package com.example.fleamarketsystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.fleamarketsystem.entity.Cart;
import com.example.fleamarketsystem.entity.CartItem;
import com.example.fleamarketsystem.entity.Item;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
	List<CartItem> findByCart(Cart cart);

	Optional<CartItem> findByCartAndItem(Cart cart, Item item);

	boolean existsByCartAndItem(Cart cart, Item item);
}
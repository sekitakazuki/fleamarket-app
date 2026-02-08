package com.example.fleamarketsystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.fleamarketsystem.entity.Cart;
import com.example.fleamarketsystem.entity.CartItem;
import com.example.fleamarketsystem.entity.Item;
import com.example.fleamarketsystem.entity.User;

@Repository
@Transactional
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
	List<CartItem> findByCart(Cart cart);

	Optional<CartItem> findByCartAndItem(Cart cart, Item item);

	boolean existsByCartAndItem(Cart cart, Item item);

	// ユーザーのカート内商品を一括削除するために追加
	void deleteByCart_User(User user);

	// ユーザーのカート内商品を一括取得するために追加
	List<CartItem> findByCart_User(User user);
}
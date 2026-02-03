package com.example.fleamarketsystem.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fleamarketsystem.entity.AppOrder;
import com.example.fleamarketsystem.entity.Cart;
import com.example.fleamarketsystem.entity.CartItem;
import com.example.fleamarketsystem.entity.Item;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.repository.AppOrderRepository;
import com.example.fleamarketsystem.repository.CartItemRepository;
import com.example.fleamarketsystem.repository.CartRepository;
import com.example.fleamarketsystem.repository.ItemRepository;

@Service
@Transactional
public class CartService {

	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final ItemRepository itemRepository;
	private final AppOrderRepository appOrderRepository;

	public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
			ItemRepository itemRepository, AppOrderRepository appOrderRepository) {
		this.cartRepository = cartRepository;
		this.cartItemRepository = cartItemRepository;
		this.itemRepository = itemRepository;
		this.appOrderRepository = appOrderRepository;
	}

	public Cart getOrCreateCart(User user) {
		return cartRepository.findByUser(user)
				.orElseGet(() -> {
					Cart cart = new Cart();
					cart.setUser(user);
					return cartRepository.save(cart);
				});
	}

	public List<CartItem> getCartItems(User user) {
		Cart cart = getOrCreateCart(user);
		return cartItemRepository.findByCart(cart);
	}

	public void addItemToCart(User user, Long itemId) {
		Cart cart = getOrCreateCart(user);

		Item item = itemRepository.findById(itemId)
				.orElseThrow(() -> new IllegalArgumentException("商品が存在しません。"));
		System.out.println(cart + "," + item);
		if (cartItemRepository.existsByCartAndItem(cart, item)) {
			throw new IllegalStateException("この商品はすでにカートに入っています。");
		}

		CartItem cartItem = new CartItem();
		cartItem.setCart(cart);
		cartItem.setItem(item);

		cartItemRepository.save(cartItem);
	}

	public void removeItemFromCart(User user, Long cartItemId) {
		Cart cart = getOrCreateCart(user);
		CartItem cartItem = cartItemRepository.findById(cartItemId)
				.orElseThrow(() -> new IllegalArgumentException("カート商品が存在しません。"));

		if (!cartItem.getCart().getId().equals(cart.getId())) {
			throw new IllegalArgumentException("不正な操作です。");
		}

		cartItemRepository.delete(cartItem);
	}

	public void clearCart(User user) {
		Cart cart = getOrCreateCart(user);
		cartItemRepository.deleteAll(cartItemRepository.findByCart(cart));
	}

	@Transactional
	public AppOrder purchaseAll(User user, String paymentIntentId) {

		Cart cart = cartRepository.findByUser(user)
				.orElseThrow(() -> new RuntimeException("カートが存在しません"));

		List<CartItem> cartItems = cartItemRepository.findByCart(cart);

		if (cartItems.isEmpty()) {
			throw new RuntimeException("カートが空です");
		}

		// 合計金額計算
		BigDecimal totalPrice = cartItems.stream()
				.map(ci -> ci.getItem().getPrice())
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// 注文作成
		AppOrder order = new AppOrder();
		order.setBuyer(user);
		order.setPrice(totalPrice);
		order.setStatus("PAID");
		order.setPaymentIntentId(paymentIntentId);

		appOrderRepository.save(order);

		// カートを空にする
		cartItemRepository.deleteAll(cartItems);

		return order;
	}
}

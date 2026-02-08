package com.example.fleamarketsystem.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.fleamarketsystem.entity.CartItem;
import com.example.fleamarketsystem.entity.User;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCreateParams;

@Service
public class StripeService {

	public StripeService(@Value("${stripe.api.secretKey}") String secretKey) {
		Stripe.apiKey = secretKey;
	}

	public PaymentIntent createPaymentIntent(BigDecimal amount, String currency, String description)
			throws StripeException {
		PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
				.setAmount(amount.multiply(new BigDecimal(100)).longValue()) // Amount in cents
				.setCurrency(currency)
				.setDescription(description)
				.setAutomaticPaymentMethods(
						PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
								.setEnabled(true)
								.build())
				.build();
		return PaymentIntent.create(params);
	}

	public PaymentIntent confirmPaymentIntent(String paymentIntentId) throws StripeException {
		PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
		return paymentIntent.confirm();
	}

	public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
		return PaymentIntent.retrieve(paymentIntentId);
	}

	public Session createCheckoutSession(List<CartItem> cartItems, User buyer, HttpServletRequest request)
			throws StripeException {
		// 1. Line Items (商品リスト) の作成
		List<Object> lineItems = new ArrayList<>();
		String requestUrl = request.getRequestURL().toString();
		String contextPath = request.getContextPath();

		String baseUrl = requestUrl.replace(request.getServletPath(), "");

		for (CartItem cartItem : cartItems) {
			Map<String, Object> lineItem = new HashMap<>();

			// 価格データ (Price Data)
			Map<String, Object> priceData = new HashMap<>();
			priceData.put("currency", "jpy");
			priceData.put("unit_amount", cartItem.getItem().getPrice().longValue());

			// 商品データ (Product Data)
			Map<String, Object> productData = new HashMap<>();
			productData.put("name", cartItem.getItem().getName());
			priceData.put("product_data", productData);

			lineItem.put("price_data", priceData);
			lineItem.put("quantity", 1);

			lineItems.add(lineItem);
		}

		// 2. セッションパラメータ全体の作成
		Map<String, Object> params = new HashMap<>();

		// 支払い方法の種類
		List<String> paymentMethodTypes = new ArrayList<>();
		paymentMethodTypes.add("card");
		params.put("payment_method_types", paymentMethodTypes);

		// モード
		params.put("mode", "payment");

		// リダイレクトURL
		params.put("success_url", baseUrl + "/cart/success?session_id={CHECKOUT_SESSION_ID}");
		params.put("cancel_url", baseUrl + "/cart");

		// 商品リストをセット
		params.put("line_items", lineItems);

		// ユーザーIDをメタデータに入れる
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("userId", buyer.getId().toString());
		params.put("metadata", metadata);

		// 3. 作成 (Mapを渡す)
		return Session.create(params);
	}
}

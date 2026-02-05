package com.dmmprices;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Handles communication with the OSRS Wiki Price API.
 * Fetches latest DMM prices and item mappings for searching.
 */
@Slf4j
@Singleton
public class WikiPriceClient {
	private static final String API_URL = "https://prices.runescape.wiki/api/v1/dmm/latest";
	private static final String MAPPING_URL = "https://prices.runescape.wiki/api/v1/osrs/mapping";
	private static final String USER_AGENT = "Deadman Annihilation Price Plugin - @USER_DMM";

	private final OkHttpClient okHttpClient;
	private final Gson gson;

	private Map<Integer, PriceData> prices = Collections.emptyMap();
	private java.util.List<ItemMapping> itemMappings = Collections.emptyList();

	@Inject
	private WikiPriceClient(OkHttpClient okHttpClient, Gson gson) {
		this.okHttpClient = okHttpClient;
		this.gson = gson;
	}

	public void fetchPrices() {
		// Fetch the latest seasonal DMM price data
		fetchFromUrl(API_URL, (json) -> {
			PriceResponse priceResponse = gson.fromJson(json, PriceResponse.class);
			if (priceResponse != null && priceResponse.getData() != null) {
				prices = priceResponse.getData();
				log.info("DMM prices loaded successfully: {}", prices.size());
			}
		});
	}

	public void fetchMapping() {
		// Fetch item IDs, names, and buy limits
		fetchFromUrl(MAPPING_URL, (json) -> {
			Type type = new TypeToken<java.util.List<ItemMapping>>() {
			}.getType();
			java.util.List<ItemMapping> mappings = gson.fromJson(json, type);
			if (mappings != null) {
				itemMappings = mappings;
				log.info("Item mappings loaded successfully: {}", itemMappings.size());
			}
		});
	}

	// Helper to handle async HTTP calls to the Wiki API
	private void fetchFromUrl(String url, java.util.function.Consumer<String> consumer) {
		Request request = new Request.Builder()
				.url(url)
				.header("User-Agent", USER_AGENT)
				.build();

		okHttpClient.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				log.error("Network error fetching from " + url, e);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				if (!response.isSuccessful()) {
					log.error("API error from " + url + ": " + response.code());
					response.close();
					return;
				}

				try {
					String json = response.body().string();
					consumer.accept(json);
				} catch (Exception e) {
					log.error("Parsing error from " + url, e);
				} finally {
					response.close();
				}
			}
		});
	}

	public PriceData getPrice(int itemId) {
		return prices.get(itemId);
	}

	public java.util.List<ItemMapping> getItemMappings() {
		return itemMappings;
	}

	@Data
	public static class PriceResponse {
		private Map<Integer, PriceData> data;
	}

	@Data
	public static class PriceData {
		private Integer high;
		private Integer highTime;
		private Integer low;
		private Integer lowTime;
	}

	@Data
	public static class ItemMapping {
		private int id;
		private String name;
		private String examine;
		private Integer limit;
		private boolean members;
		private int lowalch;
		private int highalch;
		private int value;
	}
}

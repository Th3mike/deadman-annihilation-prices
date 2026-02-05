package com.dmmprices;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(name = "Deadman Annihilation Prices", description = "Real-time Deadman: Annihilation prices from the OSRS Wiki - by Seacin", tags = {
		"dmm", "prices", "wiki", "ge", "deadman", "annihilation" })
public class DmmWikiPricePlugin extends Plugin {
	private static final Logger log = LoggerFactory.getLogger(DmmWikiPricePlugin.class);

	@Inject
	private Client client;

	@Inject
	private DmmWikiPriceConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private DmmWikiPriceOverlay overlay;

	@Inject
	private WikiPriceClient wikiPriceClient;

	@Inject
	private ClientToolbar clientToolbar;

	private DmmWikiPricePanel panel;
	private NavigationButton navButton;
	private Instant lastRefresh;

	@Override
	protected void startUp() throws Exception {
		overlayManager.add(overlay);

		wikiPriceClient.fetchPrices();
		wikiPriceClient.fetchMapping();
		lastRefresh = Instant.now();

		panel = injector.getInstance(DmmWikiPricePanel.class);

		BufferedImage icon = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
		java.awt.Graphics2D g = icon.createGraphics();
		g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

		g.setColor(new java.awt.Color(200, 0, 0));
		g.fillOval(5, 2, 10, 11);
		g.fillRect(7, 10, 6, 6);

		g.setColor(java.awt.Color.BLACK);
		g.fillOval(7, 6, 2, 3);
		g.fillOval(11, 6, 2, 3);
		g.fillRect(9, 10, 2, 2);
		g.dispose();

		navButton = NavigationButton.builder()
				.tooltip("Deadman Annihilation Prices")
				.icon(icon)
				.priority(1)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);
		log.info("Deadman Annihilation Prices plugin started!");
	}

	@Override
	protected void shutDown() throws Exception {
		overlayManager.remove(overlay);
		clientToolbar.removeNavigation(navButton);
		log.info("Deadman Annihilation Prices plugin stopped.");
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (lastRefresh == null
				|| Instant.now().isAfter(lastRefresh.plus(config.refreshInterval(), ChronoUnit.MINUTES))) {
			wikiPriceClient.fetchPrices();
			lastRefresh = Instant.now();
		}
	}

	@Provides
	DmmWikiPriceConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(DmmWikiPriceConfig.class);
	}
}

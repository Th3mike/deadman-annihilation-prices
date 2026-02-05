package com.dmmprices;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(name = "Deadman Annihilation Prices", description = "Real-time Deadman: Annihilation prices from the OSRS Wiki - by Seacin", tags = {
		"dmm", "prices", "wiki", "ge", "deadman", "annihilation" })
public class DmmWikiPricePlugin extends Plugin {
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
		// Adiciona o overlay de preço na tela
		overlayManager.add(overlay);

		// Busca os preços e os nomes dos itens na Wiki logo que o plugin abre
		wikiPriceClient.fetchPrices();
		wikiPriceClient.fetchMapping();
		lastRefresh = Instant.now();

		// Inicia o painel lateral de busca
		panel = injector.getInstance(DmmWikiPricePanel.class);

		// Criando o ícone de Caveira Vermelha (estilo DMM) para a barra lateral
		BufferedImage icon = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
		java.awt.Graphics2D g = icon.createGraphics();
		g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

		g.setColor(new java.awt.Color(200, 0, 0)); // Vermelho Carmesim
		g.fillOval(5, 2, 10, 11); // Topo da cabeça
		g.fillRect(7, 10, 6, 6); // Mandíbula

		g.setColor(java.awt.Color.BLACK);
		g.fillOval(7, 6, 2, 3); // Olho Esquerdo
		g.fillOval(11, 6, 2, 3); // Olho Direito
		g.fillRect(9, 10, 2, 2); // Nariz
		g.dispose();

		// Monta o botão na barra lateral
		navButton = NavigationButton.builder()
				.tooltip("Deadman Annihilation Prices")
				.icon(icon)
				.priority(1)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);
		log.info("Plugin Deadman Annihilation iniciado por Seacin!");
	}

	@Override
	protected void shutDown() throws Exception {
		// Limpa tudo ao fechar o plugin pra não bugar o RuneLite
		overlayManager.remove(overlay);
		clientToolbar.removeNavigation(navButton);
		log.info("Plugin Deadman Annihilation parado.");
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

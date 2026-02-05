package com.dmmprices;

import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.text.NumberFormat;
import java.util.Locale;
import javax.inject.Inject;

public class DmmWikiPriceOverlay extends Overlay
{
	private final Client client;
	private final DmmWikiPriceConfig config;
	private final WikiPriceClient wikiPriceClient;
	private final ItemManager itemManager;
	private final PanelComponent panelComponent = new PanelComponent();
	private final NumberFormat quantityFormat = NumberFormat.getIntegerInstance(Locale.US);

	@Inject
	private DmmWikiPriceOverlay(Client client, DmmWikiPriceConfig config, WikiPriceClient wikiPriceClient, ItemManager itemManager)
	{
		this.client = client;
		this.config = config;
		this.wikiPriceClient = wikiPriceClient;
		this.itemManager = itemManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// 1. Target the GE Offer setup interface (465)
		Widget geOfferWindow = client.getWidget(465, 0);
		if (geOfferWindow == null || geOfferWindow.isHidden())
		{
			return null;
		}

		// 2. Identify the item in the offer slot
		// Component 465:21 is the item image/slot in the offer setup
		Widget itemIcon = client.getWidget(465, 21);
		int itemId = -1;

		if (itemIcon != null && !itemIcon.isHidden())
		{
			itemId = itemIcon.getItemId();
		}

		if (itemId <= 0 || itemId == 6512)
		{
			return null;
		}

		// 3. Normalize Item ID (Unnoting it)
		// Many items in GE show up as noted, but Wiki uses unnoted IDs
		int canonicalId = itemManager.canonicalize(itemId);

		// 4. Get the Wiki prices
		WikiPriceClient.PriceData priceData = wikiPriceClient.getPrice(canonicalId);

		panelComponent.getChildren().clear();

		if (priceData == null)
		{
			// If we found an item but no prices, show a small "No Data" tag for feedback
			panelComponent.getChildren().add(TitleComponent.builder()
				.text("No Wiki Data")
				.color(Color.GRAY)
				.build());
		}
		else
		{
			panelComponent.getChildren().add(TitleComponent.builder()
				.text("Wiki (Annihilation)")
				.color(Color.YELLOW)
				.build());

			if (config.showBuyPrice() && priceData.getHigh() != null)
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Buy:")
					.right(quantityFormat.format(priceData.getHigh()))
					.rightColor(Color.GREEN)
					.build());
			}

			if (config.showSellPrice() && priceData.getLow() != null)
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Sell:")
					.right(quantityFormat.format(priceData.getLow()))
					.rightColor(Color.RED)
					.build());
			}
		}

		// 5. Position below the "Buy limit" text (Child 15)
		Widget buyLimitWidget = client.getWidget(465, 15);
		if (buyLimitWidget != null && !buyLimitWidget.isHidden())
		{
			Rectangle bounds = buyLimitWidget.getBounds();
			panelComponent.setPreferredLocation(new java.awt.Point(bounds.x, bounds.y + bounds.height));
		}
		else
		{
			// Fallback next to the item name
			Widget itemNameWidget = client.getWidget(465, 19);
			if (itemNameWidget != null)
			{
				Rectangle bounds = itemNameWidget.getBounds();
				panelComponent.setPreferredLocation(new java.awt.Point(bounds.x, bounds.y + bounds.height + 20));
			}
		}

		panelComponent.setPreferredSize(new Dimension(115, 0));
		return panelComponent.render(graphics);
	}
}

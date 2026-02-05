package com.dmmprices;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("deadmanannihilation")
public interface DmmWikiPriceConfig extends Config {
	@ConfigItem(keyName = "showBuyPrice", name = "Show Wiki Buy Price", description = "Display the latest 'high' price from the Wiki", position = 1)
	default boolean showBuyPrice() {
		return true;
	}

	@ConfigItem(keyName = "showSellPrice", name = "Show Wiki Sell Price", description = "Display the latest 'low' price from the Wiki", position = 2)
	default boolean showSellPrice() {
		return true;
	}

	@ConfigItem(keyName = "refreshInterval", name = "Refresh (min)", description = "How often to fetch new data from the Wiki API", position = 3)
	default int refreshInterval() {
		return 1;
	}
}

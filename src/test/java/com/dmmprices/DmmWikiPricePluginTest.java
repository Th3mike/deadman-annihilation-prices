package com.dmmprices;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class DmmWikiPricePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(DmmWikiPricePlugin.class);
		RuneLite.main(args);
	}
}

package com.alecray.pethunter;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Launches a development RuneLite client with the Pet Hunter plugin loaded. Run this class's
 * {@code main} from your IDE (requires JDK 11+).
 */
public class PetHunterPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(PetHunterPlugin.class);
		RuneLite.main(args);
	}
}

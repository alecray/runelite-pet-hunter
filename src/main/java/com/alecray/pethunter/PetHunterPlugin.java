package com.alecray.pethunter;

import com.alecray.pethunter.data.Pet;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@Slf4j
@PluginDescriptor(
	name = "Pet Hunter",
	description = "Ranks and assigns pet-hunting tasks from the pets you don't yet own",
	tags = {"pet", "pets", "collection", "log", "boss", "skilling"}
)
public class PetHunterPlugin extends Plugin
{
	private static final Pattern NEW_PET_PATTERN =
		Pattern.compile("New item added to your collection log: (.+)");

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private Notifier notifier;

	@Inject
	private PetHunterConfig config;

	@Inject
	private PetDataManager dataManager;

	@Inject
	private CollectionLogReader reader;

	@Inject
	private PetHunterPanel panel;

	private NavigationButton navButton;
	private final Runnable panelRefresh = () -> SwingUtilities.invokeLater(this::refreshPanel);

	@Provides
	PetHunterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PetHunterConfig.class);
	}

	@Override
	protected void startUp()
	{
		panel.setSyncAction(() -> clientThread.invoke(this::manualSync));
		dataManager.addChangeListener(panelRefresh);

		navButton = NavigationButton.builder()
			.tooltip("Pet Hunter")
			.icon(buildIcon())
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		// If we start while already logged in, load this profile's saved state.
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invoke(dataManager::reloadState);
		}
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		dataManager.removeChangeListener(panelRefresh);
		removeSyncButton();
		navButton = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			dataManager.reloadState();
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() != ScriptID.COLLECTION_DRAW_LIST)
		{
			return;
		}
		if (config.autoSync())
		{
			reader.readOpenPage();
		}
		if (config.showSyncButton())
		{
			addSyncButton();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
		{
			return;
		}
		Matcher m = NEW_PET_PATTERN.matcher(event.getMessage());
		if (!m.find())
		{
			return;
		}
		String key = PetNames.key(m.group(1));
		Pet pet = dataManager.getByKey(key);
		if (pet == null)
		{
			return; // not a pet we track (some other collection-log item)
		}
		dataManager.markObtained(Collections.singleton(key));
		if (config.notifyOnNewPet())
		{
			notifier.notify("Pet Hunter: you obtained " + pet.getName() + "!");
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (PetHunterConfig.GROUP.equals(event.getGroup()))
		{
			panelRefresh.run();
		}
	}

	/** Triggered by the side-panel sync button; runs on the client thread. */
	private void manualSync()
	{
		int found = reader.readOpenPage();
		if (found < 0)
		{
			chat("Pet Hunter: open the Collection Log (Other → All Pets) first, then sync.");
		}
		else
		{
			chat("Pet Hunter: synced " + dataManager.obtainedCount() + "/" + dataManager.total() + " pets.");
		}
	}

	private void chat(String message)
	{
		client.addChatMessage(ChatMessageType.CONSOLE, "", message, null);
	}

	private void refreshPanel()
	{
		panel.refresh();
	}

	// ----- in-game collection-log sync button (best-effort, Collection Log Master style) -----

	/**
	 * Inject a clickable "Sync Pets" widget into the collection log title bar. Widget layout differs
	 * between RuneLite versions, so this is wrapped defensively — if the title widget can't be found
	 * the side-panel sync button still works.
	 */
	private void addSyncButton()
	{
		try
		{
			int group = WidgetUtil.componentToInterface(ComponentID.COLLECTION_LOG_ENTRY_ITEMS);
			Widget title = client.getWidget(group, 2);
			if (title == null)
			{
				return;
			}
			// Avoid duplicating the button across redraws.
			if (title.getChildren() != null)
			{
				for (Widget child : title.getChildren())
				{
					if (child != null && "Sync Pets".equals(child.getName()))
					{
						return;
					}
				}
			}

			Widget button = title.createChild(-1, net.runelite.api.widgets.WidgetType.TEXT);
			button.setName("Sync Pets");
			button.setText("Sync Pets");
			button.setTextColor(0xff981f);
			button.setFontId(net.runelite.api.FontID.PLAIN_11);
			button.setTextShadowed(true);
			button.setOriginalWidth(70);
			button.setOriginalHeight(15);
			button.setOriginalX(380);
			button.setOriginalY(4);
			button.setHasListener(true);
			button.setOnOpListener((net.runelite.api.JavaScriptCallback) ev -> reader.readOpenPage());
			button.setAction(0, "Sync pets");
			button.revalidate();
		}
		catch (Exception e)
		{
			log.debug("Pet Hunter: could not inject collection log sync button", e);
		}
	}

	private void removeSyncButton()
	{
		clientThread.invoke(() ->
		{
			try
			{
				int group = WidgetUtil.componentToInterface(ComponentID.COLLECTION_LOG_ENTRY_ITEMS);
				Widget title = client.getWidget(group, 2);
				if (title == null || title.getChildren() == null)
				{
					return;
				}
				for (Widget child : title.getChildren())
				{
					if (child != null && "Sync Pets".equals(child.getName()))
					{
						child.setHidden(true);
						child.revalidate();
					}
				}
			}
			catch (Exception ignored)
			{
				// nothing to clean up
			}
		});
	}

	/** Draw a simple sidebar icon (a paw print) so we don't need to ship a binary asset. */
	private static BufferedImage buildIcon()
	{
		BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setColor(new Color(0xff, 0x98, 0x1f));
		// main pad
		g.fillOval(8, 12, 9, 8);
		// toes
		g.fillOval(5, 7, 4, 5);
		g.fillOval(10, 5, 4, 5);
		g.fillOval(15, 7, 4, 5);
		g.dispose();
		return img;
	}
}

package com.alecray.pethunter;

import com.alecray.pethunter.data.ActivityType;
import com.alecray.pethunter.data.Pet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Sidebar UI for Pet Hunter: shows sync status, filter controls, the pinned "current task", and the
 * filtered/ranked list of pet cards. All mutation goes through {@link PetDataManager} /
 * {@link TaskService}; this class only renders and gathers filter state.
 */
class PetHunterPanel extends PluginPanel
{
	private static final String TAG_ALL = "All tags";

	private final PetDataManager dataManager;
	private final TaskService taskService;
	private final PetHunterConfig config;
	private final PetHunterConfigManager configStore;
	private final ItemManager itemManager;

	private final JLabel statusLabel = new JLabel();
	private final JCheckBox hideObtained = new JCheckBox("Hide obtained");
	private final JComboBox<String> activityCombo = new JComboBox<>(new String[]{"All activities", "Solo", "Group"});
	private final JComboBox<String> tagCombo = new JComboBox<>();
	private final JComboBox<RankMode> rankCombo = new JComboBox<>(RankMode.values());
	private final JPanel taskPanel = new JPanel(new BorderLayout());
	private final JPanel listPanel = new JPanel();

	private Runnable syncAction = () -> {};
	private Pet currentTask;
	private boolean adjusting;

	@Inject
	PetHunterPanel(PetDataManager dataManager, TaskService taskService, PetHunterConfig config,
		PetHunterConfigManager configStore, ItemManager itemManager)
	{
		super(false);
		this.dataManager = dataManager;
		this.taskService = taskService;
		this.config = config;
		this.configStore = configStore;
		this.itemManager = itemManager;

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		add(buildHeader(), BorderLayout.NORTH);

		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
		listPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JScrollPane scroll = new JScrollPane(listPanel,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		add(scroll, BorderLayout.CENTER);

		hideObtained.setSelected(config.hideObtainedByDefault());
		rankCombo.setSelectedItem(config.rankMode());

		hideObtained.addActionListener(e -> refresh());
		activityCombo.addActionListener(e -> refresh());
		tagCombo.addActionListener(e -> { if (!adjusting) refresh(); });
		rankCombo.addActionListener(e -> refresh());

		restoreCurrentTask();
		refresh();
	}

	private JPanel buildHeader()
	{
		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("Pet Hunter");
		title.setForeground(ColorScheme.BRAND_ORANGE);
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setAlignmentX(Component.LEFT_ALIGNMENT);

		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		statusLabel.setFont(FontManager.getRunescapeSmallFont());
		statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JButton syncButton = new JButton("Sync from collection log");
		syncButton.setFont(FontManager.getRunescapeSmallFont());
		syncButton.setToolTipText("Reads the open collection log page. Open Other → All Pets to capture every pet.");
		syncButton.addActionListener(e -> syncAction.run());
		syncButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		syncButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, syncButton.getPreferredSize().height));

		JPanel filters = new JPanel(new GridLayout(0, 1, 0, 3));
		filters.setBackground(ColorScheme.DARK_GRAY_COLOR);
		filters.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
		hideObtained.setBackground(ColorScheme.DARK_GRAY_COLOR);
		hideObtained.setForeground(Color.WHITE);
		filters.add(hideObtained);
		filters.add(labelled("Activity", activityCombo));
		filters.add(labelled("Tag", tagCombo));
		filters.add(labelled("Rank by", rankCombo));
		filters.setAlignmentX(Component.LEFT_ALIGNMENT);

		JButton suggest = new JButton("Suggest next pet");
		suggest.setFont(FontManager.getRunescapeBoldFont());
		suggest.setToolTipText("Pick the top-ranked pet you still need that matches the filters.");
		suggest.addActionListener(e -> suggestNext());
		suggest.setAlignmentX(Component.LEFT_ALIGNMENT);
		suggest.setMaximumSize(new Dimension(Integer.MAX_VALUE, suggest.getPreferredSize().height));

		taskPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		taskPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE.darker()),
			BorderFactory.createEmptyBorder(6, 6, 6, 6)));
		taskPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		header.add(title);
		header.add(statusLabel);
		header.add(box(6));
		header.add(syncButton);
		header.add(filters);
		header.add(box(8));
		header.add(suggest);
		header.add(box(6));
		header.add(taskPanel);
		return header;
	}

	private JPanel labelled(String text, Component field)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JLabel l = new JLabel(text);
		l.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setPreferredSize(new Dimension(54, 0));
		row.add(l, BorderLayout.WEST);
		row.add(field, BorderLayout.CENTER);
		return row;
	}

	private static Component box(int height)
	{
		return javax.swing.Box.createVerticalStrut(height);
	}

	void setSyncAction(Runnable syncAction)
	{
		this.syncAction = syncAction;
	}

	private PetFilter buildFilter()
	{
		ActivityType type = null;
		switch (activityCombo.getSelectedIndex())
		{
			case 1:
				type = ActivityType.SOLO;
				break;
			case 2:
				type = ActivityType.GROUP;
				break;
			default:
				type = null;
		}
		Object tagSel = tagCombo.getSelectedItem();
		String tag = (tagSel == null || TAG_ALL.equals(tagSel)) ? null : tagSel.toString();
		return new PetFilter(hideObtained.isSelected(), type, tag);
	}

	private RankMode rankMode()
	{
		Object sel = rankCombo.getSelectedItem();
		return sel instanceof RankMode ? (RankMode) sel : RankMode.EASIEST_FIRST;
	}

	private void suggestNext()
	{
		Optional<Pet> next = taskService.suggestNext(buildFilter(), rankMode());
		if (next.isPresent())
		{
			setCurrentTask(next.get());
		}
		else
		{
			currentTask = null;
			configStore.setCurrentTaskKey(null);
			renderTask();
		}
	}

	private void setCurrentTask(Pet pet)
	{
		currentTask = pet;
		configStore.setCurrentTaskKey(PetNames.key(pet.getName()));
		renderTask();
	}

	private void restoreCurrentTask()
	{
		String key = configStore.getCurrentTaskKey();
		currentTask = key == null ? null : dataManager.getByKey(key);
	}

	/** Rebuild status, tag dropdown, current-task card, and the pet list. Call on the EDT. */
	void refresh()
	{
		statusLabel.setText("Obtained: " + dataManager.obtainedCount() + " / " + dataManager.total());
		refreshTagCombo();
		renderTask();
		rebuildList();
	}

	private void refreshTagCombo()
	{
		adjusting = true;
		Object selected = tagCombo.getSelectedItem();
		tagCombo.removeAllItems();
		tagCombo.addItem(TAG_ALL);
		for (String tag : dataManager.allKnownTags())
		{
			tagCombo.addItem(tag);
		}
		if (selected != null)
		{
			tagCombo.setSelectedItem(selected);
			if (tagCombo.getSelectedItem() == null)
			{
				tagCombo.setSelectedItem(TAG_ALL);
			}
		}
		adjusting = false;
	}

	private void renderTask()
	{
		taskPanel.removeAll();
		if (currentTask == null)
		{
			JLabel none = new JLabel("No task set — pick one or hit Suggest.");
			none.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			none.setFont(FontManager.getRunescapeSmallFont());
			taskPanel.add(none, BorderLayout.CENTER);
		}
		else
		{
			JLabel header = new JLabel("CURRENT TASK");
			header.setForeground(ColorScheme.BRAND_ORANGE);
			header.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));

			JPanel body = new JPanel(new BorderLayout(6, 0));
			body.setBackground(taskPanel.getBackground());
			JLabel icon = new JLabel();
			icon.setHorizontalAlignment(SwingConstants.CENTER);
			icon.setPreferredSize(new Dimension(36, 32));
			if (currentTask.getItemId() > 0)
			{
				itemManager.getImage(currentTask.getItemId()).addTo(icon);
			}
			body.add(icon, BorderLayout.WEST);

			JPanel text = new JPanel();
			text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
			text.setBackground(taskPanel.getBackground());
			JLabel name = new JLabel(currentTask.getName());
			name.setForeground(currentTask.isObtained() ? new Color(86, 168, 86) : Color.WHITE);
			name.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
			JLabel src = new JLabel(currentTask.getSource());
			src.setForeground(Color.LIGHT_GRAY);
			src.setFont(FontManager.getRunescapeSmallFont());
			String rate = currentTask.getRarity() > 0 ? "1/" + currentTask.getRarity() : "varies";
			JLabel meta = new JLabel(rate + "  •  " + currentTask.getType().getDisplayName()
				+ (currentTask.isObtained() ? "  •  DONE" : ""));
			meta.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			meta.setFont(FontManager.getRunescapeSmallFont());
			text.add(name);
			text.add(src);
			text.add(meta);
			body.add(text, BorderLayout.CENTER);

			taskPanel.add(header, BorderLayout.NORTH);
			taskPanel.add(body, BorderLayout.CENTER);
		}
		taskPanel.revalidate();
		taskPanel.repaint();
	}

	private void rebuildList()
	{
		listPanel.removeAll();
		List<Pet> pets = taskService.rankedCandidates(buildFilter(), rankMode());
		if (pets.isEmpty())
		{
			JLabel empty = new JLabel("No pets match these filters.");
			empty.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			empty.setFont(FontManager.getRunescapeSmallFont());
			empty.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
			listPanel.add(empty);
		}
		else
		{
			for (Pet pet : pets)
			{
				PetCard card = new PetCard(pet, itemManager, this::setCurrentTask, dataManager);
				card.setAlignmentX(Component.LEFT_ALIGNMENT);
				card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
				listPanel.add(card);
			}
		}
		listPanel.revalidate();
		listPanel.repaint();
	}
}

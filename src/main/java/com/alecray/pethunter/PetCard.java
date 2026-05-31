package com.alecray.pethunter;

import com.alecray.pethunter.data.Pet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * One row in the pet list: icon, name, source, rarity/type, obtained state, plus actions to set the
 * pet as the current task and to edit its custom tags.
 */
@Slf4j
class PetCard extends JPanel
{
	private static final Color OBTAINED = new Color(86, 168, 86);

	PetCard(Pet pet, ItemManager itemManager, Consumer<Pet> onSetTask, PetDataManager dataManager)
	{
		setLayout(new BorderLayout(6, 0));
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
			BorderFactory.createEmptyBorder(6, 4, 6, 4)));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		// Icon
		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(36, 32));
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		if (pet.getItemId() > 0)
		{
			AsyncBufferedImage img = itemManager.getImage(pet.getItemId());
			img.addTo(icon);
		}
		add(icon, BorderLayout.WEST);

		// Center: name + source + meta
		JPanel info = new JPanel();
		info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
		info.setBackground(getBackground());

		JLabel name = new JLabel(pet.getName());
		name.setForeground(pet.isObtained() ? OBTAINED : Color.WHITE);
		name.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));

		JLabel source = new JLabel(pet.getSource());
		source.setForeground(Color.LIGHT_GRAY);
		source.setFont(FontManager.getRunescapeSmallFont());

		JLabel meta = new JLabel(metaText(pet));
		meta.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		meta.setFont(FontManager.getRunescapeSmallFont());

		info.add(name);
		info.add(source);
		info.add(meta);
		List<String> tags = pet.allTags();
		if (!tags.isEmpty())
		{
			JLabel tagLabel = new JLabel("#" + String.join(" #", tags));
			tagLabel.setForeground(ColorScheme.BRAND_ORANGE.darker());
			tagLabel.setFont(FontManager.getRunescapeSmallFont());
			info.add(tagLabel);
		}
		add(info, BorderLayout.CENTER);

		// Right: actions
		JPanel actions = new JPanel(new GridLayout(2, 1, 0, 2));
		actions.setBackground(getBackground());

		JButton taskButton = compactButton(pet.isObtained() ? "Owned" : "Task");
		taskButton.setEnabled(!pet.isObtained());
		taskButton.setToolTipText("Set as current task");
		taskButton.addActionListener(e -> onSetTask.accept(pet));

		JButton tagButton = compactButton("Tags");
		tagButton.setToolTipText("Edit custom tags");
		tagButton.addActionListener(e -> editTags(pet, dataManager));

		actions.add(taskButton);
		actions.add(tagButton);
		add(actions, BorderLayout.EAST);

		// Click name/source to open the wiki page.
		MouseAdapter wiki = new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				openWiki(pet);
			}
		};
		name.addMouseListener(wiki);
		name.setToolTipText("Open wiki page");
	}

	private static String metaText(Pet pet)
	{
		String rate = pet.getRarity() > 0 ? "1/" + pet.getRarity() : "varies";
		return rate + "  •  " + pet.getType().getDisplayName();
	}

	private static JButton compactButton(String text)
	{
		JButton b = new JButton(text);
		b.setFont(FontManager.getRunescapeSmallFont());
		b.setMargin(new Insets(1, 4, 1, 4));
		b.setFocusPainted(false);
		return b;
	}

	private void editTags(Pet pet, PetDataManager dataManager)
	{
		String current = String.join(", ", pet.getCustomTags());
		String input = (String) JOptionPane.showInputDialog(
			this,
			"Custom tags for " + pet.getName() + " (comma-separated):",
			"Edit tags",
			JOptionPane.PLAIN_MESSAGE,
			null,
			null,
			current);
		if (input == null)
		{
			return; // cancelled
		}
		List<String> tags = Arrays.stream(input.split(","))
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.collect(Collectors.toList());
		dataManager.setCustomTags(pet, tags);
	}

	private void openWiki(Pet pet)
	{
		if (pet.getWikiUrl() == null || pet.getWikiUrl().isEmpty())
		{
			return;
		}
		try
		{
			if (Desktop.isDesktopSupported())
			{
				Desktop.getDesktop().browse(new URI(pet.getWikiUrl()));
			}
		}
		catch (Exception e)
		{
			log.warn("Pet Hunter: could not open wiki url {}", pet.getWikiUrl(), e);
		}
	}
}

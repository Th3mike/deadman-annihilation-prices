package com.dmmprices;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;

/**
 * Main panel for Deadman Annihilation Prices.
 * Features a vertical price layout and a professional OSRS aesthetic.
 */
public class DmmWikiPricePanel extends PluginPanel {
    private final WikiPriceClient wikiPriceClient;
    private final ItemManager itemManager;
    private final IconTextField searchBar = new IconTextField();
    private final JPanel listContainer = new JPanel();
    private final NumberFormat quantityFormat = NumberFormat.getIntegerInstance(Locale.US);
    private Timer searchTimer;

    private final Font runescapeFont = FontManager.getRunescapeFont();
    private final Font runescapeSmallFont = FontManager.getRunescapeSmallFont();

    @Inject
    private DmmWikiPricePanel(WikiPriceClient wikiPriceClient, ItemManager itemManager) {
        super(false);
        this.wikiPriceClient = wikiPriceClient;
        this.itemManager = itemManager;

        setBorder(new EmptyBorder(15, 10, 10, 10));
        setLayout(new BorderLayout());

        JPanel header = new JPanel();
        header.setLayout(new BorderLayout());
        header.setBorder(new EmptyBorder(0, 0, 15, 0));

        JLabel title = new JLabel("DMM Search");
        title.setFont(runescapeFont.deriveFont(Font.BOLD, 18f));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.NORTH);

        searchBar.setIcon(IconTextField.Icon.SEARCH);
        searchBar.setPreferredSize(new Dimension(100, 35));
        searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchBar.setHoverBackgroundColor(ColorScheme.DARKER_GRAY_HOVER_COLOR);
        // Placeholder removed to avoid using reflection (against RuneLite rules)

        searchBar.addActionListener(e -> updateSearch());
        searchBar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                debounceSearch();
            }
        });
        header.add(searchBar, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);

        listContainer.setLayout(new GridBagLayout());
        listContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JScrollPane scrollPane = new JScrollPane(listContainer);
        scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void debounceSearch() {
        if (searchTimer != null) {
            searchTimer.restart();
        } else {
            searchTimer = new Timer(200, e -> updateSearch());
            searchTimer.setRepeats(false);
            searchTimer.start();
        }
    }

    private void updateSearch() {
        String text = searchBar.getText().toLowerCase().trim();

        SwingUtilities.invokeLater(() -> {
            listContainer.removeAll();

            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1;
            c.gridx = 0;
            c.gridy = 0;
            c.insets = new java.awt.Insets(0, 0, 5, 0);

            if (text.length() < 2) {
                listContainer.revalidate();
                listContainer.repaint();
                return;
            }

            List<WikiPriceClient.ItemMapping> results = wikiPriceClient.getItemMappings().stream()
                    .filter(item -> item.getName().toLowerCase().contains(text))
                    .limit(40)
                    .collect(Collectors.toList());

            for (WikiPriceClient.ItemMapping item : results) {
                listContainer.add(createItemPanel(item), c);
                c.gridy++;
            }

            c.weighty = 1;
            listContainer.add(new JPanel(), c);

            listContainer.revalidate();
            listContainer.repaint();
        });
    }

    private JPanel createItemPanel(WikiPriceClient.ItemMapping item) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
                new EmptyBorder(8, 8, 8, 8)));

        // Icon
        JLabel iconLabel = new JLabel();
        iconLabel.setPreferredSize(new Dimension(32, 32));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        itemManager.getImage(item.getId()).addTo(iconLabel);
        panel.add(iconLabel, BorderLayout.WEST);

        // Center Info Container
        JPanel mainInfo = new JPanel(new BorderLayout());
        mainPanelOpaque(mainInfo);

        // Name (Top)
        JLabel nameLabel = new JLabel(item.getName());
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(runescapeFont);
        mainInfo.add(nameLabel, BorderLayout.NORTH);

        // Content Panel (Bottom Row)
        JPanel content = new JPanel(new BorderLayout());
        mainPanelOpaque(content);
        content.setBorder(new EmptyBorder(2, 0, 0, 0));

        // Prices (Vertical stack)
        JPanel pricesStack = new JPanel(new GridLayout(2, 1));
        mainPanelOpaque(pricesStack);

        WikiPriceClient.PriceData price = wikiPriceClient.getPrice(item.getId());
        if (price != null) {
            String buyStr = (price.getHigh() != null) ? quantityFormat.format(price.getHigh()) : "---";
            String sellStr = (price.getLow() != null) ? quantityFormat.format(price.getLow()) : "---";

            JLabel buyLabel = new JLabel("Buy: " + buyStr);
            buyLabel.setForeground(new Color(125, 255, 125));
            buyLabel.setFont(runescapeSmallFont);

            JLabel sellLabel = new JLabel("Sell: " + sellStr);
            sellLabel.setForeground(new Color(255, 125, 125));
            sellLabel.setFont(runescapeSmallFont);

            pricesStack.add(buyLabel);
            pricesStack.add(sellLabel);
        } else {
            JLabel noData = new JLabel("No Wiki Data");
            noData.setForeground(Color.GRAY);
            noData.setFont(runescapeSmallFont.deriveFont(Font.ITALIC));
            pricesStack.add(noData);
        }
        content.add(pricesStack, BorderLayout.WEST);

        // Limit (Far Right Corner)
        int limit = (item.getLimit() != null) ? item.getLimit() : 0;
        if (limit > 0) {
            JLabel limitLabel = new JLabel("Limit: " + quantityFormat.format(limit));
            limitLabel.setForeground(new Color(0x28, 0x66, 0x93));
            limitLabel.setFont(runescapeSmallFont);
            limitLabel.setVerticalAlignment(SwingConstants.BOTTOM);
            content.add(limitLabel, BorderLayout.EAST);
        }

        mainInfo.add(content, BorderLayout.CENTER);
        panel.add(mainInfo, BorderLayout.CENTER);

        return panel;
    }

    private void mainPanelOpaque(JPanel panel) {
        panel.setOpaque(false);
    }
}

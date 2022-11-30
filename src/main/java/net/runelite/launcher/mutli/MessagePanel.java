package net.runelite.launcher.mutli;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.basic.BasicProgressBarUI;
import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.launcher.ColorScheme;
import net.runelite.launcher.LauncherProperties;


@Getter
class MessagePanel extends JPanel
{
	private static final Dimension PANEL_SIZE = new Dimension(SplashScreenMultipleOptions.FRAME_SIZE.width - InfoPanel.PANEL_SIZE.width, SplashScreenMultipleOptions.FRAME_SIZE.height);
	private static final Dimension BAR_SIZE = new Dimension(PANEL_SIZE.width, 30);
	private static final int MESSAGE_AREA_PADDING = 15;

	private final JLabel titleLabel = new JLabel("Welcome to");
	private final JLabel messageArea;
	private final JLabel bootstrapChannel;
	private final JLabel barLabel = new JLabel("Doing something important");
	private final JProgressBar bar = new JProgressBar(0, 100);

	@Getter(AccessLevel.NONE)
	private final JScrollPane scrollPane;
	private final JPanel buttonPanel;
	private final List<JButton> buttons = new ArrayList<>();


	MessagePanel(List<JButton> buttons)
	{
		this.setPreferredSize(PANEL_SIZE);
		this.setLayout(new GridBagLayout());
		this.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.NORTH;
		c.weightx = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.ipady = 25;

		titleLabel.setText("Welcome to " + LauncherProperties.getApplicationName());
		// main message
		titleLabel.setFont(new Font(FontManager.getRunescapeFont().getName(), FontManager.getRunescapeFont().getStyle(), 32));
		titleLabel.setHorizontalAlignment(JLabel.CENTER);
		titleLabel.setForeground(Color.WHITE);
		this.add(titleLabel, c);
		c.gridy++;

		// alternate message action
		messageArea = messageArea("Open-source client for Old School RuneScape with more functionality and fewer restrictions.");

		scrollPane = new JScrollPane(messageArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());
		final JViewport scrollPaneViewport = scrollPane.getViewport();
		scrollPaneViewport.setForeground(Color.WHITE);
		scrollPaneViewport.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		scrollPaneViewport.setOpaque(true);

		c.weighty = 1;
		c.fill = 1;
		this.add(scrollPane, c);
		c.gridy++;

		bootstrapChannel = messageArea("Please Select the Client you wish to run, any beta or alpha clients may be unstable due to expermental code. Thanks.");

		this.add(bootstrapChannel, c);
		c.gridy++;

		buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(1, 2, 10, 10));
		buttonPanel.setBorder(new EmptyBorder(50, 10, 50, 10));
		buttonPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		buttonPanel.setOpaque(true);

		buttons.forEach(buttonPanel::add);


		this.add(buttonPanel, c);
		c.gridy++;

		c.weighty = 0;
		c.weightx = 1;
		c.ipady = 5;

		barLabel.setFont(FontManager.getRunescapeFont());
		barLabel.setHorizontalAlignment(JLabel.CENTER);
		barLabel.setForeground(Color.WHITE);
		barLabel.setBorder(new EmptyBorder(5, 0, 5, 0));
		this.add(barLabel, c);
		c.gridy++;
		bar.setFont(FontManager.getRunescapeFont());
		bar.setBackground(ColorScheme.BRAND_TRANSPARENT.darker());
		bar.setForeground(ColorScheme.BRAND);
		bar.setMinimumSize(BAR_SIZE);
		bar.setMaximumSize(BAR_SIZE);
		bar.setBorder(new MatteBorder(0, 0, 0, 0, Color.LIGHT_GRAY));
		bar.setUI(new BasicProgressBarUI()
		{
			protected Color getSelectionBackground()
			{
				return ColorScheme.DARKER_GRAY_COLOR;
			}

			protected Color getSelectionForeground()
			{
				return ColorScheme.DARKER_GRAY_COLOR;
			}
		});
		bar.setFont(FontManager.getRunescapeFont());
		bar.setVisible(true);
		this.add(bar, c);
		c.gridy++;
	}

	private JLabel messageArea(String message)
	{
		JLabel messageArea = new JLabel("<html><div style='text-align:center;'>" + message + "</div></html>")
		{
			@Override
			public Dimension getPreferredSize()
			{
				final Dimension results = super.getPreferredSize();
				results.width = PANEL_SIZE.width - MESSAGE_AREA_PADDING;
				return results;
			}
		};
		messageArea.setFont(new Font(FontManager.getRunescapeFont().getName(), FontManager.getRunescapeSmallFont().getStyle(), 16));
		messageArea.setForeground(Color.WHITE);
		messageArea.setBorder(new EmptyBorder(0, MESSAGE_AREA_PADDING, 0, MESSAGE_AREA_PADDING));

		return messageArea;
	}

	void setMessageContent(String content)
	{
		if (content != null && !content.startsWith("<html"))
		{
			content = "<html><div style='text-align:center;'>" + content + "</div></html>";
		}

		messageArea.setText(content);
		messageArea.revalidate();
		messageArea.repaint();
	}

	void setMessageTitle(String text)
	{
		titleLabel.setText(text);
		titleLabel.revalidate();
		titleLabel.repaint();
	}

}
package net.runelite.launcher.mutli;


import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.launcher.ColorScheme;
import net.runelite.launcher.LinkBrowser;
import net.runelite.launcher.SplashScreen;


import static net.runelite.launcher.Launcher.*;

@Slf4j
class InfoPanel extends JPanel
{
	private static final Color DARK_GREY = new Color(10, 10, 10, 255);

	private static BufferedImage TRANSPARENT_LOGO;
	static final Dimension PANEL_SIZE = new Dimension(200, SplashScreenMultipleOptions.FRAME_SIZE.height);

	private static final Dimension VERSION_SIZE = new Dimension(PANEL_SIZE.width, 25);

	private static final String TROUBLESHOOTING_URL = "https://github.com/runelite/runelite/wiki/Troubleshooting-problems-with-the-client";
	private static final String DISCORD_INVITE_LINK = "https://discord.gg/cSYfh6rs48";


	InfoPanel(String mode)
	{
		this.setLayout(new GridBagLayout());
		this.setPreferredSize(PANEL_SIZE);
		this.setBackground(new Color(38, 38, 38));

		final GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.ipady = 5;
		try
		{
			TRANSPARENT_LOGO = ImageIO.read(SplashScreen.class.getResourceAsStream("runelite_transparent.png"));
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		// Logo
		final ImageIcon transparentLogo = new ImageIcon();
		if (TRANSPARENT_LOGO != null)
		{
			transparentLogo.setImage(TRANSPARENT_LOGO.getScaledInstance(128, 128, Image.SCALE_SMOOTH));
		}
		final JLabel logo = new JLabel(transparentLogo);

		c.anchor = GridBagConstraints.NORTH;
		c.weighty = 1;
		this.add(logo, c);
		c.gridy++;
		c.anchor = GridBagConstraints.SOUTH;
		c.weighty = 0;


		final JLabel logsFolder = createPanelButton("Open logs folder", null, () -> LinkBrowser.openLocalFile(LOGS_DIR));
		this.add(logsFolder, c);
		c.gridy++;

		final JLabel discord = createPanelButton("Get help on Discord", "Instant invite link to join the OpenOSRS discord", () -> LinkBrowser.browse(DISCORD_INVITE_LINK));
		this.add(discord, c);
		c.gridy++;

		final JLabel troubleshooting = createPanelButton("Troubleshooting steps", "Opens a link to the troubleshooting wiki", () -> LinkBrowser.browse(TROUBLESHOOTING_URL));
		this.add(troubleshooting, c);
		c.gridy++;

		final JLabel exit = createPanelButton("Exit", "Closes the application immediately", () -> System.exit(0));
		this.add(exit, c);
		c.gridy++;
	}

	private static JLabel createPanelTextButton(final String title)
	{
		final JLabel textButton = new JLabel(title);
		textButton.setFont(FontManager.getRunescapeSmallFont());
		textButton.setHorizontalAlignment(JLabel.CENTER);
		textButton.setForeground(ColorScheme.BRAND);
		textButton.setBackground(null);
		textButton.setPreferredSize(VERSION_SIZE);
		textButton.setMinimumSize(VERSION_SIZE);
		textButton.setBorder(new MatteBorder(1, 0, 0, 0, DARK_GREY));

		return textButton;
	}

	private static JLabel createPanelTextButton(final String title, final Runnable runnable)
	{
		final JLabel textButton = new JLabel(title);
		textButton.setFont(FontManager.getRunescapeSmallFont());
		textButton.setHorizontalAlignment(JLabel.CENTER);
		textButton.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
		textButton.setBackground(null);
		textButton.setPreferredSize(VERSION_SIZE);
		textButton.setMinimumSize(VERSION_SIZE);
		textButton.setBorder(new MatteBorder(1, 0, 0, 0, DARK_GREY));
		textButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		textButton.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				runnable.run();
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				textButton.setBackground(new Color(60, 60, 60));
				textButton.repaint();
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				textButton.setBackground(null);
				textButton.repaint();
			}
		});

		return textButton;
	}

	private static JLabel createPanelButton(final String name, final String tooltip, final Runnable runnable)
	{
		final JLabel btn = new JLabel(name, JLabel.CENTER);
		btn.setToolTipText(tooltip);
		btn.setOpaque(true);
		btn.setBackground(null);
		btn.setForeground(Color.WHITE);

		btn.setBorder(new CompoundBorder(
			new MatteBorder(1, 0, 0, 0, DARK_GREY),
			new EmptyBorder(3, 0, 3, 0))
		);
		btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btn.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				runnable.run();
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				btn.setBackground(new Color(60, 60, 60));
				btn.repaint();
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				btn.setBackground(null);
				btn.repaint();
			}
		});

		return btn;
	}
}
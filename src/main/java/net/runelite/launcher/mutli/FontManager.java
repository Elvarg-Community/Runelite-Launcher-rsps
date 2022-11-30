package net.runelite.launcher.mutli;

import net.runelite.launcher.Launcher;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import javax.swing.text.StyleContext;

public class FontManager
{
	private static Font runescapeFont;
	private static Font runescapeSmallFont;

	public static void init()
	{
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

		try
		{
			Font font = Font.createFont(Font.TRUETYPE_FONT,
					Launcher.class.getResourceAsStream("runescape.ttf"))
				.deriveFont(Font.PLAIN, 16);
			ge.registerFont(font);

			runescapeFont = StyleContext.getDefaultStyleContext()
				.getFont(font.getName(), Font.PLAIN, 16);
			ge.registerFont(runescapeFont);

			Font smallFont = Font.createFont(Font.TRUETYPE_FONT,
					Launcher.class.getResourceAsStream("runescape_small.ttf"))
				.deriveFont(Font.PLAIN, 16);
			ge.registerFont(smallFont);

			runescapeSmallFont = StyleContext.getDefaultStyleContext()
				.getFont(smallFont.getName(), Font.PLAIN, 16);
			ge.registerFont(runescapeSmallFont);
		}
		catch (FontFormatException ex)
		{
			throw new RuntimeException("Font loaded, but format incorrect.", ex);
		}
		catch (IOException ex)
		{
			throw new RuntimeException("Font file not found.", ex);
		}
	}

	static Font getRunescapeFont()
	{
		return runescapeFont;
	}

	static Font getRunescapeSmallFont()
	{
		return runescapeSmallFont;
	}
}
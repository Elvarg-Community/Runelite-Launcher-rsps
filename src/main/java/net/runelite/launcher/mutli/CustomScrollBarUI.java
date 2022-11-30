package net.runelite.launcher.mutli;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import lombok.Setter;
import net.runelite.launcher.ColorScheme;


public class CustomScrollBarUI extends BasicScrollBarUI
{

	/* The background color of the bar's thumb */
	@Setter
	private Color thumbColor = ColorScheme.MEDIUM_GRAY_COLOR;

	/* The background color of the bar's track */
	@Setter
	private Color trackColor = ColorScheme.SCROLL_TRACK_COLOR;

	/**
	 * Overrides the painting of the bar's track (the darker part underneath that extends
	 * the full page length).
	 */
	@Override
	protected void paintTrack(Graphics graphics, JComponent jComponent, Rectangle rectangle)
	{
		graphics.setColor(trackColor);
		graphics.fillRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
	}

	/**
	 * Overrides the painting of the bar's thumb (the lighter part on top that users
	 * use to slide up and down the page).
	 */
	@Override
	protected void paintThumb(Graphics graphics, JComponent jComponent, Rectangle rectangle)
	{
		graphics.setColor(thumbColor);
		graphics.fillRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
	}

	/**
	 * Creates an empty JButton to be used as the scroll bar's arrows (to disable the arrows).
	 */
	private JButton createEmptyButton()
	{
		JButton button = new JButton();
		Dimension zeroDim = new Dimension(0, 0);
		button.setPreferredSize(zeroDim);
		button.setMinimumSize(zeroDim);
		button.setMaximumSize(zeroDim);
		return button;
	}

	public static ComponentUI createUI(JComponent c)
	{
		JScrollBar bar = (JScrollBar) c;
		bar.setUnitIncrement(16);
		bar.setPreferredSize(new Dimension(7, 0));
		return new CustomScrollBarUI();
	}

	/**
	 * Applies an empty button to the decrease (down arrow) button.
	 */
	@Override
	protected JButton createDecreaseButton(int orientation)
	{
		return createEmptyButton();
	}

	/**
	 * Applies an empty button to the increase (up arrow) button.
	 */
	@Override
	protected JButton createIncreaseButton(int orientation)
	{
		return createEmptyButton();
	}
}
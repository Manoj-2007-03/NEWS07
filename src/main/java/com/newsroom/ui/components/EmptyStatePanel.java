package com.newsroom.ui.components;

import com.newsroom.theme.ThemeColors;
import com.newsroom.theme.ThemeManager;
import com.newsroom.util.UIUtils;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Reusable empty state view for empty searches, saved lists, or history.
 * Demonstrates Component Reusability and Encapsulation.
 */
public class EmptyStatePanel extends JPanel {
    private final JLabel iconLabel;
    private final JLabel titleLabel;
    private final JLabel descriptionLabel;

    public EmptyStatePanel(String symbol, String title, String description) {
        this(symbol, title, description, null, null);
    }

    public EmptyStatePanel(String symbol, String title, String description, String actionText, Runnable onAction) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);

        ThemeColors colors = ThemeManager.getInstance().getColors();

        add(Box.createVerticalGlue());

        // Symbol / Monogram
        iconLabel = new JLabel(symbol);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        iconLabel.setFont(UIUtils.getSansFont(Font.PLAIN, 42));
        iconLabel.setForeground(colors.textMuted);
        add(iconLabel);
        add(Box.createVerticalStrut(14));

        // Title
        titleLabel = new JLabel(title);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(UIUtils.getEditorialFont(Font.BOLD, 20));
        titleLabel.setForeground(colors.textPrimary);
        add(titleLabel);
        add(Box.createVerticalStrut(8));

        // Description
        descriptionLabel = new JLabel("<html><center style='width: 320px;'>" + description + "</center></html>");
        descriptionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        descriptionLabel.setFont(UIUtils.getSansFont(Font.PLAIN, 13));
        descriptionLabel.setForeground(colors.textSecondary);
        add(descriptionLabel);

        if (actionText != null && onAction != null) {
            add(Box.createVerticalStrut(18));
            RoundedButton actionBtn = new RoundedButton(actionText, RoundedButton.Style.PRIMARY);
            actionBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            actionBtn.setPreferredSize(new Dimension(180, 36));
            actionBtn.setMaximumSize(new Dimension(180, 36));
            actionBtn.addActionListener(e -> onAction.run());
            add(actionBtn);
        }

        add(Box.createVerticalGlue());
    }

    public void updateTheme(ThemeColors colors) {
        if (iconLabel != null) iconLabel.setForeground(colors.textMuted);
        if (titleLabel != null) titleLabel.setForeground(colors.textPrimary);
        if (descriptionLabel != null) descriptionLabel.setForeground(colors.textSecondary);
    }
}

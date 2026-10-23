package com.newsroom.ui;

import com.newsroom.interfaces.ArticleNavigationListener;
import com.newsroom.interfaces.BookmarkService;
import com.newsroom.interfaces.NewsService;
import com.newsroom.interfaces.ReadingHistoryService;
import com.newsroom.model.HistoryEntry;
import com.newsroom.model.ReaderSettings;
import com.newsroom.theme.ThemeColors;
import com.newsroom.theme.ThemeManager;
import com.newsroom.ui.components.CompactArticleCard;
import com.newsroom.ui.components.EmptyStatePanel;
import com.newsroom.ui.components.RoundedButton;
import com.newsroom.util.DateTimeFormatterUtil;
import com.newsroom.util.UIUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

/**
 * Reading History view displaying chronological access logs.
 * Demonstrates Collections integration and Session State Tracking.
 */
public class HistoryPanel extends BaseViewPanel {
    private JPanel listContainer;
    private JLabel countBadge;

    public HistoryPanel(NewsService newsService,
                        BookmarkService bookmarkService,
                        ReadingHistoryService historyService,
                        ArticleNavigationListener navListener,
                        ReaderSettings settings) {
        super(newsService, bookmarkService, historyService, navListener, settings);
        initUI();
    }

    private void initUI() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setBorder(BorderFactory.createEmptyBorder(20, 28, 36, 28));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Reading History");
        title.setFont(UIUtils.getEditorialFont(Font.BOLD, 28));
        title.setForeground(ThemeManager.getInstance().getColors().textPrimary);
        left.add(title);
        left.add(Box.createVerticalStrut(4));

        JLabel desc = new JLabel("Chronological log of articles accessed during this session.");
        desc.setFont(UIUtils.getSansFont(Font.PLAIN, 13));
        desc.setForeground(ThemeManager.getInstance().getColors().textSecondary);
        left.add(desc);

        header.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        countBadge = new JLabel("0 Articles Read");
        countBadge.setFont(UIUtils.getSansFont(Font.BOLD, 12));
        countBadge.setForeground(ThemeManager.getInstance().getColors().accent);
        right.add(countBadge);

        RoundedButton clearBtn = new RoundedButton("Clear History", RoundedButton.Style.SECONDARY, 6);
        clearBtn.setPreferredSize(new Dimension(110, 32));
        clearBtn.addActionListener(e -> {
            if (historyService.getHistoryCount() == 0) return;
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to clear your session reading history?",
                    "Clear Reading History",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                historyService.clearHistory();
                refreshView();
            }
        });
        right.add(clearBtn);

        header.add(right, BorderLayout.EAST);

        container.add(header);
        container.add(Box.createVerticalStrut(24));

        // List Container
        listContainer = new JPanel();
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setOpaque(false);
        container.add(listContainer);

        add(wrapInScrollPane(container), BorderLayout.CENTER);
        refreshView();
    }

    @Override
    public void refreshView() {
        if (listContainer == null) return;
        listContainer.removeAll();

        List<HistoryEntry> history = historyService.getHistory();
        countBadge.setText(history.size() + " Articles Read");

        if (history.isEmpty()) {
            listContainer.add(new EmptyStatePanel(
                    "◷",
                    "No Reading History Yet",
                    "Articles you open and read in this session will appear here in chronological order.",
                    "Start Reading News",
                    navListener::onNavigateToHome
            ));
        } else {
            for (HistoryEntry entry : history) {
                listContainer.add(new CompactArticleCard(entry.getArticle(), navListener, bookmarkService));
                listContainer.add(Box.createVerticalStrut(8));
            }
        }

        listContainer.revalidate();
        listContainer.repaint();
    }

    @Override
    protected void onThemeUpdated(ThemeColors colors) {
        if (countBadge != null) countBadge.setForeground(colors.accent);
        refreshView();
    }
}

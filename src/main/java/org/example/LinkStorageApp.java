package org.example;


import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;



public class LinkStorageApp extends JFrame {
    private JTextField linkField;
    private JButton addButton;
    private JPanel linkPanel;
    private List<String> linkList;
    private DatabaseManager databaseManager;
    private OkHttpClient httpClient;
    private JButton pasteLinkButton;
    private JLabel titleLabel; // Add this JLabel for displaying video titles

    public LinkStorageApp() {
        setTitle("Website Link Storage");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        linkList = new ArrayList<>();
        databaseManager = new DatabaseManager();

        linkField = new JTextField(30);
        addButton = new JButton("Add Link");
        pasteLinkButton = new JButton("Paste Link");
        linkPanel = new JPanel();
        linkPanel.setLayout(new BoxLayout(linkPanel, BoxLayout.Y_AXIS));
        titleLabel = new JLabel(); // Initialize the JLabel

        addButton.addActionListener(e -> addLink());
        pasteLinkButton.addActionListener(e -> pasteLink());
        httpClient = new OkHttpClient();

        linkField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    addLink();
                }
            }
        });

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        inputPanel.add(linkField, BorderLayout.NORTH);
        inputPanel.add(addButton, BorderLayout.CENTER);
        inputPanel.add(pasteLinkButton, BorderLayout.SOUTH);

        add(inputPanel, BorderLayout.NORTH);
        add(new JScrollPane(linkPanel), BorderLayout.CENTER);

        linkList.addAll(loadLinksFromDatabase());
        displayLinks();
    }

    private void displayLinks() {
        linkPanel.removeAll();
        for (String link : linkList) {
            addLinkPanel(link);
        }
        linkPanel.revalidate();
        linkPanel.repaint();
    }

    // Inside the LinkStorageApp class
    private void addLinkPanel(String link) {
        JPanel linkEntryPanel = new JPanel();
        linkEntryPanel.setLayout(new BoxLayout(linkEntryPanel, BoxLayout.Y_AXIS));

        JLabel linkLabel = new JLabel("<html><a href=\"" + link + "\">" + link + "</a></html>");
        JButton deleteButton = new JButton("Delete");

        JPanel titlePanel = new JPanel();
        JLabel titleLabel = new JLabel();
        titlePanel.add(titleLabel);

        JPanel thumbnailPanel = new JPanel();
        JLabel thumbnailLabel = new JLabel();
        thumbnailPanel.add(thumbnailLabel); // Add the thumbnail label

        deleteButton.addActionListener(e -> {
            int index = linkList.indexOf(link);
            if (index != -1) {
                linkList.remove(index);
                deleteLinkFromDatabase(link);
                displayLinks();
            }
        });

        linkEntryPanel.add(linkLabel);
        linkEntryPanel.add(deleteButton);
        linkEntryPanel.add(titlePanel);
        linkEntryPanel.add(thumbnailPanel); // Add the thumbnail panel

        String youtubeVideoId = extractYouTubeVideoId(link);
        if (youtubeVideoId != null) {
            String videoTitle = fetchYouTubeVideoTitle(youtubeVideoId);
            titleLabel.setText("Video Title: " + videoTitle);

            String thumbnailUrl = fetchYouTubeVideoThumbnailUrl(youtubeVideoId);
            if (thumbnailUrl != null) {
                ImageIcon thumbnailIcon = fetchImageIcon(thumbnailUrl);
                thumbnailLabel.setIcon(thumbnailIcon); // Set the thumbnail image
            }
        }

        linkPanel.add(linkEntryPanel);
    }
    private String fetchYouTubeVideoThumbnailUrl(String videoId) {
        String apiKey = "YOUR_YOUTUBE_API_KEY"; // Replace with your actual YouTube Data API key
        String apiUrl = "https://www.googleapis.com/youtube/v3/videos?id=" + videoId +
                "&key=" + apiKey + "&part=snippet";

        try {
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .build();

            Response response = httpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                JSONObject jsonResponse = new JSONObject(response.body().string());
                JSONArray items = jsonResponse.getJSONArray("items");
                if (items.length() > 0) {
                    JSONObject videoItem = items.getJSONObject(0);
                    JSONObject snippet = videoItem.getJSONObject("snippet");
                    JSONObject thumbnails = snippet.getJSONObject("thumbnails");
                    JSONObject mediumThumbnail = thumbnails.getJSONObject("medium");
                    return mediumThumbnail.getString("url");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    private ImageIcon fetchImageIcon(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            Image image = ImageIO.read(url);
            return new ImageIcon(image);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void addLink() {
        String link = linkField.getText();
        if (!link.isEmpty()) {
            linkList.add(link);
            addLinkPanel(link);
            saveLinkToDatabase(link);
            linkField.setText("");
        }
    }


    private List<String> loadLinksFromDatabase() {
        List<String> links = new ArrayList<>();
        try {
            var connection = databaseManager.getConnection();
            if (connection != null) {
                var statement = connection.prepareStatement("SELECT link FROM links");
                var resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    links.add(resultSet.getString("link"));
                }
                resultSet.close();
                statement.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return links;
    }

    private void saveLinkToDatabase(String link) {
        try {
            var connection = databaseManager.getConnection();
            if (connection != null) {
                var statement = connection.prepareStatement("INSERT INTO links (link) VALUES (?)");
                statement.setString(1, link);
                statement.executeUpdate();
                statement.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteLinkFromDatabase(String link) {
        try {
            var connection = databaseManager.getConnection();
            if (connection != null) {
                var statement = connection.prepareStatement("DELETE FROM links WHERE link = ?");
                statement.setString(1, link);
                statement.executeUpdate();
                statement.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private class LinkMouseListener extends MouseAdapter {
        private String link;

        public LinkMouseListener(String link) {
            this.link = link;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            openWebPage(link);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            linkPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public void mouseExited(MouseEvent e) {
            linkPanel.setCursor(Cursor.getDefaultCursor());
        }
    }

    private void openWebPage(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pasteLink() {
        try {
            // Get the clipboard contents
            java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            String clipboardData = (String) clipboard.getData(DataFlavor.stringFlavor);

            // Check if the clipboard data is a valid link
            if (clipboardData != null && clipboardData.startsWith("http")) {
                linkField.setText(clipboardData);
                addLink();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String extractYouTubeVideoId(String link) {
        String videoId = null;
        if (link.contains("youtube.com/watch?v=")) {
            videoId = link.substring(link.indexOf("youtube.com/watch?v=") + 20);
        } else if (link.contains("youtu.be/")) {
            videoId = link.substring(link.indexOf("youtu.be/") + 9);
        }
        if (videoId != null) {
            int endIndex = videoId.indexOf('&');
            if (endIndex != -1) {
                videoId = videoId.substring(0, endIndex);
            }
        }
        return videoId;
    }

    private String fetchYouTubeVideoTitle(String videoId) {
        String apiKey = "YOUR_YOUTUBE_API_KEY"; // Replace with your actual YouTube Data API key
        String apiUrl = "https://www.googleapis.com/youtube/v3/videos?id=" + videoId +
                "&key=" + apiKey + "&part=snippet";

        try {
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .build();

            Response response = httpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                JSONObject jsonResponse = new JSONObject(response.body().string());
                if (jsonResponse.has("items")) {
                    JSONObject videoItem = jsonResponse.getJSONArray("items").getJSONObject(0);
                    JSONObject snippet = videoItem.getJSONObject("snippet");
                    return snippet.getString("title");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "YouTube Video - " + videoId;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LinkStorageApp().setVisible(true));
    }
}

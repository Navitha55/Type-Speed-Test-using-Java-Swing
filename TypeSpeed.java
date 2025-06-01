import javax.swing.*;
import javax.swing.Timer;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class TypeSpeed extends JFrame {
    JButton pause = new JButton("Pause");
    JButton restart = new JButton("Restart");
    JButton next = new JButton("Next");

    JTextArea targetArea;
    JTextPane inputPane;
    JLabel statsLabel = new JLabel("WPM: 0 | Accuracy: 0%");

    java.util.List<String> paragraphs = new ArrayList<>();
    int currentIndex = 0;
    String targetText = "";

    Timer timer;
    boolean started = false;
    long startTime;
    int elapsedSeconds = 0;

    int totalTypedChars = 0;
    int totalCorrectChars = 0;
    int totalTime = 0;

    public TypeSpeed() {
        setTitle("Type Speed Test");
        setSize(600, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        next.setEnabled(false); // Disable initially

        targetArea = new JTextArea();
        targetArea.setEditable(false);
        targetArea.setFont(new Font("Serif", Font.BOLD, 18));
        targetArea.setLineWrap(true);
        targetArea.setWrapStyleWord(true);
        add(new JScrollPane(targetArea), BorderLayout.NORTH);

        inputPane = new JTextPane();
        inputPane.setFont(new Font("Serif", Font.PLAIN, 18));
        add(new JScrollPane(inputPane), BorderLayout.CENTER);

        statsLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        statsLabel.setForeground(new Color(0, 102, 204));
        statsLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel foot = new JPanel(new GridLayout(2, 1));
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(pause);
        buttonPanel.add(restart);
        buttonPanel.add(next);
        foot.add(buttonPanel);
        foot.add(statsLabel);
        add(foot, BorderLayout.SOUTH);

        pause.addActionListener(e -> pause());
        restart.addActionListener(e -> restart());
        next.addActionListener(e -> loadNextParagraph());

        inputPane.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                if (!started) {
                    startTime = System.currentTimeMillis();
                    started = true;
                    elapsedSeconds = 0;
                    timer.start();
                }
                highlightCharacters();
                updateStats();
                checkCompletion();
            }
        });

        timer = new Timer(1000, e -> {
            elapsedSeconds++;
            updateStats();
        });

        loadParagraphsFromFile("paragraphs.txt");
        loadNextParagraph();

        setVisible(true);
    }

    private void loadParagraphsFromFile(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    paragraphs.add(line.trim());
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading file!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadNextParagraph() {
        if (currentIndex >= paragraphs.size()) {
            return; // Final result already shown
        }

        targetText = paragraphs.get(currentIndex);
        targetArea.setText(targetText);
        inputPane.setText("");
        inputPane.setEditable(true);
        inputPane.requestFocus();

        pause.setText("Pause");
        started = false;
        elapsedSeconds = 0;
        statsLabel.setText("WPM: 0 | Accuracy: 0%");
        next.setEnabled(false); // Disable next until user finishes

        if (timer.isRunning()) timer.stop();
    }


    private void restart() {
        currentIndex = 0;
        totalTypedChars = 0;
        totalCorrectChars = 0;
        totalTime = 0;
        loadNextParagraph();
    }

    private void pause() {
        if (pause.getText().equals("Pause")) {
            pause.setText("Resume");
            inputPane.setEditable(false);
            timer.stop();
        } else {
            pause.setText("Pause");
            inputPane.setEditable(true);
            timer.start();
        }
    }

    private void highlightCharacters() {
        StyledDocument doc = inputPane.getStyledDocument();
        StyleContext sc = StyleContext.getDefaultStyleContext();

        AttributeSet green = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, new Color(0, 153, 0));
        AttributeSet red = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.RED);
        AttributeSet black = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.BLACK);

        String typed = inputPane.getText();
        doc.setCharacterAttributes(0, typed.length(), black, true);

        int len = Math.min(typed.length(), targetText.length());
        for (int i = 0; i < len; i++) {
            if (typed.charAt(i) == targetText.charAt(i)) {
                doc.setCharacterAttributes(i, 1, green, true);
            } else {
                doc.setCharacterAttributes(i, 1, red, true);
            }
        }

        if (typed.length() > targetText.length()) {
            doc.setCharacterAttributes(targetText.length(), typed.length() - targetText.length(), red, true);
        }
    }

    private void updateStats() {
        String typed = inputPane.getText();
        int correct = 0;
        for (int i = 0; i < Math.min(typed.length(), targetText.length()); i++) {
            if (typed.charAt(i) == targetText.charAt(i)) correct++;
        }

        double accuracy = typed.length() > 0 ? (correct * 100.0 / typed.length()) : 0;
        double minutes = elapsedSeconds / 60.0;
        int wpm = minutes > 0 ? (int) ((typed.length() / 5.0) / minutes) : 0;

        statsLabel.setText(String.format("⏱ WPM: %d   |   🎯 Accuracy: %.2f%%", wpm, accuracy));
    }

    private void checkCompletion() {
    String typedText = inputPane.getText();
    if (typedText.equals(targetText)) {
        timer.stop();
        inputPane.setEditable(false);
        next.setEnabled(true); // enable next button

        int correct = 0;
        for (int i = 0; i < targetText.length(); i++) {
            if (typedText.charAt(i) == targetText.charAt(i)) correct++;
        }

        int typedLength = typedText.length();
        double accuracy = typedLength > 0 ? (correct * 100.0 / typedLength) : 0;
        double minutes = elapsedSeconds / 60.0;
        int wpm = minutes > 0 ? (int) ((typedLength / 5.0) / minutes) : 0;

        // Update totals
        totalCorrectChars += correct;
        totalTypedChars += typedLength;
        totalTime += elapsedSeconds;

        String resultMessage = String.format("""
            ✅ Paragraph Completed!

            ⏱ Time: %d sec
            📈 WPM: %d
            🎯 Accuracy: %.2f%%
        """, elapsedSeconds, wpm, accuracy);

        JOptionPane.showMessageDialog(this, resultMessage, "Result", JOptionPane.INFORMATION_MESSAGE);

        currentIndex++;

        if (currentIndex >= paragraphs.size()) {
            showFinalResult(); // full summary after last paragraph
        }
    }
}


    private void showFinalResult() {
        double accuracy = totalTypedChars > 0 ? (totalCorrectChars * 100.0 / totalTypedChars) : 0;
        double minutes = totalTime / 60.0;
        int wpm = minutes > 0 ? (int) ((totalTypedChars / 5.0) / minutes) : 0;

        String message = String.format("""
            🎉 All Paragraphs Completed!

            ✅ Average WPM: %d
            🎯 Overall Accuracy: %.2f%%
            🕒 Total Time: %d seconds
        """, wpm, accuracy, totalTime);

        JOptionPane.showMessageDialog(this, message, "Final Result", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TypeSpeed::new);
    }
}

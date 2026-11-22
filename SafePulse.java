import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;

/**
 * SAFEPULSE - Smart Women Safety System (College Mini Project)
 *
 * Features:
 *  1. SOS button (TEST SMS MODE - simulated, no real SMS is sent)
 *  2. Emergency contacts (add / delete, saved in contacts.txt)
 *  3. Safe Journey check-in timer (auto-SOS if user does not confirm)
 *  4. Live approximate location (IP based) + Google Maps link
 *  5. Alert history (saved in history.txt)
 *  6. Safety tips and emergency helpline numbers
 */
public class SafePulse extends JFrame {

    // ---------- DATA ----------
    ArrayList<String> contacts = new ArrayList<>();
    String currentLatitude = null;
    String currentLongitude = null;
    String currentMapsLink = null;

    // ---------- UI ----------
    private JLabel locationStatus;

    private static final Color BG = new Color(20, 22, 35);
    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    SafePulse() {

        setTitle("SAFEPULSE - Women Safety System");
        setSize(900, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        loadContacts();

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(BG);
        mainPanel.setLayout(null);

        // Title
        JLabel title = new JLabel("SAFEPULSE");
        title.setBounds(40, 25, 300, 50);
        title.setFont(new Font("Arial", Font.BOLD, 30));
        title.setForeground(Color.WHITE);
        mainPanel.add(title);

        // Subtitle
        JLabel subtitle = new JLabel("SMART WOMEN SAFETY SYSTEM");
        subtitle.setBounds(43, 70, 350, 25);
        subtitle.setForeground(Color.LIGHT_GRAY);
        mainPanel.add(subtitle);

        // Welcome message
        JLabel welcome = new JLabel("Welcome to SafePulse!");
        welcome.setBounds(40, 120, 400, 40);
        welcome.setFont(new Font("Arial", Font.BOLD, 24));
        welcome.setForeground(Color.WHITE);
        mainPanel.add(welcome);

        // Location status (updated automatically)
        locationStatus = new JLabel("Location: Detecting...");
        locationStatus.setBounds(40, 165, 600, 25);
        locationStatus.setFont(new Font("Arial", Font.PLAIN, 14));
        locationStatus.setForeground(Color.YELLOW);
        mainPanel.add(locationStatus);

        // SOS button
        JButton sos = new JButton("SOS - EMERGENCY");
        sos.setBounds(300, 210, 280, 90);
        sos.setFont(new Font("Arial", Font.BOLD, 22));
        sos.setBackground(new Color(220, 45, 65));
        sos.setForeground(Color.WHITE);
        sos.setOpaque(true);
        sos.setBorderPainted(false);
        sos.setFocusPainted(false);
        sos.addActionListener(e -> runTestSmsMode());
        mainPanel.add(sos);

        // Feature buttons panel (3 rows x 2 columns = 6 buttons)
        JPanel featurePanel = new JPanel();
        featurePanel.setBounds(100, 330, 700, 220);
        featurePanel.setLayout(new GridLayout(3, 2, 20, 15));
        featurePanel.setBackground(BG);

        JButton contactButton = new JButton("Emergency Contacts");
        contactButton.addActionListener(e -> contactWindow());

        JButton timer = new JButton("Safety Check-in");
        timer.addActionListener(e -> safetyTimer());

        JButton tips = new JButton("Safety Tips");
        tips.addActionListener(e -> showSafetyTips());

        JButton historyButton = new JButton("Alert History");
        historyButton.addActionListener(e -> showHistory());

        JButton location = new JButton("Current Location");
        location.addActionListener(e -> getCurrentLocation(false));

        JButton helplines = new JButton("Emergency Helplines");
        helplines.addActionListener(e -> showHelplines());

        featurePanel.add(contactButton);
        featurePanel.add(timer);
        featurePanel.add(tips);
        featurePanel.add(historyButton);
        featurePanel.add(location);
        featurePanel.add(helplines);

        mainPanel.add(featurePanel);
        add(mainPanel);

        setVisible(true);

        // Fetch location in the background so SOS messages include it
        getCurrentLocation(true);
    }

    // =====================================================
    //  SAFETY TIPS & HELPLINES
    // =====================================================
    void showSafetyTips() {

        String safetyTips =
            "       SAFEPULSE - SAFETY TIPS\n\n" +
            "1. Always save emergency contact numbers.\n\n" +
            "2. Share your travel plans with trusted people.\n\n" +
            "3. Stay alert and aware of your surroundings.\n\n" +
            "4. Avoid isolated places whenever possible.\n\n" +
            "5. Keep your phone charged while travelling.\n\n" +
            "6. Use trusted transportation services.\n\n" +
            "7. In an emergency, call 112 (India).";

        JOptionPane.showMessageDialog(
            this,
            safetyTips,
            "Personal Safety Tips",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    void showHelplines() {

        String text =
            "      EMERGENCY HELPLINES (INDIA)\n\n" +
            "National Emergency Number : 112\n" +
            "Police                    : 100\n" +
            "Women Helpline            : 1091\n" +
            "Women Helpline (Domestic) : 181\n" +
            "Ambulance                 : 102 / 108\n" +
            "Cyber Crime Helpline      : 1930";

        JOptionPane.showMessageDialog(
            this,
            text,
            "SAFEPULSE - Helplines",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    // =====================================================
    //  EMERGENCY CONTACTS
    // =====================================================
    void contactWindow() {

        JFrame frame = new JFrame("Emergency Contacts");
        frame.setSize(450, 400);
        frame.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout());

        DefaultListModel<String> model = new DefaultListModel<>();

        for (String contact : contacts) {
            model.addElement(contact);
        }

        JList<String> list = new JList<>(model);

        JTextField name = new JTextField(12);
        JTextField phone = new JTextField(12);

        JButton add = new JButton("Add Contact");
        JButton delete = new JButton("Delete Selected");

        JPanel input = new JPanel(new GridLayout(3, 2, 5, 5));

        input.add(new JLabel("Name:"));
        input.add(name);
        input.add(new JLabel("Phone:"));
        input.add(phone);
        input.add(add);
        input.add(delete);

        add.addActionListener(e -> {

            String n = name.getText().trim();
            String p = phone.getText().trim();

            if (n.isEmpty() || !p.matches("[0-9]{10}")) {
                JOptionPane.showMessageDialog(frame,
                    "Enter a name and valid 10-digit phone number.");
                return;
            }

            String contact = n + " - " + p;

            contacts.add(contact);
            model.addElement(contact);

            saveContacts();

            name.setText("");
            phone.setText("");
        });

        delete.addActionListener(e -> {

            int index = list.getSelectedIndex();

            if (index >= 0) {
                contacts.remove(index);
                model.remove(index);
                saveContacts();
            } else {
                JOptionPane.showMessageDialog(frame,
                    "Select a contact to delete.");
            }
        });

        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        panel.add(input, BorderLayout.SOUTH);

        frame.add(panel);
        frame.setVisible(true);
    }

    void saveContacts() {

        try (PrintWriter writer =
             new PrintWriter(new FileWriter("contacts.txt"))) {

            for (String contact : contacts) {
                writer.println(contact);
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Unable to save contacts.");
        }
    }

    void loadContacts() {

        File file = new File("contacts.txt");

        if (!file.exists()) return;

        try (BufferedReader reader =
             new BufferedReader(new FileReader(file))) {

            String line;

            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    contacts.add(line);
                }
            }

        } catch (IOException e) {
            System.out.println("Unable to load contacts.");
        }
    }

    public ArrayList<String> getEmergencyPhoneNumbers() {

        ArrayList<String> numbers = new ArrayList<>();

        for (String contact : contacts) {

            int separator = contact.lastIndexOf(" - ");

            if (separator != -1) {

                String phone = contact.substring(separator + 3).trim();

                if (phone.matches("[0-9]{10}")) {
                    numbers.add("+91" + phone);
                }
            }
        }

        return numbers;
    }

    public int contactsCount() {
        return getEmergencyPhoneNumbers().size();
    }

    // =====================================================
    //  SAFE JOURNEY CHECK-IN TIMER
    //  Phase 0 : journey timer counting down
    //  Phase 1 : 10 second grace period to press "I AM SAFE"
    //            -> if not pressed, SOS is triggered automatically
    // =====================================================
    public void safetyTimer() {

        final int GRACE_SECONDS = 10;

        JFrame frame = new JFrame("Safe Journey Mode");
        frame.setSize(400, 350);
        frame.setLocationRelativeTo(this);
        frame.setLayout(new GridLayout(3, 2, 20, 15));

        JLabel title = new JLabel("SAFE JOURNEY CHECK-IN", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));

        JLabel countdown = new JLabel("Select your timer", JLabel.CENTER);
        countdown.setFont(new Font("Arial", Font.BOLD, 20));

        String[] options = {"1 Minute", "2 Minutes", "5 Minutes"};
        int[] durations = {60, 120, 300};

        JComboBox<String> timerChoice = new JComboBox<>(options);

        JButton start = new JButton("START JOURNEY");
        JButton safe = new JButton("I AM SAFE");
        safe.setEnabled(false);

        final Timer[] timer = new Timer[1];
        final int[] seconds = {0};
        final int[] phase = {0};

        start.addActionListener(e -> {

            if (timer[0] != null && timer[0].isRunning()) {
                return;
            }

            seconds[0] = durations[timerChoice.getSelectedIndex()];
            phase[0] = 0;

            start.setEnabled(false);
            safe.setEnabled(true);   // user may confirm safe any time

            addHistory("Safe Journey Started - " +
                       timerChoice.getSelectedItem());

            countdown.setText(String.format("Time Left: %02d:%02d",
                seconds[0] / 60, seconds[0] % 60));

            timer[0] = new Timer(1000, event -> {

                seconds[0]--;

                if (phase[0] == 0) {

                    countdown.setText(String.format("Time Left: %02d:%02d",
                        seconds[0] / 60, seconds[0] % 60));

                    if (seconds[0] <= 0) {
                        phase[0] = 1;
                        seconds[0] = GRACE_SECONDS;
                        addHistory("Safety Check-in Deadline Reached");
                        Toolkit.getDefaultToolkit().beep();
                    }

                } else {

                    countdown.setText("CONFIRM SAFETY! " + seconds[0] + "s");

                    if (seconds[0] <= 0) {
                        timer[0].stop();
                        countdown.setText("No response - SOS triggered!");
                        safe.setEnabled(false);
                        start.setEnabled(true);
                        addHistory("No Check-in Response - SOS Triggered");
                        runTestSmsMode();
                    }
                }
            });

            timer[0].start();
        });

        safe.addActionListener(e -> {

            if (timer[0] != null && timer[0].isRunning()) {
                timer[0].stop();
            }

            addHistory("Safety Check-in Completed");
            countdown.setText("You confirmed that you are safe!");

            JOptionPane.showMessageDialog(
                frame,
                "Safety check-in completed successfully!"
            );

            safe.setEnabled(false);
            start.setEnabled(true);
        });

        // Stop the timer if the window is closed
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (timer[0] != null && timer[0].isRunning()) {
                    timer[0].stop();
                    addHistory("Safe Journey Window Closed - Timer Cancelled");
                }
            }
        });

        frame.add(title);
        frame.add(timerChoice);
        frame.add(countdown);
        frame.add(start);
        frame.add(safe);

        frame.setVisible(true);
    }

    // =====================================================
    //  EMERGENCY MESSAGE + SOS (TEST SMS MODE)
    // =====================================================
    public String buildEmergencyMessage() {

        String time = LocalDateTime.now().format(TIME_FORMAT);

        String message =
            "EMERGENCY ALERT - SAFEPULSE\n\n" +
            "I need immediate assistance.\n" +
            "Please contact me immediately.\n\n" +
            "Time: " + time;

        if (currentLatitude != null && currentLongitude != null) {

            message +=
                "\n\nCURRENT LOCATION:\n" +
                "Latitude: " + currentLatitude + "\n" +
                "Longitude: " + currentLongitude + "\n" +
                "Google Maps: " + currentMapsLink;

        } else {

            message += "\n\nLocation: Not Available";
        }

        return message;
    }

    public void runTestSmsMode() {

        ArrayList<String> numbers = getEmergencyPhoneNumbers();

        if (numbers.isEmpty()) {

            JOptionPane.showMessageDialog(
                this,
                "No emergency contacts found!\nAdd contacts first.",
                "SAFEPULSE",
                JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        showSmsDashboard();

        String message = buildEmergencyMessage();

        StringBuilder result = new StringBuilder();

        result.append("SAFEPULSE - TEST SMS MODE\n\n");
        result.append("Emergency message prepared successfully!\n\n");
        result.append("RECIPIENT STATUS:\n\n");

        for (String number : numbers) {
            result.append(number)
                  .append(" : SIMULATED - NOT SENT\n");
        }

        result.append("\nMESSAGE:\n\n");
        result.append(message);

        result.append("\n\nTOTAL CONTACTS: ").append(numbers.size());
        result.append("\nSMS STATUS: TEST MODE");
        result.append("\nREAL SMS SENT: NO");

        addHistory(
            "SOS Triggered (Test SMS Mode) - " +
            numbers.size() + " contacts - Not Sent"
        );

        JOptionPane.showMessageDialog(
            this,
            result.toString(),
            "SAFEPULSE - TEST SMS RESULT",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    public void showSmsDashboard() {

        JFrame frame = new JFrame("SAFEPULSE - SMS Dashboard");

        frame.setSize(500, 450);
        frame.setLocationRelativeTo(this);

        JPanel panel = new JPanel();
        panel.setBackground(BG);
        panel.setLayout(null);

        JLabel title = new JLabel("SMS STATUS DASHBOARD");
        title.setBounds(70, 25, 360, 40);
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        panel.add(title);

        JLabel mode = new JLabel("MODE: TEST SMS");
        mode.setBounds(70, 85, 300, 30);
        mode.setFont(new Font("Arial", Font.BOLD, 16));
        mode.setForeground(Color.YELLOW);
        panel.add(mode);

        JLabel contactsLabel = new JLabel(
            "Total Emergency Contacts: " + contactsCount()
        );
        contactsLabel.setBounds(70, 140, 350, 30);
        contactsLabel.setForeground(Color.WHITE);
        panel.add(contactsLabel);

        JLabel status = new JLabel("SMS Status: SIMULATED");
        status.setBounds(70, 190, 350, 30);
        status.setForeground(Color.GREEN);
        status.setFont(new Font("Arial", Font.BOLD, 16));
        panel.add(status);

        JLabel sent = new JLabel("Actual SMS Sent: NO");
        sent.setBounds(70, 240, 300, 30);
        sent.setForeground(Color.RED);
        panel.add(sent);

        JLabel message = new JLabel("Emergency Message: PREPARED");
        message.setBounds(70, 290, 380, 30);
        message.setForeground(Color.WHITE);
        panel.add(message);

        JButton close = new JButton("CLOSE");
        close.setBounds(160, 350, 150, 40);
        close.addActionListener(e -> frame.dispose());
        panel.add(close);

        frame.add(panel);
        frame.setVisible(true);
    }

    // =====================================================
    //  HISTORY
    // =====================================================
    public void addHistory(String activity) {

        String time = LocalDateTime.now().format(TIME_FORMAT);
        String record = time + " - " + activity;

        try (PrintWriter writer = new PrintWriter(
                new FileWriter("history.txt", true))) {

            writer.println(record);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Unable to save history!");
        }
    }

    public void showHistory() {

        JFrame frame = new JFrame("SafePulse - Alert History");
        frame.setSize(550, 400);
        frame.setLocationRelativeTo(this);

        DefaultListModel<String> model = new DefaultListModel<>();

        File file = new File("history.txt");

        if (file.exists()) {

            try (BufferedReader reader =
                    new BufferedReader(new FileReader(file))) {

                String line;

                while ((line = reader.readLine()) != null) {
                    model.addElement(line);
                }

            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Unable to load history!");
            }
        }

        if (model.isEmpty()) {
            model.addElement("No alert history yet.");
        }

        JList<String> list = new JList<>(model);
        list.setFont(new Font("Arial", Font.PLAIN, 14));

        JButton clear = new JButton("Clear History");
        clear.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(
                frame, "Delete all history?", "Confirm",
                JOptionPane.YES_NO_OPTION);

            if (ok == JOptionPane.YES_OPTION) {
                new File("history.txt").delete();
                model.clear();
                model.addElement("No alert history yet.");
            }
        });

        frame.add(new JScrollPane(list), BorderLayout.CENTER);
        frame.add(clear, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    // =====================================================
    //  LOCATION (IP based, approximate)
    //  silent = true  -> background fetch at startup (no popups)
    //  silent = false -> user clicked the button (show popups)
    // =====================================================
    public void getCurrentLocation(boolean silent) {

        if (!silent) {
            locationStatus.setText("Location: Detecting...");
            locationStatus.setForeground(Color.YELLOW);
        }

        new SwingWorker<String, Void>() {

            @Override
            protected String doInBackground() throws Exception {

                HttpClient client = HttpClient.newHttpClient();

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ipapi.co/json/"))
                    .header("User-Agent", "SafePulse/1.0")
                    .GET()
                    .build();

                HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() != 200) {
                    throw new IOException("Location service unavailable.");
                }

                return response.body();
            }

            @Override
            protected void done() {

                try {
                    String data = get();

                    String city = extractField(data, "city");
                    String state = extractField(data, "region");
                    String country = extractField(data, "country_name");

                    String latitude = extractNumber(data, "latitude");
                    String longitude = extractNumber(data, "longitude");

                    if (latitude == null || longitude == null) {
                        throw new Exception("Location details unavailable.");
                    }

                    String mapsLink =
                        "https://www.google.com/maps?q="
                        + latitude + "," + longitude;

                    currentLatitude = latitude;
                    currentLongitude = longitude;
                    currentMapsLink = mapsLink;

                    locationStatus.setText(
                        "Location: " + city + ", " + state + ", " + country
                        + "  (" + latitude + ", " + longitude + ")");
                    locationStatus.setForeground(Color.GREEN);

                    if (silent) return;

                    int choice = JOptionPane.showConfirmDialog(
                        SafePulse.this,
                        "CURRENT LOCATION\n\n" +
                        "City: " + city + "\n" +
                        "State: " + state + "\n" +
                        "Country: " + country + "\n" +
                        "Latitude: " + latitude + "\n" +
                        "Longitude: " + longitude +
                        "\n\nOpen Google Maps?",
                        "SAFEPULSE - LOCATION",
                        JOptionPane.YES_NO_OPTION
                    );

                    if (choice == JOptionPane.YES_OPTION) {
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().browse(URI.create(mapsLink));
                        } else {
                            JOptionPane.showMessageDialog(
                                SafePulse.this,
                                "Cannot open browser.\nLink: " + mapsLink);
                        }
                    }

                } catch (Exception e) {

                    locationStatus.setText(
                        "Location: Not available (check internet)");
                    locationStatus.setForeground(Color.RED);

                    if (!silent) {
                        JOptionPane.showMessageDialog(
                            SafePulse.this,
                            "Unable to fetch location.\n" +
                            "Check your internet connection.",
                            "Location Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            }

        }.execute();
    }

    private String extractField(String data, String key) {

        Pattern pattern = Pattern.compile(
            "\"" + key + "\"\\s*:\\s*\"([^\"]*)\""
        );

        Matcher matcher = pattern.matcher(data);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return "Unavailable";
    }

    private String extractNumber(String data, String key) {

        Pattern pattern = Pattern.compile(
            "\"" + key + "\"\\s*:\\s*(-?[0-9.]+)"
        );

        Matcher matcher = pattern.matcher(data);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    // =====================================================
    //  MAIN
    // =====================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(SafePulse::new);
    }
}

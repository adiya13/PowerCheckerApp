import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import com.sun.management.OperatingSystemMXBean;

public class Main extends JFrame {
    private JComboBox<String> processComboBox;
    private JButton refreshButton;
    private JTextArea outputArea;
    private JButton startButton;
    private JButton stopButton;
    private boolean isMonitoring = false;
    private Thread monitorThread;

    public Main() {
        // Set up the frame
        super("Process Power Monitor");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create north panel with GridBagLayout for better control
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Create components
        JLabel processLabel = new JLabel("Select Process:");
        processComboBox = new JComboBox<>();
        processComboBox.setPreferredSize(new Dimension(300, 25));
        refreshButton = new JButton("Refresh List");

        // Create a separate panel for buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        startButton = new JButton("Start Monitoring");
        stopButton = new JButton("Stop Monitoring");
        stopButton.setEnabled(false);
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);

        // Add components to top panel
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        topPanel.add(processLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        topPanel.add(processComboBox, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        topPanel.add(refreshButton, gbc);

        // Add button panel in a new row
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        topPanel.add(buttonPanel, gbc);

        // Create output area
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        // Add panels to frame
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Create status bar
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel statusLabel = new JLabel("Status: Ready");
        statusPanel.add(statusLabel);
        add(statusPanel, BorderLayout.SOUTH);

        // Add action listeners
        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshProcessList();
                statusLabel.setText("Status: Process list refreshed");
            }
        });

        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (startMonitoring()) {
                    statusLabel.setText("Status: Monitoring active");
                }
            }
        });

        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopMonitoring();
                statusLabel.setText("Status: Monitoring stopped");
            }
        });

        // Initial process list
        refreshProcessList();

        // Display the window
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void refreshProcessList() {
        SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return getRunningProcesses();
            }

            @Override
            protected void done() {
                try {
                    List<String> processes = get();
                    processComboBox.setModel(new DefaultComboBoxModel<>(processes.toArray(new String[0])));
                    if (processes.size() > 0) {
                        processComboBox.setSelectedIndex(0);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(Main.this,
                            "Error retrieving process list: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private List<String> getRunningProcesses() {
        TreeSet<String> processes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER); // For alphabetical order

        try {
            ProcessBuilder builder = new ProcessBuilder("tasklist", "/FO", "CSV");
            Process process = builder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            // Skip header
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                // Parse CSV format, extract just the process name
                if (line.length() > 0) {
                    // CSV format has quotes, remove them and get first field (process name)
                    String[] parts = line.split(",");
                    if (parts.length > 0) {
                        String processName = parts[0].replaceAll("\"", "").trim();
                        processes.add(processName);
                    }
                }
            }
            reader.close();

        } catch (Exception e) {
            outputArea.append("Error getting process list: " + e.getMessage() + "\n");
        }

        return new ArrayList<>(processes);
    }

    private boolean startMonitoring() {
        if (isMonitoring) return false;

        Object selectedItem = processComboBox.getSelectedItem();
        if (selectedItem == null) {
            JOptionPane.showMessageDialog(this, "Please select a process to monitor");
            return false;
        }

        final String processName = selectedItem.toString();
        outputArea.setText("Starting monitoring for: " + processName + "\n");
        isMonitoring = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        processComboBox.setEnabled(false);
        refreshButton.setEnabled(false);

        monitorThread = new Thread(new Runnable() {
            public void run() {
                monitorProcess(processName);
            }
        });
        monitorThread.start();
        return true;
    }

    private void stopMonitoring() {
        isMonitoring = false;
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        processComboBox.setEnabled(true);
        refreshButton.setEnabled(true);

        if (monitorThread != null) {
            monitorThread.interrupt();
            outputArea.append("Monitoring stopped.\n");
        }
    }

    private void monitorProcess(String processName) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        while (isMonitoring) {
            try {
                ProcessBuilder builder = new ProcessBuilder("tasklist", "/FI", "IMAGENAME eq " + processName);
                Process process = builder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                boolean found = false;
                StringBuilder matchedProcessInfo = new StringBuilder();

                // Skip the first two lines (header)
                reader.readLine();
                reader.readLine();

                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty() && !line.contains("No tasks")) {
                        found = true;
                        matchedProcessInfo.append(line).append("\n");
                    }
                }
                reader.close();

                final String timestamp = dateFormat.format(new Date());

                if (found) {
                    double cpuUsage = getCPUUsage();
                    double estimatedPower = estimatePowerConsumption(cpuUsage);

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            outputArea.append("\n[" + timestamp + "]\n");
                            outputArea.append("Process Found:\n" + matchedProcessInfo.toString());
                            outputArea.append("CPU Usage: " + String.format("%.2f", cpuUsage) + "%\n");
                            outputArea.append("Estimated Power Consumption: " + String.format("%.2f", estimatedPower) + "W\n");
                            outputArea.append("-------------------------------------------\n");

                            // Auto-scroll to bottom
                            outputArea.setCaretPosition(outputArea.getDocument().getLength());
                        }
                    });
                } else {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            outputArea.append("\n[" + timestamp + "]\n");
                            outputArea.append(processName + " not found or has stopped.\n");
                            outputArea.append("-------------------------------------------\n");
                            outputArea.setCaretPosition(outputArea.getDocument().getLength());
                        }
                    });
                }

                Thread.sleep(5000); // Check every 5 seconds

            } catch (InterruptedException ie) {
                // Thread was interrupted, exit the loop
                break;
            } catch (Exception e) {
                final String errorMsg = e.getMessage();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        outputArea.append("Error: " + errorMsg + "\n");
                        outputArea.setCaretPosition(outputArea.getDocument().getLength());
                    }
                });
            }
        }
    }

    private static double getCPUUsage() {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        return osBean.getProcessCpuLoad() * 100; // CPU usage in percentage
    }

    private static double estimatePowerConsumption(double cpuUsage) {
        double basePower = 10.0; // Idle power consumption in watts
        double maxPower = 65.0;  // Maximum CPU power consumption in watts
        return basePower + ((maxPower - basePower) * (cpuUsage / 100));
    }

    public static void main(String[] args) {
        // Set look and feel to match the OS
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Start the application on the Event Dispatch Thread
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new Main();
            }
        });
    }
}
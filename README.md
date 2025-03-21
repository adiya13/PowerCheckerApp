# PowerCheckerApp
Windows application power consumption checking application made in JAVA

Greenie Web Software Engineering / Software Development Internship Test Task - Diya Sarkar

1. Introduction
Power consumption monitoring is critical for system optimization. This Java application monitors CPU power consumption of Windows processes in real-time, helping developers and system administrators understand energy usage patterns of specific applications.

2. System Requirements
Windows 10/11 (64-bit)
JDK 21 or later
Windows Tasklist Utility (pre-installed)

3. Program Overview
This application:
Displays a dropdown list of all running processes
Monitors selected processes using Windows tasklist
Measures CPU load via Java's OperatingSystemMXBean
Estimates power consumption using a configurable model
Provides real-time monitoring with a user-friendly interface

4. Key Features
Process Selection: Dropdown menu of all running processes
Real-time Monitoring: Updates every 5 seconds
Power Estimation: Calculates approximate wattage based on CPU usage
Dynamic Process List: Refresh button to update available processes
User Controls: Start/Stop monitoring functionality

5. Code Implementation
5.1 Core Components
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;

5.2 Process Monitoring
ProcessBuilder builder = new ProcessBuilder("tasklist", "/FI", 
    "IMAGENAME eq " + processName);
Process process = builder.start();
BufferedReader reader = new BufferedReader(
    new InputStreamReader(process.getInputStream()));
String line;
boolean found = false;
while ((line = reader.readLine()) != null) {
    if (!line.trim().isEmpty() && !line.contains("No tasks")) {
        found = true;
        // Process information found
    }
}

5.3 CPU Usage Measurement
private static double getCPUUsage() {
    OperatingSystemMXBean osBean = (OperatingSystemMXBean) 
        ManagementFactory.getOperatingSystemMXBean();
    return osBean.getProcessCpuLoad() * 100; // CPU usage percentage
}

5.4 Power Consumption Estimation
private static double estimatePowerConsumption(double cpuUsage) {
    double basePower = 10.0; // Idle consumption (watts)
    double maxPower = 65.0;  // Maximum consumption (watts)
    return basePower + ((maxPower - basePower) * (cpuUsage / 100));
}

5.5 Process List Retrieval
private List<String> getRunningProcesses() {
    TreeSet<String> processes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    ProcessBuilder builder = new ProcessBuilder("tasklist", "/FO", "CSV");
    Process process = builder.start();
    BufferedReader reader = new BufferedReader(
        new InputStreamReader(process.getInputStream()));
    // Parse and store process names
    return new ArrayList<>(processes);
}

6. User Interface
The application provides a clean GUI with:
Process selection dropdown
Refresh list button
Start/Stop monitoring buttons
Output display area for monitoring results
Status bar for system feedback

7. Output Breakdown
Field
Example
Description
Timestamp
2025-03-21 10:15:30
Date and time of measurement
Process Name
chrome.exe
Name of monitored application
PID
12345
Process identifier
Session
Console
Session where process is running
Session Number
2
Identifier for the session
Memory
234,584 K
RAM usage in kilobytes
CPU Usage
27.43%
Current CPU utilization
Power Consumption
25.08W
Estimated power usage


8. Usage Instructions

Launch the application by running the code.
Select a process from the dropdown menu
Click "Start Monitoring" to begin tracking
View real-time updates in the output area
Click "Stop Monitoring" to end tracking
Select a different process and restart monitoring if desired

9. Conclusion
This application provides a practical tool for monitoring process power consumption through a simple, intuitive interface. While estimates are based on CPU load rather than direct hardware measurements, they offer valuable insights into application energy usage patterns.

package com.myscheduler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

public class CommandScheduler {

	private static final String COMMANDS_FILE = "D:\\tmp\\commands.txt";
	private static final Map<Integer, List<String>> recurringCommands = new HashMap<>();
	private static final List<ScheduledCommand> oneTimeCommands = new ArrayList<>();

	public static void main(String[] args) {
		System.out.println("Scheduler Started At:"+LocalDateTime.now());
		parseCommandsFile();
		scheduleRecurringCommands();
		scheduleOneTimeCommands();
	}

	private static void parseCommandsFile() {
		try (BufferedReader br = new BufferedReader(new FileReader(COMMANDS_FILE))) {
			String line;
			while ((line = br.readLine()) != null) {
				line = line.strip();
				if (line.isBlank() || line.startsWith("#"))
					continue;

				if (line.startsWith("*/")) {
					int n = Integer.parseInt(line.substring(2, line.indexOf(' ')).trim());
					String cmd = line.substring(line.indexOf(' ') + 1).trim();

					recurringCommands.computeIfAbsent(n, k -> new ArrayList<>()).add(cmd);

				} else {
					String[] parts = line.split(" ", 6);
					if (parts.length == 6) {
						int minute = Integer.parseInt(parts[0]);
						int hour = Integer.parseInt(parts[1]);
						int day = Integer.parseInt(parts[2]);
						int month = Integer.parseInt(parts[3]);
						int year = Integer.parseInt(parts[4]);
						String cmd = parts[5];
						oneTimeCommands.add(new ScheduledCommand(minute, hour, day, month, year, cmd));
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Error reading file: " + e.getMessage());
		}
	}

	/*
	 * private static void scheduleRecurringCommands() { ScheduledExecutorService
	 * scheduler = Executors.newScheduledThreadPool(recurringCommands.size());
	 * 
	 * for (Map.Entry<Integer, List<String>> entry : recurringCommands.entrySet()) {
	 * int interval = entry.getKey(); List<String> commands = entry.getValue();
	 * 
	 * scheduler.scheduleAtFixedRate(() -> { for (String cmd : commands) {
	 * runCommand(cmd); } }, 1, interval, TimeUnit.MINUTES); // Starts after 1 min
	 * delay } }
	 */
	private static void scheduleRecurringCommands() {
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		int startMinute = LocalDateTime.now().getMinute();
		
		scheduler.scheduleAtFixedRate(() -> {
			int currentMin = LocalDateTime.now().getMinute();
			int diff = currentMin - startMinute;

			if (diff <= 0) {
				return;
			}
			for (Map.Entry<Integer, List<String>> entry : recurringCommands.entrySet()) {
				int interval = entry.getKey();
				if (diff % interval == 0) {
					for (String cmd : entry.getValue()) {
						System.out.println("[ " + LocalDateTime.now() + " ] Running: " + cmd);
					}
				}
			}
		}, 0, 1, TimeUnit.MINUTES);
	}

	private static void scheduleOneTimeCommands() {
		ScheduledExecutorService oneTimeScheduler = Executors.newScheduledThreadPool(1);

		for (ScheduledCommand sc : oneTimeCommands) {
			long delayMillis = sc.getDelayMillis();
			if (delayMillis > 0) {
				oneTimeScheduler.schedule(
						() -> System.out.println("[ " + LocalDateTime.now() + " ] Running: " + sc.command), delayMillis,
						TimeUnit.MILLISECONDS);
			}
		}
	}

	/*
	 * private static void runCommand(String command) {
	 * 
	 * System.out.println("[ " + LocalDateTime.now() + " ] Running: " + command);
	 * 
	 * }
	 */

	private record ScheduledCommand(int minute, int hour, int day, int month, int year, String command) {
		public long getDelayMillis() {
			LocalDateTime now = LocalDateTime.now();
			LocalDateTime targetTime = LocalDateTime.of(year, month, day, hour, minute);
			return Math.max(0, java.time.Duration.between(now, targetTime).toMillis());
		}
	}
}
package com.esprit.services;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Calendar Export Service - Generates ICS files for reservations
 * Compatible with Google Calendar, Outlook, Apple Calendar
 */
public class CalendarService {

    private static final DateTimeFormatter ICS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    /**
     * Generate ICS content for a reservation
     */
    public static String generateICS(String eventName, String location, String description,
                                     LocalDateTime start, LocalDateTime end, String confirmationCode) {
        StringBuilder ics = new StringBuilder();
        ics.append("BEGIN:VCALENDAR\r\n");
        ics.append("VERSION:2.0\r\n");
        ics.append("PRODID:-//TABAANI SmartTravel//Reservation//FR\r\n");
        ics.append("CALSCALE:GREGORIAN\r\n");
        ics.append("METHOD:PUBLISH\r\n");
        ics.append("BEGIN:VEVENT\r\n");
        ics.append("DTSTART:").append(start.format(ICS_FORMAT)).append("\r\n");
        ics.append("DTEND:").append(end.format(ICS_FORMAT)).append("\r\n");
        ics.append("SUMMARY:").append(escapeICS(eventName)).append("\r\n");
        if (location != null) ics.append("LOCATION:").append(escapeICS(location)).append("\r\n");
        ics.append("DESCRIPTION:").append(escapeICS(description != null ? description : "")).append("\r\n");
        ics.append("UID:").append(confirmationCode != null ? confirmationCode : "tabaani-" + System.currentTimeMillis()).append("@tabaani.tn\r\n");
        ics.append("STATUS:CONFIRMED\r\n");
        ics.append("BEGIN:VALARM\r\n");
        ics.append("TRIGGER:-PT1H\r\n");
        ics.append("ACTION:DISPLAY\r\n");
        ics.append("DESCRIPTION:Rappel: ").append(escapeICS(eventName)).append(" dans 1 heure\r\n");
        ics.append("END:VALARM\r\n");
        ics.append("BEGIN:VALARM\r\n");
        ics.append("TRIGGER:-P1D\r\n");
        ics.append("ACTION:DISPLAY\r\n");
        ics.append("DESCRIPTION:Rappel: ").append(escapeICS(eventName)).append(" demain\r\n");
        ics.append("END:VALARM\r\n");
        ics.append("END:VEVENT\r\n");
        ics.append("END:VCALENDAR\r\n");
        return ics.toString();
    }

    /**
     * Save ICS file to disk
     */
    public static boolean saveICSFile(File file, String icsContent) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.print(icsContent);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String escapeICS(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace("\n", "\\n");
    }
}
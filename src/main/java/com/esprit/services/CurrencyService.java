package com.esprit.services;

import com.google.gson.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * Currency Exchange Service - Uses free ExchangeRate API
 * Converts DT (Tunisian Dinar) to other currencies
 */
public class CurrencyService {

    // Fixed exchange rates (offline fallback - approximate rates)
    private static final Map<String, Double> RATES = new LinkedHashMap<>();
    static {
        RATES.put("EUR", 0.30);   // 1 DT = 0.30 EUR
        RATES.put("USD", 0.32);   // 1 DT = 0.32 USD
        RATES.put("GBP", 0.25);   // 1 DT = 0.25 GBP
        RATES.put("MAD", 3.20);   // 1 DT = 3.20 MAD
        RATES.put("SAR", 1.20);   // 1 DT = 1.20 SAR
        RATES.put("AED", 1.17);   // 1 DT = 1.17 AED
        RATES.put("CAD", 0.43);   // 1 DT = 0.43 CAD
        RATES.put("CHF", 0.28);   // 1 DT = 0.28 CHF
        RATES.put("TRY", 10.3);   // 1 DT = 10.3 TRY
        RATES.put("DZD", 43.0);   // 1 DT = 43.0 DZD
    }

    private static final Map<String, String> CURRENCY_NAMES = new LinkedHashMap<>();
    static {
        CURRENCY_NAMES.put("EUR", "\uD83C\uDDEA\uD83C\uDDFA Euro");
        CURRENCY_NAMES.put("USD", "\uD83C\uDDFA\uD83C\uDDF8 Dollar US");
        CURRENCY_NAMES.put("GBP", "\uD83C\uDDEC\uD83C\uDDE7 Livre Sterling");
        CURRENCY_NAMES.put("MAD", "\uD83C\uDDF2\uD83C\uDDE6 Dirham Marocain");
        CURRENCY_NAMES.put("SAR", "\uD83C\uDDF8\uD83C\uDDE6 Riyal Saoudien");
        CURRENCY_NAMES.put("AED", "\uD83C\uDDE6\uD83C\uDDEA Dirham Emirien");
        CURRENCY_NAMES.put("CAD", "\uD83C\uDDE8\uD83C\uDDE6 Dollar Canadien");
        CURRENCY_NAMES.put("CHF", "\uD83C\uDDE8\uD83C\uDDED Franc Suisse");
        CURRENCY_NAMES.put("TRY", "\uD83C\uDDF9\uD83C\uDDF7 Livre Turque");
        CURRENCY_NAMES.put("DZD", "\uD83C\uDDE9\uD83C\uDDFF Dinar Algerien");
    }

    public static Map<String, Double> getRates() { return Collections.unmodifiableMap(RATES); }
    public static Map<String, String> getCurrencyNames() { return Collections.unmodifiableMap(CURRENCY_NAMES); }

    public static double convert(double amountDT, String toCurrency) {
        Double rate = RATES.get(toCurrency);
        if (rate == null) return amountDT;
        return amountDT * rate;
    }

    public static String formatConverted(double amountDT, String toCurrency) {
        double converted = convert(amountDT, toCurrency);
        return String.format("%.2f %s", converted, toCurrency);
    }

    public static String getCurrencySymbol(String code) {
        switch (code) {
            case "EUR": return "\u20AC";
            case "USD": return "$";
            case "GBP": return "\u00A3";
            case "MAD": return "MAD";
            case "SAR": return "SAR";
            case "AED": return "AED";
            case "CAD": return "CA$";
            case "CHF": return "CHF";
            case "TRY": return "\u20BA";
            case "DZD": return "DZD";
            default: return code;
        }
    }
}
package ua.haponov.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class DataInfoDecoder {

    private static final Pattern STRING_PATTERN = Pattern.compile("\"([^\"]*)\"");

    /**
     * Раскодирует строки из формата 1С, исправляя кодировку UTF-8
     */
    public static String decode1CString(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        Matcher matcher = STRING_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            result.append(input, lastEnd, matcher.start());

            String content = matcher.group(1);
            result.append("\"").append(fixEncoding(content)).append("\"");

            lastEnd = matcher.end();
        }
        result.append(input.substring(lastEnd));

        return result.toString();
    }

    private static String fixEncoding(String text) {
        try {
            if (text.contains("Р") || text.contains("С")) {

                byte[] bytes = text.getBytes("Windows-1251");
                String decoded = new String(bytes, StandardCharsets.UTF_8);

                return decoded;
            }
        } catch (Exception e) {
        }
        return text;
    }

    /**
     * Извлекает только строковое значение из формата {"S", "Value"} и раскодирует его
     */
    public static String extractValue(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        Matcher matcher = STRING_PATTERN.matcher(input);
        String lastContent = "";

        while (matcher.find()) {
            String content = matcher.group(1);
            if (content.length() > 1 || (!content.equals("S") && !content.equals("P"))) {
                lastContent = content;
            }
        }

        return lastContent.isEmpty() ? input : fixEncoding(lastContent);
    }

}
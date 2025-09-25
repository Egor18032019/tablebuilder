package com.tablebuilder.demo.utils;



import com.tablebuilder.demo.exception.InvalidNameException;

import java.util.regex.Pattern;



import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * Утилитарный класс для преобразования произвольных строк (включая кириллицу)
 * в валидные SQL-идентификаторы (только [a-z][a-z0-9_]*)
 */
public final class NameUtils {

    private static final Map<Character, String> CYRILLIC_TO_LATIN = new HashMap<>();
    private static final Pattern VALID_SQL_IDENTIFIER = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$");

    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$");


    static { // Ну а что вы хотели ?
        // Основные буквы
        CYRILLIC_TO_LATIN.put('а', "a");
        CYRILLIC_TO_LATIN.put('б', "b");
        CYRILLIC_TO_LATIN.put('в', "v");
        CYRILLIC_TO_LATIN.put('г', "g");
        CYRILLIC_TO_LATIN.put('д', "d");
        CYRILLIC_TO_LATIN.put('е', "e");
        CYRILLIC_TO_LATIN.put('ё', "yo");
        CYRILLIC_TO_LATIN.put('ж', "zh");
        CYRILLIC_TO_LATIN.put('з', "z");
        CYRILLIC_TO_LATIN.put('и', "i");
        CYRILLIC_TO_LATIN.put('й', "y");
        CYRILLIC_TO_LATIN.put('к', "k");
        CYRILLIC_TO_LATIN.put('л', "l");
        CYRILLIC_TO_LATIN.put('м', "m");
        CYRILLIC_TO_LATIN.put('н', "n");
        CYRILLIC_TO_LATIN.put('о', "o");
        CYRILLIC_TO_LATIN.put('п', "p");
        CYRILLIC_TO_LATIN.put('р', "r");
        CYRILLIC_TO_LATIN.put('с', "s");
        CYRILLIC_TO_LATIN.put('т', "t");
        CYRILLIC_TO_LATIN.put('у', "u");
        CYRILLIC_TO_LATIN.put('ф', "f");
        CYRILLIC_TO_LATIN.put('х', "kh");
        CYRILLIC_TO_LATIN.put('ц', "ts");
        CYRILLIC_TO_LATIN.put('ч', "ch");
        CYRILLIC_TO_LATIN.put('ш', "sh");
        CYRILLIC_TO_LATIN.put('щ', "shch");
        CYRILLIC_TO_LATIN.put('ъ', "");
        CYRILLIC_TO_LATIN.put('ы', "y");
        CYRILLIC_TO_LATIN.put('ь', "");
        CYRILLIC_TO_LATIN.put('э', "e");
        CYRILLIC_TO_LATIN.put('ю', "yu");
        CYRILLIC_TO_LATIN.put('я', "ya");

        // Заглавные
        CYRILLIC_TO_LATIN.put('А', "A");
        CYRILLIC_TO_LATIN.put('Б', "B");
        CYRILLIC_TO_LATIN.put('В', "V");
        CYRILLIC_TO_LATIN.put('Г', "G");
        CYRILLIC_TO_LATIN.put('Д', "D");
        CYRILLIC_TO_LATIN.put('Е', "E");
        CYRILLIC_TO_LATIN.put('Ё', "Yo");
        CYRILLIC_TO_LATIN.put('Ж', "Zh");
        CYRILLIC_TO_LATIN.put('З', "Z");
        CYRILLIC_TO_LATIN.put('И', "I");
        CYRILLIC_TO_LATIN.put('Й', "Y");
        CYRILLIC_TO_LATIN.put('К', "K");
        CYRILLIC_TO_LATIN.put('Л', "L");
        CYRILLIC_TO_LATIN.put('М', "M");
        CYRILLIC_TO_LATIN.put('Н', "N");
        CYRILLIC_TO_LATIN.put('О', "O");
        CYRILLIC_TO_LATIN.put('П', "P");
        CYRILLIC_TO_LATIN.put('Р', "R");
        CYRILLIC_TO_LATIN.put('С', "S");
        CYRILLIC_TO_LATIN.put('Т', "T");
        CYRILLIC_TO_LATIN.put('У', "U");
        CYRILLIC_TO_LATIN.put('Ф', "F");
        CYRILLIC_TO_LATIN.put('Х', "Kh");
        CYRILLIC_TO_LATIN.put('Ц', "Ts");
        CYRILLIC_TO_LATIN.put('Ч', "Ch");
        CYRILLIC_TO_LATIN.put('Ш', "Sh");
        CYRILLIC_TO_LATIN.put('Щ', "Shch");
        CYRILLIC_TO_LATIN.put('Ъ', "");
        CYRILLIC_TO_LATIN.put('Ы', "Y");
        CYRILLIC_TO_LATIN.put('Ь', "");
        CYRILLIC_TO_LATIN.put('Э', "E");
        CYRILLIC_TO_LATIN.put('Ю', "Yu");
        CYRILLIC_TO_LATIN.put('Я', "Ya");
    }

    private NameUtils() {}

    /**
     * Транслитерация кириллицы в латиницу и валидация SQL-идентификатора
     * Риск SQL-инъекций + Каждый запрос требует кавычек + работа с Hibernate/JPA
     * @param input - строка
     * @return
     */
    public static String toValidSqlName(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "col_" + Math.abs(System.currentTimeMillis() % 1000000);
        }

        String cleanInput = input.trim();

        // Транслитерация вручную
        StringBuilder transliterated = new StringBuilder();
        for (char c : cleanInput.toCharArray()) {
            if (CYRILLIC_TO_LATIN.containsKey(c)) {
                transliterated.append(CYRILLIC_TO_LATIN.get(c));
            } else {
                transliterated.append(c);
            }
        }

        // Очистка до валидного SQL-имени
        String cleaned = transliterated.toString()
                .replaceAll("[^a-zA-Z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");

        if (cleaned.isEmpty() || Character.isDigit(cleaned.charAt(0))) {
            cleaned = "col_" + cleaned;
        }

        if (cleaned.length() > 63) {
            cleaned = cleaned.substring(0, 63);
        }

        cleaned = cleaned.replaceAll("_+$", "");

        if (cleaned.isEmpty() || !VALID_SQL_IDENTIFIER.matcher(cleaned).matches()) {
            cleaned = "col_" + Math.abs(input.hashCode());
            if (cleaned.length() > 63) {
                cleaned = cleaned.substring(0, 63);
            }
        }

        return cleaned.toLowerCase();
    }

    public static String sanitizeName(String name) {
        if (name == null || !VALID_NAME_PATTERN.matcher(name).matches()) {
            throw new InvalidNameException("Invalid name: " + name + ". Must start with letter, contain only letters, digits, underscores.");
        }
        return name.toLowerCase();
    }

}
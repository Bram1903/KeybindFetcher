package com.deathmotion.translatablefetcher.util;

import com.deathmotion.translatablefetcher.TranslatableFetcher;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import lombok.Getter;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

public final class VanillaTranslatableUtil {

    private static final Logger LOGGER = Logger.getLogger(TranslatableFetcher.getMOD_ID());
    private static final Gson GSON = new Gson();

    private static final String RESOURCE_PATH = "/assets/translatable-fetcher/Translatables.json";

    @Getter
    private static Set<String> bundledTranslatable = Set.of();

    private VanillaTranslatableUtil() {
    }

    public static void load() {
        try (InputStream input = VanillaTranslatableUtil.class.getResourceAsStream(RESOURCE_PATH)) {
            if (input == null) {
                LOGGER.warning("Could not find bundled translatables file at " + RESOURCE_PATH);
                bundledTranslatable = Set.of();
                return;
            }

            try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                String[] values = GSON.fromJson(reader, String[].class);

                if (values == null) {
                    LOGGER.warning("Bundled translatables file was empty or invalid.");
                    bundledTranslatable = Set.of();
                    return;
                }

                Set<String> loaded = new TreeSet<>();
                Collections.addAll(loaded, values);

                bundledTranslatable = Collections.unmodifiableSet(loaded);
                LOGGER.info("Loaded " + bundledTranslatable.size() + " bundled translatable keys.");
            }
        } catch (JsonParseException ex) {
            LOGGER.severe("Failed to parse bundled translatables JSON: " + ex.getMessage());
            bundledTranslatable = Set.of();
        } catch (Exception ex) {
            LOGGER.severe("Failed to load bundled translatables: " + ex.getMessage());
            bundledTranslatable = Set.of();
        }
    }

    public static Set<String> getExtras(Set<String> runtimeKeys) {
        Set<String> extras = new TreeSet<>(runtimeKeys);
        extras.removeAll(bundledTranslatable);
        return extras;
    }
}
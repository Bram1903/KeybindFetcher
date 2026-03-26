package com.deathmotion.translatablefetcher.commands;

import com.deathmotion.translatablefetcher.TranslatableFetcher;
import com.deathmotion.translatablefetcher.mixin.accessor.ClientLanguageAccessor;
import com.deathmotion.translatablefetcher.util.VanillaTranslatableUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Util;
import org.jetbrains.annotations.UnknownNullability;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public final class FetchCommand {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final Logger LOGGER = Logger.getLogger(TranslatableFetcher.getMOD_ID());
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void register(@UnknownNullability CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommandManager.literal("fetchtranslatable")
                        .executes(FetchCommand::sendAllTranslatables)
                        .then(ClientCommandManager.literal("export").executes(FetchCommand::exportAllTranslatables))
        );
    }

    private static int sendAllTranslatables(CommandContext<FabricClientCommandSource> ctx) {
        List<String> keys = collectNonVanillaTranslationKeys();

        if (keys.isEmpty()) {
            return send(ctx, accent("No non-vanilla translatable keys found.", ChatFormatting.GRAY));
        }

        String logOutput = keys.stream()
                .map(key -> "\"" + key + "\",")
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        LOGGER.info("Detected non-vanilla translatable keys (" + keys.size() + "):\n" + logOutput);

        return send(
                ctx,
                Component.empty()
                        .append(accent("Found ", ChatFormatting.GREEN))
                        .append(Component.literal(String.valueOf(keys.size())).withStyle(ChatFormatting.GOLD))
                        .append(accent(" non-vanilla translatable keys. Check the console output.", ChatFormatting.GREEN))
        );
    }

    private static int exportAllTranslatables(CommandContext<FabricClientCommandSource> ctx) {
        List<String> keys = collectNonVanillaTranslationKeys();

        if (keys.isEmpty()) {
            return send(ctx, accent("No non-vanilla translatable keys found.", ChatFormatting.GRAY));
        }

        Path exportDir = mc.gameDirectory.toPath().resolve("translatablefetcher");
        Path exportFile = exportDir.resolve("Translatables.json");

        send(ctx, accent("Export started in background...", ChatFormatting.YELLOW));

        CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(exportDir);

                JsonArray json = new JsonArray();
                for (String key : keys) {
                    json.add(key);
                }

                Files.writeString(exportFile, GSON.toJson(json));

                LOGGER.info("Exported " + keys.size() + " non-vanilla translatable keys to " + exportFile);

                mc.execute(() -> ctx.getSource().sendFeedback(
                        Component.empty()
                                .append(accent("Exported ", ChatFormatting.GREEN))
                                .append(Component.literal(String.valueOf(keys.size())).withStyle(ChatFormatting.GOLD))
                                .append(accent(" non-vanilla translatable keys to ", ChatFormatting.GREEN))
                                .append(Component.literal(exportFile.toString()).withStyle(
                                        Style.EMPTY
                                                .withColor(ChatFormatting.AQUA)
                                                .withUnderlined(true)
                                                .withClickEvent(new ClickEvent.OpenFile(exportFile.toFile()))
                                                .withHoverEvent(new HoverEvent.ShowText(
                                                        Component.literal(exportFile.toAbsolutePath().toString())
                                                ))
                                ))
                ));
            } catch (IOException e) {
                LOGGER.severe("Failed to export translatable keys: " + e.getMessage());

                mc.execute(() -> ctx.getSource().sendFeedback(
                        Component.empty()
                                .append(accent("Failed to export translatable keys: ", ChatFormatting.RED))
                                .append(Component.literal(e.getMessage()).withStyle(ChatFormatting.GRAY))
                ));
            }
        }, Util.ioPool());

        return 1;
    }

    private static List<String> collectNonVanillaTranslationKeys() {
        Set<String> runtimeKeys = collectAllTranslationKeys();
        Set<String> extras = VanillaTranslatableUtil.getExtras(runtimeKeys);
        return List.copyOf(extras);
    }

    private static Set<String> collectAllTranslationKeys() {
        Set<String> keys = new TreeSet<>();

        collectKeybindTranslationKeys(keys);
        collectLoadedTranslationKeys(keys);

        return keys;
    }

    private static void collectKeybindTranslationKeys(Set<String> keys) {
        KeyMapping[] mappings = mc.options.keyMappings;
        Arrays.stream(mappings)
                .map(KeyMapping::getName)
                .forEach(keys::add);
    }

    private static void collectLoadedTranslationKeys(Set<String> keys) {
        Language language = Language.getInstance();

        if (!(language instanceof ClientLanguage clientLanguage)) {
            LOGGER.warning("Active language is not a ClientLanguage instance.");
            return;
        }

        Map<String, String> storage =
                ((ClientLanguageAccessor) clientLanguage).translatableFetcher$getStorage();

        keys.addAll(storage.keySet());
    }

    private static MutableComponent accent(String text, ChatFormatting... fmt) {
        return Component.literal(text).withStyle(fmt);
    }

    private static int send(CommandContext<FabricClientCommandSource> ctx, Component msg) {
        ctx.getSource().sendFeedback(msg);
        return 1;
    }
}
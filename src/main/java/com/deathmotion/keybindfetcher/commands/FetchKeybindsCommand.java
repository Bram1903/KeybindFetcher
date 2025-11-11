package com.deathmotion.keybindfetcher.commands;

import com.deathmotion.keybindfetcher.KeybindFetcher;
import com.deathmotion.keybindfetcher.util.VanillaKeybindsUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class FetchKeybindsCommand {

    private static final Minecraft mc = Minecraft.getInstance();

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                literal("fetchkeybinds")
                        .executes(FetchKeybindsCommand::sendAll)
                        .then(literal("mods").executes(FetchKeybindsCommand::sendMods))
                        .then(literal("dumpvanillakeys").executes(FetchKeybindsCommand::dumpVanillaKeybinds))
        );
    }

    private static int sendAll(CommandContext<FabricClientCommandSource> ctx) {
        KeyMapping[] mappings = mc.options.keyMappings;
        if (mappings.length == 0) {
            return send(ctx, Component.literal("No keybinds found."));
        }
        return send(ctx, renderList("Keybinds:", Arrays.stream(mappings)));
    }

    private static int sendMods(CommandContext<FabricClientCommandSource> ctx) {
        KeyMapping[] mappings = mc.options.keyMappings;
        if (mappings.length == 0) {
            return send(ctx, Component.literal("No keybinds found."));
        }

        List<KeyMapping> custom = Arrays.stream(mappings)
                .filter(FetchKeybindsCommand::isModKeybind)
                .toList();

        if (custom.isEmpty()) {
            return send(ctx, Component.literal("No custom (mod) keybinds found."));
        }

        return send(ctx, renderList("Custom keybinds (mods):", custom.stream()));
    }

    private static int dumpVanillaKeybinds(CommandContext<FabricClientCommandSource> ctx) {
        KeyMapping[] mappings = Minecraft.getInstance().options.keyMappings;

        List<String> names = Arrays.stream(mappings)
                .map(KeyMapping::getName)
                .sorted()
                .toList();

        MutableComponent msg = Component.literal("=== VANILLA KEYBINDS ===\n");
        for (String name : names) {
            msg.append(name).append("\n");
        }

        ctx.getSource().sendFeedback(msg);
        Logger.getLogger(KeybindFetcher.getMOD_ID()).info(msg.getString());

        return 1;
    }

    private static boolean isModKeybind(KeyMapping km) {
        return !VanillaKeybindsUtil.VANILLA_KEYBINDS.contains(km.getName());
    }

    private static MutableComponent renderList(String header, Stream<KeyMapping> stream) {
        List<KeyMapping> list = stream.toList();
        MutableComponent msg = Component.literal(header);

        if (!list.isEmpty()) {
            msg.append("\n");
        }

        for (int i = 0; i < list.size(); i++) {
            KeyMapping km = list.get(i);
            msg.append(lineFor(km));
            if (i < list.size() - 1) {
                msg.append("\n");
            }
        }
        return msg;
    }

    private static MutableComponent lineFor(KeyMapping km) {
        return Component.literal("- ")
                .append(Component.translatable(km.getName()))
                .append(": ")
                .append(km.getName());
    }

    private static int send(CommandContext<FabricClientCommandSource> ctx, Component msg) {
        ctx.getSource().sendFeedback(msg);
        return 1;
    }
}

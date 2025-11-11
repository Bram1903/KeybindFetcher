package com.deathmotion.keybindfetcher.commands;

import com.deathmotion.keybindfetcher.KeybindFetcher;
import com.deathmotion.keybindfetcher.util.VanillaKeybindsUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FetchKeybindsCommand {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final MutableComponent NL = Component.literal("\n");
    private static final Logger LOGGER = Logger.getLogger(KeybindFetcher.getMOD_ID());

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommandManager.literal("fetchkeybinds")
                        .executes(FetchKeybindsCommand::sendAll)
                        .then(ClientCommandManager.literal("mods").executes(FetchKeybindsCommand::sendMods))
        );
    }

    private static int sendAll(CommandContext<FabricClientCommandSource> ctx) {
        KeyMapping[] mappings = mc.options.keyMappings;
        if (mappings.length == 0) {
            return send(ctx, accent("No keybinds found.", ChatFormatting.GRAY));
        }

        String keybindList = Arrays.stream(mappings)
                .map(KeyMapping::getName)
                .collect(Collectors.joining("\n"));
        LOGGER.info("Detected Keybinds:\n" + keybindList);

        return send(ctx, renderList("Keybinds", Arrays.stream(mappings)));
    }

    private static int sendMods(CommandContext<FabricClientCommandSource> ctx) {
        return sendFiltered(ctx,
                FetchKeybindsCommand::isModKeybind
        );
    }

    private static int sendFiltered(CommandContext<FabricClientCommandSource> ctx, Predicate<KeyMapping> predicate) {
        KeyMapping[] mappings = mc.options.keyMappings;
        if (mappings.length == 0) {
            return send(ctx, accent("No keybinds found.", ChatFormatting.GRAY));
        }

        List<KeyMapping> list = Arrays.stream(mappings)
                .filter(predicate)
                .toList();

        if (list.isEmpty()) {
            return send(ctx, accent("No custom (mod) keybinds found.", ChatFormatting.GRAY));
        }

        return send(ctx, renderList("Custom keybinds (mods)", list.stream()));
    }

    private static boolean isModKeybind(KeyMapping km) {
        return !VanillaKeybindsUtil.VANILLA_KEYBINDS.contains(km.getName());
    }

    private static MutableComponent renderList(String headerTitle, Stream<KeyMapping> stream) {
        List<KeyMapping> list = stream.toList();

        MutableComponent msg = header(headerTitle + " (" + list.size() + ")");
        if (!list.isEmpty()) msg.append(NL);

        for (int i = 0; i < list.size(); i++) {
            msg.append(lineForKeyOnly(list.get(i)));
            if (i < list.size() - 1) msg.append(NL);
        }
        return msg;
    }

    private static MutableComponent lineForKeyOnly(KeyMapping km) {
        String keyId = km.getName();

        MutableComponent hover = Component.empty()
                .append(accent("Category: ", ChatFormatting.GRAY))
                .append(Component.translatable(km.getCategory()).withStyle(ChatFormatting.YELLOW))
                .append(accent("\nClick to copy key id", ChatFormatting.DARK_GRAY));

        return bullet()
                .append(Component.translatable(keyId).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                .append(accent("  [", ChatFormatting.DARK_GRAY))
                .append(Component.literal(keyId).withStyle(ChatFormatting.GOLD))
                .append(accent("]", ChatFormatting.DARK_GRAY))
                .withStyle(
                        Style.EMPTY
                                .withHoverEvent(new HoverEvent.ShowText(hover))
                                .withClickEvent(new ClickEvent.CopyToClipboard(keyId))
                );
    }

    private static MutableComponent header(String text) {
        return Component.literal("⟪ ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(text).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal(" ⟫").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static MutableComponent bullet() {
        return Component.literal("• ").withStyle(ChatFormatting.DARK_GRAY);
    }

    private static MutableComponent accent(String text, ChatFormatting... fmt) {
        return Component.literal(text).withStyle(fmt);
    }

    private static int send(CommandContext<FabricClientCommandSource> ctx, Component msg) {
        ctx.getSource().sendFeedback(msg);
        return 1;
    }
}

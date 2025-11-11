package com.deathmotion.keybindfetcher;

import com.deathmotion.keybindfetcher.commands.FetchKeybindsCommand;
import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

@Environment(EnvType.CLIENT)
public class KeybindFetcher implements ClientModInitializer {

    @Getter
    private static final String MOD_ID = "KeybindFetcher";

    @Getter
    private static KeybindFetcher instance;

    @Override
    public void onInitializeClient() {
        instance = this;

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            FetchKeybindsCommand.register(dispatcher);
        });
    }
}

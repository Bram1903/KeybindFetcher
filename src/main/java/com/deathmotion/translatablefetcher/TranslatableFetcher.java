package com.deathmotion.translatablefetcher;

import com.deathmotion.translatablefetcher.commands.FetchCommand;
import com.deathmotion.translatablefetcher.util.VanillaTranslatableUtil;
import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

@Environment(EnvType.CLIENT)
public class TranslatableFetcher implements ClientModInitializer {

    @Getter
    private static final String MOD_ID = "TranslatableFetcher";

    @Getter
    private static TranslatableFetcher instance;

    @Override
    public void onInitializeClient() {
        instance = this;
        VanillaTranslatableUtil.load();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            FetchCommand.register(dispatcher);
        });
    }
}

package com.telepathicgrunt.the_bumblezone.client;

import com.telepathicgrunt.the_bumblezone.Bumblezone;
import com.telepathicgrunt.the_bumblezone.modinit.BzSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public class MusicHandler {

    private static SoundInstance ANGRY_BEE_MUSIC = null;
    private static final ResourceLocation BIOME_MUSIC = new ResourceLocation(Bumblezone.MODID, "biome_music");

    // CLIENT-SIDED
    public static void playAngryBeeMusic(Player entity) {
        Minecraft minecraftClient = Minecraft.getInstance();
        if(!entity.isCreative() && entity == minecraftClient.player && !minecraftClient.getSoundManager().isActive(ANGRY_BEE_MUSIC)) {
            ANGRY_BEE_MUSIC = SimpleSoundInstance.forMusic(BzSounds.ANGERED_BEES.get());
            minecraftClient.getSoundManager().play(ANGRY_BEE_MUSIC);
        }
        minecraftClient.getSoundManager().stop(BIOME_MUSIC, SoundSource.MUSIC);
    }

    // CLIENT-SIDED
    public static void stopAngryBeeMusic(Player entity) {
        Minecraft minecraftClient = Minecraft.getInstance();
        if(entity == minecraftClient.player && ANGRY_BEE_MUSIC != null) {
            minecraftClient.getSoundManager().stop(ANGRY_BEE_MUSIC);
        }
    }
}

package com.telepathicgrunt.the_bumblezone.items;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.RecordItem;

public class BzMusicDiscs extends RecordItem {
    public BzMusicDiscs(int comparatorOutput, SoundEvent sound, Properties settings, int musicTimeLength) {
        super(comparatorOutput, sound, settings, musicTimeLength);
    }
}

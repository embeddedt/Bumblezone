package com.telepathicgrunt.the_bumblezone.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.telepathicgrunt.the_bumblezone.Bumblezone;
import com.telepathicgrunt.the_bumblezone.entities.mobs.BeehemothEntity;
import com.telepathicgrunt.the_bumblezone.mixin.client.KeyMappingAccessor;
import com.telepathicgrunt.the_bumblezone.packets.BeehemothControlsPacket;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import org.lwjgl.glfw.GLFW;

public class BeehemothControls {

    public static final KeyMapping KEY_BIND_BEEHEMOTH_DOWN = createSafeKeyMapping("key." + Bumblezone.MODID + ".beehemoth_down", GLFW.GLFW_KEY_CAPS_LOCK);
    public static final KeyMapping KEY_BIND_BEEHEMOTH_UP = createSafeKeyMapping("key." + Bumblezone.MODID + ".beehemoth_up", GLFW.GLFW_KEY_SPACE);

    public static void keyInput(int key, int scancode, int action) {
        if (Minecraft.getInstance().player != null &&
            Minecraft.getInstance().player.getVehicle() instanceof BeehemothEntity &&
            (KEY_BIND_BEEHEMOTH_UP.matches(key, scancode) ||
             KEY_BIND_BEEHEMOTH_DOWN.matches(key, scancode)))
        {
            FriendlyByteBuf passedData = new FriendlyByteBuf(Unpooled.buffer());
            passedData.writeByte(KEY_BIND_BEEHEMOTH_UP.matches(key, scancode) ? action : 2);
            passedData.writeByte(KEY_BIND_BEEHEMOTH_DOWN.matches(key, scancode) ? action : 2);
            ClientPlayNetworking.send(BeehemothControlsPacket.PACKET_ID, passedData);
        }
    }

    private static KeyMapping createSafeKeyMapping(String description, int keycode) {
        InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(keycode);
        KeyMapping oldMapping = KeyMappingAccessor.getMAP().get(key);
        KeyMapping keyMapping = new KeyMapping(description, keycode, "key.categories." + Bumblezone.MODID);
        KeyMappingAccessor.getMAP().put(key, oldMapping);
        KeyMappingAccessor.getALL().remove(description);
        return keyMapping;
    }
}

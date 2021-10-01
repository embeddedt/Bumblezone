package com.telepathicgrunt.bumblezone.mixin.entities;

import com.telepathicgrunt.bumblezone.entities.BeeInteractivity;
import com.telepathicgrunt.bumblezone.entities.CreatingHoneySlime;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(PlayerEntity.class)
public class PlayerInteractsEntityMixin {
    // Feeding bees honey or sugar water. Or turning Slime into Honey Slime
    @Inject(method = "interact",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isEmpty()Z", ordinal = 0))
    private void thebumblezone_onBeeFeeding(Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if(entity instanceof BeeEntity beeEntity) {
            if(BeeInteractivity.beeFeeding(entity.world, ((PlayerEntity)(Object)this), hand, beeEntity) == ActionResult.SUCCESS)
                cir.setReturnValue(ActionResult.SUCCESS);
            else if(BeeInteractivity.beeUnpollinating(entity.world, ((PlayerEntity)(Object)this), hand, beeEntity) == ActionResult.SUCCESS)
                cir.setReturnValue(ActionResult.SUCCESS);
        }
        else if (entity instanceof SlimeEntity slimeEntity) {
            if(CreatingHoneySlime.createHoneySlime(entity.world, ((PlayerEntity)(Object)this), hand, slimeEntity) == ActionResult.SUCCESS)
                cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}
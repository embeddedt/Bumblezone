package com.telepathicgrunt.the_bumblezone.items;

import com.telepathicgrunt.the_bumblezone.Bumblezone;
import com.telepathicgrunt.the_bumblezone.modinit.BzItems;
import com.telepathicgrunt.the_bumblezone.modinit.BzSounds;
import com.telepathicgrunt.the_bumblezone.modinit.BzTags;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Vanishable;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class HoneyCompass extends Item implements Vanishable {
    public static final String TAG_TARGET_POS = "TargetPos";
    public static final String TAG_TARGET_DIMENSION = "TargetDimension";
    public static final String TAG_TYPE = "CompassType";
    public static final String TARGET_BLOCK = "TargetBlock";

    public HoneyCompass(Item.Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack itemStack) {
        return isStructureCompass(itemStack) || super.isFoil(itemStack);
    }

    @Override
    public String getDescriptionId(ItemStack itemStack) {
        if(isStructureCompass(itemStack)) {
            return "item.the_bumblezone.honey_compass_structure";
        }

        if(isBlockCompass(itemStack)) {
            return "item.the_bumblezone.honey_compass_block";
        }

        return super.getDescriptionId(itemStack);
    }

    @Override
    public void inventoryTick(ItemStack itemStack, Level level, Entity entity, int i, boolean bl) {
        if (!level.isClientSide) {
            if (isBlockCompass(itemStack)) {
                CompoundTag compoundTag = itemStack.getOrCreateTag();
                if (compoundTag.contains(TAG_TARGET_POS) && compoundTag.contains(TARGET_BLOCK) && compoundTag.contains(TAG_TARGET_DIMENSION)) {

                    Optional<ResourceKey<Level>> optional = Level.RESOURCE_KEY_CODEC.parse(NbtOps.INSTANCE, compoundTag.get(HoneyCompass.TAG_TARGET_DIMENSION)).result();
                    if (optional.isPresent() && optional.get().equals(level.dimension())) {
                        BlockPos blockPos = NbtUtils.readBlockPos(compoundTag.getCompound(TAG_TARGET_POS));
                        if (!level.isInWorldBounds(blockPos)) {
                            compoundTag.remove(TAG_TARGET_POS);
                            compoundTag.remove(TAG_TARGET_DIMENSION);
                            compoundTag.remove(TARGET_BLOCK);
                            compoundTag.remove(TAG_TYPE);
                            return;
                        }

                        ChunkAccess chunk = level.getChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4, ChunkStatus.FULL, false);
                        if(chunk != null && !(Registry.BLOCK.getKey(chunk.getBlockState(blockPos).getBlock()).toString().equals(compoundTag.getString(TARGET_BLOCK)))) {
                            compoundTag.remove(TAG_TARGET_POS);
                            compoundTag.remove(TAG_TARGET_DIMENSION);
                            compoundTag.remove(TARGET_BLOCK);
                            compoundTag.remove(TAG_TYPE);
                        }
                    }
                }
                else {
                    compoundTag.remove(TAG_TARGET_POS);
                    compoundTag.remove(TAG_TARGET_DIMENSION);
                    compoundTag.remove(TARGET_BLOCK);
                    compoundTag.remove(TAG_TYPE);
                }
            }

        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand)  {
        ItemStack itemStack = player.getItemInHand(interactionHand);
        BlockPos playerPos = player.blockPosition();

        if (!level.isClientSide() && !isStructureCompass(itemStack)) {
            BlockPos structurePos = ((ServerLevel) level).findNearestMapFeature(BzTags.HONEY_COMPASS_LOCATING, playerPos, 100, false);
            if(structurePos == null) {
                return super.use(level, player, interactionHand);
            }

            level.playSound(null, playerPos, BzSounds.HONEY_COMPASS_STRUCTURE_LOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
            boolean singleCompass = !player.getAbilities().instabuild && itemStack.getCount() == 1;
            if (singleCompass) {
                this.addStructureTags(level.dimension(), structurePos, itemStack.getOrCreateTag());
            }
            else {
                ItemStack newCompass = new ItemStack(BzItems.HONEY_COMPASS, 1);
                CompoundTag newCompoundTag = itemStack.hasTag() ? itemStack.getTag().copy() : new CompoundTag();
                newCompass.setTag(newCompoundTag);
                if (!player.getAbilities().instabuild) {
                    itemStack.shrink(1);
                }

                this.addStructureTags(level.dimension(), structurePos, newCompoundTag);
                if (!player.getInventory().add(newCompass)) {
                    player.drop(newCompass, false);
                }
            }

            return InteractionResultHolder.success(itemStack);
        }

        return super.use(level, player, interactionHand);
    }


    @Override
    public InteractionResult useOn(UseOnContext useOnContext) {
        BlockPos blockPos = useOnContext.getClickedPos();
        Level level = useOnContext.getLevel();
        Player player = useOnContext.getPlayer();
        ItemStack handCompass = useOnContext.getItemInHand();
        BlockState targetBlock = level.getBlockState(blockPos);

        if (player != null && isValidBeeHive(targetBlock)) {
            level.playSound(null, blockPos, BzSounds.HONEY_COMPASS_BLOCK_LOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
            boolean singleCompass = !player.getAbilities().instabuild && handCompass.getCount() == 1;
            if (singleCompass) {
                this.addBlockTags(level.dimension(), blockPos, handCompass.getOrCreateTag(), targetBlock.getBlock());
            }
            else {
                ItemStack newCompass = new ItemStack(BzItems.HONEY_COMPASS, 1);
                CompoundTag newCompoundTag = handCompass.hasTag() ? handCompass.getTag().copy() : new CompoundTag();
                newCompass.setTag(newCompoundTag);
                if (!player.getAbilities().instabuild) {
                    handCompass.shrink(1);
                }

                this.addBlockTags(level.dimension(), blockPos, newCompoundTag, targetBlock.getBlock());
                if (!player.getInventory().add(newCompass)) {
                    player.drop(newCompass, false);
                }
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.useOn(useOnContext);
    }

    public static boolean isValidBeeHive(BlockState block) {
        if(block.is(BzTags.BLACKLISTED_HONEY_COMPASS_BLOCKS)) return false;

        if(block.is(BlockTags.BEEHIVES) || block.getBlock() instanceof BeehiveBlock) {
            return true;
        }

        return false;
    }

    private void addStructureTags(ResourceKey<Level> resourceKey, BlockPos blockPos, CompoundTag compoundTag) {
        compoundTag.put(TAG_TARGET_POS, NbtUtils.writeBlockPos(blockPos));
        Level.RESOURCE_KEY_CODEC.encodeStart(NbtOps.INSTANCE, resourceKey).resultOrPartial(Bumblezone.LOGGER::error).ifPresent(tag -> compoundTag.put(TAG_TARGET_DIMENSION, tag));
        compoundTag.putString(TAG_TYPE, "structure");
        compoundTag.remove(TARGET_BLOCK);
    }

    private void addBlockTags(ResourceKey<Level> resourceKey, BlockPos blockPos, CompoundTag compoundTag, Block block) {
        compoundTag.put(TAG_TARGET_POS, NbtUtils.writeBlockPos(blockPos));
        Level.RESOURCE_KEY_CODEC.encodeStart(NbtOps.INSTANCE, resourceKey).resultOrPartial(Bumblezone.LOGGER::error).ifPresent(tag -> compoundTag.put(TAG_TARGET_DIMENSION, tag));
        compoundTag.putString(TAG_TYPE, "block");
        compoundTag.putString(TARGET_BLOCK, Registry.BLOCK.getKey(block).toString());
    }

    private boolean isBlockCompass(ItemStack compassItem) {
        if(compassItem.hasTag()) {
            CompoundTag tag = compassItem.getTag();
            return tag != null && tag.contains(TAG_TYPE) && tag.getString(TAG_TYPE).equals("block");
        }
        return false;
    }

    private boolean isStructureCompass(ItemStack compassItem) {
        if(compassItem.hasTag()) {
            CompoundTag tag = compassItem.getTag();
            return tag != null && tag.contains(TAG_TYPE) && tag.getString(TAG_TYPE).equals("structure");
        }
        return false;
    }

    public static ClampedItemPropertyFunction getClampedItemPropertyFunction() {
        return new ClampedItemPropertyFunction() {
            private final HoneyCompass.CompassWobble wobble = new HoneyCompass.CompassWobble();
            private final HoneyCompass.CompassWobble wobbleRandom = new HoneyCompass.CompassWobble();

            @Override
            public float unclampedCall(ItemStack itemStack, @Nullable ClientLevel clientLevel, @Nullable LivingEntity livingEntity, int i) {
                Entity entity = livingEntity != null ? livingEntity : itemStack.getEntityRepresentation();
                if (entity == null) {
                    return 0.0F;
                }
                else {
                    if (clientLevel == null && entity.level instanceof ClientLevel) {
                        clientLevel = (ClientLevel)entity.level;
                    }

                    BlockPos blockPos = this.getStructurePosition(clientLevel, itemStack.getOrCreateTag());
                    long gameTime = clientLevel.getGameTime();
                    if (blockPos != null && !(entity.position().distanceToSqr((double)blockPos.getX() + 0.5, entity.position().y(), (double)blockPos.getZ() + 0.5) < 1.0E-5F)) {
                        boolean isLocalPlayer = livingEntity instanceof Player && ((Player)livingEntity).isLocalPlayer();
                        double currentFacingAngle = 0.0;
                        if (isLocalPlayer) {
                            currentFacingAngle = livingEntity.getYRot();
                        }
                        else if (entity instanceof ItemFrame) {
                            currentFacingAngle = this.getFrameRotation((ItemFrame)entity);
                        }
                        else if (entity instanceof ItemEntity) {
                            currentFacingAngle = 180.0F - ((ItemEntity)entity).getSpin(0.5F) / (float) (Math.PI * 2) * 360.0F;
                        }
                        else if (livingEntity != null) {
                            currentFacingAngle = livingEntity.yBodyRot;
                        }

                        currentFacingAngle = Mth.positiveModulo(currentFacingAngle / 360.0, 1.0);
                        double angleToTarget = this.getAngleTo(Vec3.atCenterOf(blockPos), entity) / (float) (Math.PI * 2);
                        double angleToRender;
                        if (isLocalPlayer) {
                            if (this.wobble.shouldUpdate(gameTime)) {
                                this.wobble.update(gameTime, 0.5 - (currentFacingAngle - 0.25));
                            }

                            angleToRender = angleToTarget + this.wobble.rotation;
                        }
                        else {
                            angleToRender = 0.5 - (currentFacingAngle - 0.25 - angleToTarget);
                        }

                        return Mth.positiveModulo((float)angleToRender, 1.0F);
                    }
                    else {
                        if (this.wobbleRandom.shouldUpdate(gameTime)) {
                            this.wobbleRandom.update(gameTime, Math.random());
                        }

                        double d = this.wobbleRandom.rotation + (double)((float)this.hash(i) / 2.14748365E9F);
                        return Mth.positiveModulo((float)d, 1.0F);
                    }
                }
            }

            private int hash(int i) {
                return i * 1327217883;
            }

            @Nullable
            private BlockPos getStructurePosition(Level level, CompoundTag compoundTag) {
                boolean structurePos = compoundTag.contains(HoneyCompass.TAG_TARGET_POS);
                boolean dimension = compoundTag.contains(HoneyCompass.TAG_TARGET_DIMENSION);
                if (structurePos && dimension) {
                    Optional<ResourceKey<Level>> optional = Level.RESOURCE_KEY_CODEC.parse(NbtOps.INSTANCE, compoundTag.get(HoneyCompass.TAG_TARGET_DIMENSION)).result();
                    if (optional.isPresent() && level.dimension() == optional.get()) {
                        return NbtUtils.readBlockPos(compoundTag.getCompound(HoneyCompass.TAG_TARGET_POS));
                    }
                }

                return null;
            }

            private double getFrameRotation(ItemFrame itemFrame) {
                Direction direction = itemFrame.getDirection();
                int itemFrameFacingAngle = direction.getAxis().isVertical() ? 90 * direction.getAxisDirection().getStep() : 0;
                return Mth.wrapDegrees(180 + direction.get2DDataValue() * 90 + itemFrame.getRotation() * 45 + itemFrameFacingAngle);
            }

            private double getAngleTo(Vec3 vec3, Entity entity) {
                return Math.atan2(vec3.z() - entity.getZ(), vec3.x() - entity.getX());
            }
        };
    }

    static class CompassWobble {
        double rotation;
        private double deltaRotation;
        private long lastUpdateTick;

        boolean shouldUpdate(long currentTick) {
            return this.lastUpdateTick != currentTick;
        }

        void update(long lastTick, double currentRotation) {
            this.lastUpdateTick = lastTick;
            double rotDiff = currentRotation - this.rotation;
            rotDiff = Mth.positiveModulo(rotDiff + 0.5, 1.0) - 0.5;
            this.deltaRotation += rotDiff * 0.1;
            this.deltaRotation *= 0.8;
            this.rotation = Mth.positiveModulo(this.rotation + this.deltaRotation, 1.0);
        }
    }
}

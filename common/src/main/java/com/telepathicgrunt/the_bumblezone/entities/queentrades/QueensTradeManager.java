package com.telepathicgrunt.the_bumblezone.entities.queentrades;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.telepathicgrunt.the_bumblezone.Bumblezone;
import com.telepathicgrunt.the_bumblezone.events.lifecycle.TagsUpdatedEvent;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class QueensTradeManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().setLenient().disableHtmlEscaping().excludeFieldsWithoutExposeAnnotation().create();
    public static final QueensTradeManager QUEENS_TRADE_MANAGER = new QueensTradeManager();

    public Object2ObjectOpenHashMap<Item, WeightedRandomList<TradeEntryReducedObj>> tradeReduced = new Object2ObjectOpenHashMap<>();
    public Map<Pair<List<TradeEntryObj>, Boolean>, List<TradeEntryObj>> tradeRaw = new HashMap<>();
    public List<TradeEntryReducedObj> tradeRandomizer = new ArrayList<>();

    public QueensTradeManager() {
        super(GSON, "bz_bee_queen_trades");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> loader, ResourceManager manager, ProfilerFiller profiler) {
        tradeRaw.clear();
        loader.forEach((fileIdentifier, jsonElement) -> {
            try {
                QueenTradesCollectionObj tradesCollection = GSON.fromJson(jsonElement, QueenTradesCollectionObj.class);
                if (tradesCollection.is_color_randomizer_trade) {
                    if (!tradesCollection.randomizes.isEmpty()) {
                        tradeRaw.put(Pair.of(tradesCollection.randomizes, true), tradesCollection.randomizes);
                    }
                    return;
                }

                if (tradesCollection.wants.isEmpty() || tradesCollection.possible_rewards.isEmpty()) {
                    return;
                }
                tradeRaw.put(Pair.of(tradesCollection.wants, false), tradesCollection.possible_rewards);
            }
            catch (Exception e) {
                Bumblezone.LOGGER.error("Bumblezone Error: Couldn't parse bee queen trades file {}", fileIdentifier, e);
            }
        });
    }

    // KEEP THIS HERE BECAUSE ABOVE FIRES BEFORE TAGS ARE READY
    public void resolveQueenTrades(final TagsUpdatedEvent event) {
        if (tradeRaw.isEmpty()) {
            return;
        }

        List<TradeEntryReducedObj> randomizerTrades = new ArrayList<>();
        Object2ObjectOpenHashMap<Item, WeightedRandomList<TradeEntryReducedObj>> reducedTradeMap = new Object2ObjectOpenHashMap<>();
        for (Map.Entry<Pair<List<TradeEntryObj>, Boolean>, List<TradeEntryObj>> entry : tradeRaw.entrySet()) {

            ArrayList<Item> wants = new ArrayList<>();
            List<TradeEntryReducedObj> rewards = new ArrayList<>();
            entry.getKey().getFirst().forEach((value) -> {
                Set<Item> items = null;
                if (value.id.startsWith("#")) {
                    ResourceLocation tagRl = new ResourceLocation(value.id.substring(1));
                    TagKey<Item> itemTag = TagKey.create(Registries.ITEM, tagRl);
                    Optional<HolderSet.Named<Item>> taggedItems = BuiltInRegistries.ITEM.getTag(itemTag);
                    if (taggedItems.isPresent()) {
                        items = taggedItems.get().stream().map(Holder::value).collect(Collectors.toSet());
                    }
                    else if (value.isRequired()) {
                        Bumblezone.LOGGER.error("Bumblezone Error: Couldn't find want item tag {} in bee queen trades file", value.id);
                    }
                }
                else {
                    Optional<Item> item = BuiltInRegistries.ITEM.getOptional(new ResourceLocation(value.id));
                    if (item.isPresent()) {
                        items = Set.of(item.get());
                    }
                    else if (value.isRequired()) {
                        Bumblezone.LOGGER.error("Bumblezone Error: Couldn't find want item {} in bee queen trades file", value.id);
                    }
                }

                if (items == null) {
                    return;
                }

                wants.addAll(items);
            });

            if (wants.isEmpty()) {
                continue;
            }

            AtomicInteger totalGroupWeight = new AtomicInteger();
            entry.getValue().forEach(value -> {
                if (value.id.startsWith("#")) {
                    ResourceLocation tagRl = new ResourceLocation(value.id.substring(1));
                    TagKey<Item> itemTag = TagKey.create(Registries.ITEM, tagRl);
                    Optional<HolderSet.Named<Item>> taggedItems = BuiltInRegistries.ITEM.getTag(itemTag);
                    taggedItems.ifPresent(holders -> totalGroupWeight.addAndGet(value.getWeight() * holders.size()));
                }
                else {
                    Optional<Holder.Reference<Item>> item = BuiltInRegistries.ITEM.getHolder(ResourceKey.create(Registries.ITEM, new ResourceLocation(value.id)));
                    item.ifPresent(holder -> totalGroupWeight.addAndGet(value.getWeight()));
                }
            });

            if (entry.getKey().getSecond()) {
                randomizerTrades.add(new TradeEntryReducedObj(wants, 1, 0, 1));
            }

            entry.getValue().forEach((value) -> {
                if (value.id.startsWith("#")) {
                    ResourceLocation tagRl = new ResourceLocation(value.id.substring(1));
                    TagKey<Item> itemTag = TagKey.create(Registries.ITEM, tagRl);
                    Optional<HolderSet.Named<Item>> taggedItems = BuiltInRegistries.ITEM.getTag(itemTag);
                    if (taggedItems.isPresent()) {
                        List<Item> rewardGroup = taggedItems.get().stream().map(Holder::value).collect(Collectors.toList());
                        rewards.add(new TradeEntryReducedObj(rewardGroup, value.getCount(), value.getXpReward(), value.getWeight() * rewardGroup.size(), totalGroupWeight.get(), entry.getKey().getSecond()));
                    }
                    else if (value.isRequired()) {
                        Bumblezone.LOGGER.error("Bumblezone Error: Couldn't find reward item tag {} in bee queen trades file", value.id);
                    }
                }
                else {
                    Optional<Item> item = BuiltInRegistries.ITEM.getOptional(new ResourceLocation(value.id));
                    if (item.isPresent()) {
                        rewards.add(new TradeEntryReducedObj(List.of(item.get()), value.getCount(), value.getXpReward(), value.getWeight(), totalGroupWeight.get(), entry.getKey().getSecond()));
                    }
                    else if (value.isRequired()) {
                        Bumblezone.LOGGER.error("Bumblezone Error: Couldn't find reward item {} in bee queen trades file", value.id);
                    }
                }
            });

            if (rewards.isEmpty()) {
                continue;
            }

            for (Item item : wants) {
                WeightedRandomList<TradeEntryReducedObj> existingRewards = reducedTradeMap.get(item);
                if (existingRewards == null) {
                    reducedTradeMap.put(item, WeightedRandomList.create(rewards));
                }
                else {
                    List<TradeEntryReducedObj> newWants = new ArrayList<>();
                    newWants.addAll(existingRewards.unwrap());
                    newWants.addAll(rewards);
                    reducedTradeMap.put(item, WeightedRandomList.create(newWants));
                }
            }
        }
        tradeReduced = reducedTradeMap;
        tradeRandomizer = randomizerTrades;
        tradeRaw.clear();
    }
}
package com.grubielauncher.companion;

import com.grubielauncher.companion.core.ServerSnapshot;
import com.grubielauncher.companion.mc.Mc;
import com.grubielauncher.companion.mixin.ClientAdvancementsAccessor;
import com.grubielauncher.companion.mixin.StatsCounterAccessor;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatsCounter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StatsReader {
    private StatsReader() {
    }

    public static Map<String, Map<String, Integer>> read(StatsCounter counter) {
        Map<String, Map<String, Integer>> result = new HashMap<>();
        for (Object2IntMap.Entry<Stat<?>> entry : ((StatsCounterAccessor) counter).grubie$stats().object2IntEntrySet()) {
            int value = entry.getIntValue();
            if (value == 0) continue;
            Stat<?> stat = entry.getKey();
            String type = String.valueOf(BuiltInRegistries.STAT_TYPE.getKey(stat.getType()));
            String key = keyOf(stat);
            if (type.equals("null") || key.equals("null")) continue;
            result.computeIfAbsent(type, ignored -> new HashMap<>()).put(key, value);
        }
        return result;
    }

    public static Map<String, ServerSnapshot.Advancement> advancements(ClientAdvancements advancements) {
        Map<String, ServerSnapshot.Advancement> result = new HashMap<>();
        Map<?, AdvancementProgress> progress = ((ClientAdvancementsAccessor) advancements).grubie$progress();
        for (Map.Entry<?, AdvancementProgress> entry : progress.entrySet()) {
            String id = Mc.advancementId(entry.getKey());
            AdvancementProgress value = entry.getValue();
            if (id == null || value == null || id.contains(":recipes/")) continue;
            List<String> criteria = new ArrayList<>();
            for (String criterion : value.getCompletedCriteria()) criteria.add(criterion);
            if (criteria.isEmpty() && !value.isDone()) continue;
            result.put(id, new ServerSnapshot.Advancement(value.isDone(), criteria));
        }
        return result;
    }

    private static <T> String keyOf(Stat<T> stat) {
        return String.valueOf(stat.getType().getRegistry().getKey(stat.getValue()));
    }
}

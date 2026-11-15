package com.toa.gearinspector;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;

final class GearSnapshot
{
    final Map<GearSlot, Integer> items;
    final String unavailableReason;

    private GearSnapshot(Map<GearSlot, Integer> items, String unavailableReason)
    {
        this.items = Collections.unmodifiableMap(new EnumMap<>(items));
        this.unavailableReason = unavailableReason;
    }

    static GearSnapshot unavailable(String reason)
    {
        return new GearSnapshot(new EnumMap<>(GearSlot.class), reason);
    }

    static GearSnapshot read(Player player)
    {
        if (player == null)
        {
            return unavailable("Out of range / different world");
        }
        PlayerComposition composition = player.getPlayerComposition();
        if (composition == null || composition.getTransformedNpcId() != -1)
        {
            return unavailable("Player appearance unavailable");
        }
        EnumMap<GearSlot, Integer> items = new EnumMap<>(GearSlot.class);
        for (GearSlot slot : GearSlot.values())
        {
            if (slot.kit != null)
            {
                // getEquipmentId already decodes ITEM_OFFSET, which has changed historically.
                // Do not subtract 512/2048 here, or mix in body-part kit IDs.
                items.put(slot, composition.getEquipmentId(slot.kit));
            }
        }
        return new GearSnapshot(items, null);
    }

    boolean available()
    {
        return unavailableReason == null;
    }
}

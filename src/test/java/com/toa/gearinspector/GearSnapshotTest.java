package com.toa.gearinspector;

import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.kit.KitType;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GearSnapshotTest
{
    @Test
    public void readsDecodedItemsAndNeverInventsRingOrAmmo()
    {
        Player player = mock(Player.class);
        PlayerComposition composition = mock(PlayerComposition.class);
        when(player.getPlayerComposition()).thenReturn(composition);
        when(composition.getTransformedNpcId()).thenReturn(-1);
        when(composition.getEquipmentId(any())).thenReturn(-1);
        when(composition.getEquipmentId(KitType.WEAPON)).thenReturn(27275);
        GearSnapshot result = GearSnapshot.read(player);
        assertTrue(result.available());
        assertEquals(Integer.valueOf(27275), result.items.get(GearSlot.WEAPON));
        assertEquals(Integer.valueOf(-1), result.items.get(GearSlot.SHIELD));
        assertEquals(9, GearSlot.values().length);
        assertEquals(9, result.items.size());
        verify(composition, never()).getEquipmentIds();
    }

    @Test
    public void missingOrTransformedPlayerIsUnavailable()
    {
        assertFalse(GearSnapshot.read(null).available());
        Player player = mock(Player.class);
        assertFalse(GearSnapshot.read(player).available());
        PlayerComposition composition = mock(PlayerComposition.class);
        when(player.getPlayerComposition()).thenReturn(composition);
        when(composition.getTransformedNpcId()).thenReturn(1);
        assertFalse(GearSnapshot.read(player).available());
    }

    @Test
    public void matchesFormattingWithoutChangingIdentity()
    {
        assertEquals("my name", Names.key("<img=2><col=ffffff>MY\u00a0Name</col>"));
        assertEquals(Names.key("MY_Name"), Names.key("my name"));
        assertEquals("", Names.key(null));
        assertNotEquals(Names.key("abc"), Names.key("abcd"));
        assertFalse(Names.isPlayerName("-"));
        assertFalse(Names.isPlayerName("<col=777777> - </col>"));
        assertFalse(Names.isPlayerName("\u00a0"));
        assertFalse(Names.isPlayerName(null));
        assertTrue(Names.isPlayerName("H 0 V"));
        assertTrue(Names.isPlayerName("dingle burry"));
    }
}

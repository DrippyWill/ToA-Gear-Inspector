package com.toa.gearinspector;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.kit.KitType;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PartyInterfaceTest
{
    private Client client;
    private ToaGearInspectorConfig config;
    private PartyInterface subject;
    private Widget applicants;
    private Widget list;
    private Widget hostControl;
    private Widget header;

    @Before
    public void setup()
    {
        client = mock(Client.class);
        config = mock(ToaGearInspectorConfig.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getPlayers()).thenReturn(Collections.emptyList());
        applicants = widget(InterfaceID.ToaPartydetails.APPLICANTS, new Rectangle(10, 80, 470, 210));
        header = widget(InterfaceID.ToaPartydetails.APPLICANTS_NAME, new Rectangle(16, 90, 114, 32));
        list = widget(InterfaceID.ToaPartydetails.APPLICANTS_LIST, new Rectangle(16, 122, 458, 200));
        widget(InterfaceID.ToaPartydetails.APPLICANTS_LIST_CONTAINER, new Rectangle(16, 122, 458, 168));
        hostControl = widget(InterfaceID.ToaPartydetails.UNBLOCK, new Rectangle(200, 310, 70, 22));
        subject = new PartyInterface(client);
    }

    private Widget widget(int id, Rectangle bounds)
    {
        Widget widget = mock(Widget.class);
        when(widget.getBounds()).thenReturn(bounds);
        when(client.getWidget(id)).thenReturn(widget);
        return widget;
    }

    private Widget name(String text, int y)
    {
        Widget widget = mock(Widget.class);
        when(widget.getType()).thenReturn(WidgetType.TEXT);
        when(widget.getText()).thenReturn(text);
        AtomicInteger width = new AtomicInteger(114);
        when(widget.getOriginalWidth()).thenAnswer(invocation -> width.get());
        when(widget.setOriginalWidth(anyInt())).thenAnswer(invocation -> {
            width.set(invocation.getArgument(0));
            return widget;
        });
        when(widget.getBounds()).thenAnswer(invocation -> new Rectangle(16, y, width.get(), 22));
        return widget;
    }

    private void rows(Widget first, Widget second)
    {
        Widget[] children = new Widget[40];
        children[1] = first;
        children[21] = second;
        // A combat-level cell must never be interpreted as an applicant.
        children[2] = name("126", 124);
        when(list.getChildren()).thenReturn(children);
    }

    private void beginAutomaticRebuild()
    {
        widget(InterfaceID.ToaPartydetails.CONTENTS, new Rectangle(10, 40, 484, 350));
        when(client.getGameCycle()).thenReturn(500);
        subject.onScriptPre(PartyInterface.AUTO_REFRESH_SCRIPT,
            new Object[]{PartyInterface.AUTO_REFRESH_SCRIPT, 0, 0, 0, 0});
        subject.onScriptPre(PartyInterface.INIT_SCRIPT, null);
        when(applicants.isHidden()).thenReturn(true);
        when(list.getChildren()).thenReturn(new Widget[0]);
    }

    @Test
    public void automaticRefreshKeepsLastCompleteRowsUntilFinalScriptCommits()
    {
        rows(name("Alice", 124), name("Bob", 146));
        subject.update();
        Rectangle before = subject.header();
        beginAutomaticRebuild();
        subject.update();
        assertEquals(before, subject.header());
        assertEquals("Alice", subject.rows().get(0).name);
        assertEquals(2, subject.rows().size());
        // A partial rebuild must not replace the previous complete roster.
        when(applicants.isHidden()).thenReturn(false);
        rows(name("Bob", 124), null);
        subject.update();
        assertEquals("Alice", subject.rows().get(0).name);
        assertEquals(2, subject.rows().size());
        // Completion can arrive after this frame's BeforeRender: commit synchronously.
        subject.onScriptPost(PartyInterface.COMPLETE_SCRIPT);
        assertEquals(1, subject.rows().size());
        assertEquals("Bob", subject.rows().get(0).name);
    }

    @Test
    public void earlyTimerCallsAndManualChangesAreNeverBridged()
    {
        rows(name("Alice", 124), null);
        subject.update();
        widget(InterfaceID.ToaPartydetails.CONTENTS, new Rectangle(10, 40, 484, 350));
        when(client.getGameCycle()).thenReturn(499);
        subject.onScriptPre(PartyInterface.AUTO_REFRESH_SCRIPT,
            new Object[]{PartyInterface.AUTO_REFRESH_SCRIPT, 0, 0, 0, 0});
        subject.onScriptPre(PartyInterface.INIT_SCRIPT, null);
        when(applicants.isHidden()).thenReturn(true);
        subject.update();
        assertNull(subject.header());
        when(applicants.isHidden()).thenReturn(false);
        subject.update();
        beginAutomaticRebuild();
        subject.cancelRefresh(); // A native tab/accept/reject click cancels the bridge.
        subject.update();
        assertNull(subject.header());
        assertTrue(subject.rows().isEmpty());
    }

    @Test
    public void refreshTimeoutAndClosingOrMovingTheWindowDoNotLeaveGhostRows()
    {
        for (int scenario = 0; scenario < 3; scenario++)
        {
            when(applicants.isHidden()).thenReturn(false);
            rows(name("Alice", 124), null);
            subject.update();
            beginAutomaticRebuild();
            if (scenario == 0)
            {
                when(client.getGameCycle()).thenReturn(500 + PartyInterface.MAX_REFRESH_CYCLES + 1);
            }
            else if (scenario == 1)
            {
                when(client.getWidget(InterfaceID.ToaPartydetails.CONTENTS)).thenReturn(null);
            }
            else
            {
                when(client.getWidget(InterfaceID.ToaPartydetails.CONTENTS).getBounds())
                    .thenReturn(new Rectangle(20, 40, 484, 350));
            }
            subject.update();
            assertNull(subject.header());
            assertTrue(subject.rows().isEmpty());
        }
    }

    @Test
    public void completedEmptyRosterClearsImmediatelyAndGearStaysLiveDuringRebuild()
    {
        rows(name("Alice", 124), null);
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Alice");
        PlayerComposition composition = mock(PlayerComposition.class);
        when(player.getPlayerComposition()).thenReturn(composition);
        when(composition.getTransformedNpcId()).thenReturn(-1);
        when(composition.getEquipmentId(any())).thenReturn(-1);
        when(client.getPlayers()).thenReturn(Arrays.asList(player));
        subject.update();
        assertTrue(subject.rows().get(0).gear.available());
        beginAutomaticRebuild();
        when(client.getPlayers()).thenReturn(Collections.emptyList());
        subject.update();
        assertFalse(subject.rows().get(0).gear.available());
        when(applicants.isHidden()).thenReturn(false);
        subject.onScriptPost(PartyInterface.COMPLETE_SCRIPT);
        assertTrue(subject.rows().isEmpty());
    }

    @Test
    public void malformedTimerArgumentsDoNotArmARefresh()
    {
        rows(name("Alice", 124), null);
        subject.update();
        subject.onScriptPre(PartyInterface.AUTO_REFRESH_SCRIPT, null);
        subject.onScriptPre(PartyInterface.AUTO_REFRESH_SCRIPT, new Object[]{1, 2});
        subject.onScriptPre(PartyInterface.INIT_SCRIPT, null);
        when(applicants.isHidden()).thenReturn(true);
        subject.update();
        assertNull(subject.header());
    }

    @Test
    public void mapsCurrentNamesAfterSortAndRestoresOnClose()
    {
        Widget alice = name("Alice", 124);
        Widget bob = name("Bob", 146);
        rows(alice, bob);
        subject.update();
        assertEquals(2, subject.rows().size());
        assertEquals("Alice", subject.rows().get(0).name);
        assertEquals(88, alice.getOriginalWidth());
        assertEquals(new Rectangle(104, 124, 26, 22), subject.rows().get(0).gearBounds);
        subject.update();
        assertEquals(88, alice.getOriginalWidth()); // never shrinks cumulatively
        rows(bob, alice);
        subject.update();
        assertEquals("Bob", subject.rows().get(0).name);
        when(applicants.isHidden()).thenReturn(true);
        subject.update();
        assertTrue(subject.rows().isEmpty());
        assertNull(subject.header());
        assertEquals(114, alice.getOriginalWidth());
        verify(alice, never()).setText(anyString());
        verify(header, never()).setOriginalWidth(anyInt());
    }

    @Test
    public void applicantsAreInspectableWithoutHostControls()
    {
        rows(name("Alice", 124), null);
        when(hostControl.isHidden()).thenReturn(true);
        subject.update();
        assertEquals(1, subject.rows().size());
    }

    @Test
    public void readsMembersInOtherPartiesUsingThirteenChildStrideAndSkipsEmptySeats()
    {
        when(applicants.isHidden()).thenReturn(true);
        when(hostControl.isHidden()).thenReturn(true);
        widget(InterfaceID.ToaPartydetails.MEMBERS, new Rectangle(10, 80, 470, 210));
        widget(InterfaceID.ToaPartydetails.MEMBERS_NAME, new Rectangle(16, 90, 114, 32));
        widget(InterfaceID.ToaPartydetails.MEMBERS_LIST_CONTAINER, new Rectangle(16, 122, 458, 168));
        Widget members = widget(InterfaceID.ToaPartydetails.MEMBERS_LIST, new Rectangle(16, 122, 458, 168));
        Widget alice = name("Alice", 124);
        Widget bob = name("Bob", 146);
        Widget[] children = new Widget[65];
        children[1] = alice;
        children[14] = bob;
        Widget dash = name("-", 168);
        children[27] = dash;
        children[40] = name("", 190);
        children[53] = name("<col=777777> - </col>", 212);
        children[2] = name("126", 124);
        children[11] = mock(Widget.class); // Native reorder controls are not name cells.
        when(members.getChildren()).thenReturn(children);
        subject.update();
        assertEquals(2, subject.rows().size());
        assertEquals("Alice", subject.rows().get(0).name);
        assertEquals("Bob", subject.rows().get(1).name);
        assertEquals(88, alice.getOriginalWidth());
        assertEquals(114, dash.getOriginalWidth());
        children[1] = bob;
        children[14] = alice;
        subject.update();
        assertEquals("Bob", subject.rows().get(0).name);
    }

    @Test
    public void clearingOccupiedSeatRemovesItsHoverRowAndRestoresWidth()
    {
        Widget occupant = name("Alice", 124);
        rows(occupant, null);
        subject.update();
        assertEquals(1, subject.rows().size());
        assertEquals(88, occupant.getOriginalWidth());
        when(occupant.getText()).thenReturn("<col=777777>-</col>");
        subject.update();
        assertTrue(subject.rows().isEmpty());
        assertEquals(114, occupant.getOriginalWidth());
        when(occupant.getText()).thenReturn("Bob");
        subject.update();
        assertEquals("Bob", subject.rows().get(0).name);
        assertEquals(88, occupant.getOriginalWidth());
    }

    @Test
    public void switchingBetweenTablesRestoresOldLabelsAndUsesTheNewTable()
    {
        Widget applicant = name("Applicant", 124);
        rows(applicant, null);
        subject.update();
        assertEquals(88, applicant.getOriginalWidth());
        when(applicants.isHidden()).thenReturn(true);
        Widget membersPanel = widget(InterfaceID.ToaPartydetails.MEMBERS, new Rectangle(10, 80, 470, 210));
        widget(InterfaceID.ToaPartydetails.MEMBERS_NAME, new Rectangle(16, 90, 114, 32));
        widget(InterfaceID.ToaPartydetails.MEMBERS_LIST_CONTAINER, new Rectangle(16, 122, 458, 168));
        Widget members = widget(InterfaceID.ToaPartydetails.MEMBERS_LIST, new Rectangle(16, 122, 458, 168));
        Widget member = name("Member", 124);
        when(members.getChildren()).thenReturn(new Widget[]{null, member});
        subject.update();
        assertEquals("Member", subject.rows().get(0).name);
        assertEquals(114, applicant.getOriginalWidth());
        assertEquals(88, member.getOriginalWidth());
        when(membersPanel.isHidden()).thenReturn(true);
        when(applicants.isHidden()).thenReturn(false);
        subject.update();
        assertEquals("Applicant", subject.rows().get(0).name);
        assertEquals(114, member.getOriginalWidth());
        assertEquals(88, applicant.getOriginalWidth());
        when(applicants.isHidden()).thenReturn(true); // Invocations / Summary.
        subject.update();
        assertNull(subject.header());
        assertEquals(114, applicant.getOriginalWidth());
    }

    @Test
    public void clipsScrolledRowsAndRestoresRebuiltWidgets()
    {
        Widget previous = name("Old", 124);
        rows(previous, null);
        subject.update();
        Widget offscreen = name("Hidden", 98);
        Widget partial = name("Visible", 282);
        rows(offscreen, partial);
        subject.update();
        assertEquals(114, previous.getOriginalWidth());
        assertEquals(1, subject.rows().size());
        assertEquals(new Rectangle(104, 282, 26, 8), subject.rows().get(0).gearBounds);
        subject.clear();
        assertEquals(114, partial.getOriginalWidth());
    }

    @Test
    public void readsNewEquipmentAndDropsItImmediatelyWhenPlayerLeaves()
    {
        rows(name("<col=ff0000>Alice</col>", 124), null);
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("ALICE");
        PlayerComposition composition = mock(PlayerComposition.class);
        when(player.getPlayerComposition()).thenReturn(composition);
        when(composition.getTransformedNpcId()).thenReturn(-1);
        when(composition.getEquipmentId(any())).thenReturn(-1);
        when(composition.getEquipmentId(KitType.WEAPON)).thenReturn(4151);
        when(client.getPlayers()).thenReturn(Arrays.asList(player));
        subject.update();
        assertEquals(Integer.valueOf(4151), subject.rows().get(0).gear.items.get(GearSlot.WEAPON));
        when(composition.getEquipmentId(KitType.WEAPON)).thenReturn(27275);
        subject.update();
        assertEquals(Integer.valueOf(27275), subject.rows().get(0).gear.items.get(GearSlot.WEAPON));
        when(client.getPlayers()).thenReturn(Collections.emptyList());
        subject.update();
        assertFalse(subject.rows().get(0).gear.available());
        assertTrue(subject.rows().get(0).gear.items.isEmpty());
    }

    @Test
    public void logoutEmptyListAndMissingWidgetsAreSafe()
    {
        subject.update();
        assertTrue(subject.rows().isEmpty());
        when(client.getGameState()).thenReturn(GameState.HOPPING);
        subject.update();
        assertNull(subject.header());
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getWidget(InterfaceID.ToaPartydetails.APPLICANTS)).thenReturn(null);
        subject.update();
        assertTrue(subject.rows().isEmpty());
    }
}

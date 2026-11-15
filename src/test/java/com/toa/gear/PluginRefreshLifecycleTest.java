package com.toa.gear;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.game.ItemManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Replays the actual WidgetClosed -> init -> complete sequence from the user's client log. */
public class PluginRefreshLifecycleTest
{
    private Client client;
    private ApplicantInterface view;
    private ApplicantGearOverlay overlay;
    private ToaApplicantGearPlugin plugin;
    private Widget members;
    private Widget name;
    private AtomicInteger cycle;
    private AtomicInteger rowY;
    private AtomicReference<Rectangle> headerBounds;

    @Before
    public void setup() throws Exception
    {
        client = mock(Client.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getPlayers()).thenReturn(Collections.emptyList());
        when(client.getCanvasWidth()).thenReturn(765);
        when(client.getCanvasHeight()).thenReturn(503);
        when(client.getMouseCanvasPosition()).thenReturn(new Point(115, 134));
        cycle = new AtomicInteger(6243);
        when(client.getGameCycle()).thenAnswer(i -> cycle.get());
        ToaApplicantGearConfig config = mock(ToaApplicantGearConfig.class);
        when(config.hoverName()).thenReturn(true);
        members = widget(InterfaceID.ToaPartydetails.MEMBERS, new Rectangle(10, 80, 470, 210));
        headerBounds = new AtomicReference<>(new Rectangle(16, 90, 114, 32));
        Widget header = widget(InterfaceID.ToaPartydetails.MEMBERS_NAME, headerBounds.get());
        when(header.getBounds()).thenAnswer(i -> headerBounds.get());
        widget(InterfaceID.ToaPartydetails.MEMBERS_LIST_CONTAINER, new Rectangle(16, 122, 458, 168));
        Widget list = widget(InterfaceID.ToaPartydetails.MEMBERS_LIST, new Rectangle(16, 122, 458, 168));
        rowY = new AtomicInteger(124);
        name = occupant();
        when(list.getChildren()).thenReturn(new Widget[]{null, name});
        view = new ApplicantInterface(client);
        overlay = new ApplicantGearOverlay(client, config, view,
            new GearPanel(mock(ItemManager.class)));
        plugin = new ToaApplicantGearPlugin();
        inject("applicantInterface", view);
        inject("overlay", overlay);
        inject("running", true);
        plugin.onBeforeRender(new BeforeRender());
        frame();
        when(client.getMouseCanvasPosition()).thenReturn(new Point(150, 155)); // Inside popup, off the row.
    }

    private Widget widget(int id, Rectangle bounds)
    {
        Widget widget = mock(Widget.class);
        when(widget.getBounds()).thenReturn(bounds);
        when(client.getWidget(id)).thenReturn(widget);
        return widget;
    }

    private void inject(String name, Object value) throws Exception
    {
        Field field = ToaApplicantGearPlugin.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(plugin, value);
    }

    private Widget occupant()
    {
        Widget widget = mock(Widget.class);
        when(widget.getType()).thenReturn(WidgetType.TEXT);
        when(widget.getText()).thenReturn("Alice");
        AtomicInteger width = new AtomicInteger(114);
        when(widget.getOriginalWidth()).thenAnswer(i -> width.get());
        when(widget.setOriginalWidth(anyInt())).thenAnswer(i -> {
            width.set(i.getArgument(0));
            return widget;
        });
        when(widget.getBounds()).thenAnswer(i -> new Rectangle(16, rowY.get(), width.get(), 22));
        return widget;
    }

    private BufferedImage frame()
    {
        BufferedImage image = new BufferedImage(765, 503, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        overlay.render(g);
        g.dispose();
        return image;
    }

    private void reload()
    {
        reload(false);
    }

    private void reload(boolean unload)
    {
        cycle.set(6244);
        plugin.onWidgetClosed(new WidgetClosed(InterfaceID.TOA_PARTYDETAILS, 0, unload));
        if (unload)
        {
            // A full reload can replace every native widget object, not just their rows.
            members = widget(InterfaceID.ToaPartydetails.MEMBERS, new Rectangle(10, 80, 470, 210));
            Widget header = widget(InterfaceID.ToaPartydetails.MEMBERS_NAME, headerBounds.get());
            when(header.getBounds()).thenAnswer(i -> headerBounds.get());
            widget(InterfaceID.ToaPartydetails.MEMBERS_LIST_CONTAINER, new Rectangle(16, 122, 458, 168));
            Widget list = widget(InterfaceID.ToaPartydetails.MEMBERS_LIST, new Rectangle(16, 122, 458, 168));
            name = occupant();
            when(list.getChildren()).thenReturn(new Widget[]{null, name});
        }
        plugin.onScriptPreFired(new ScriptPreFired(ApplicantInterface.INIT_SCRIPT));
        rowY.set(-22); // Native rows exist but their draw coordinates are not yet usable.
        headerBounds.set(new Rectangle(0, 0, 114, 32));
        plugin.onScriptPostFired(new ScriptPostFired(ApplicantInterface.COMPLETE_SCRIPT));
    }

    @Test
    public void loggedReloadSequencePreservesAllOverlayPixelsAndHoverAcrossHighFpsFrames()
    {
        verifyReload(false);
    }

    @Test
    public void sameCycleReopenAlsoPreservesHoverWhenOldWidgetsAreUnloaded()
    {
        verifyReload(true);
    }

    private void verifyReload(boolean unload)
    {
        BufferedImage before = frame();
        reload(unload);
        for (int i = 0; i < 4; i++)
        {
            plugin.onBeforeRender(new BeforeRender());
            BufferedImage during = frame();
            assertArrayEquals(before.getRGB(0, 0, 765, 503, null, 0, 765),
                during.getRGB(0, 0, 765, 503, null, 0, 765));
            assertTrue(overlay.blocks(new java.awt.Point(150, 155)));
            assertEquals(88, name.getOriginalWidth());
        }
        cycle.incrementAndGet();
        rowY.set(124);
        headerBounds.set(new Rectangle(16, 90, 114, 32));
        plugin.onBeforeRender(new BeforeRender());
        BufferedImage after = frame();
        assertArrayEquals(before.getRGB(0, 0, 765, 503, null, 0, 765),
            after.getRGB(0, 0, 765, 503, null, 0, 765));
        assertFalse(view.awaitingReloadLayout());
    }

    @Test
    public void actualUnloadClearsBeforeTheNextPaint()
    {
        frame();
        plugin.onWidgetClosed(new WidgetClosed(InterfaceID.TOA_PARTYDETAILS, 0, true));
        when(members.isHidden()).thenReturn(true);
        plugin.onBeforeRender(new BeforeRender());
        assertNull(view.header());
        assertTrue(view.rows().isEmpty());
        assertFalse(overlay.blocks(new java.awt.Point(150, 155)));
    }

    @Test
    public void nonUnloadingCloseWithoutAVisibleTableDoesNotLeaveGhosts()
    {
        plugin.onWidgetClosed(new WidgetClosed(InterfaceID.TOA_PARTYDETAILS, 0, false));
        when(members.isHidden()).thenReturn(true);
        plugin.onBeforeRender(new BeforeRender());
        assertNull(view.header());
        assertFalse(overlay.blocks(new java.awt.Point(150, 155)));
    }

    @Test
    public void emptySeatAfterReloadLosesInspectionOnTheFirstSettledCycle()
    {
        reload();
        when(name.getText()).thenReturn("-");
        cycle.incrementAndGet();
        rowY.set(124);
        headerBounds.set(new Rectangle(16, 90, 114, 32));
        plugin.onBeforeRender(new BeforeRender());
        frame();
        assertTrue(view.rows().isEmpty());
        assertFalse(overlay.blocks(new java.awt.Point(150, 155)));
        assertEquals(114, name.getOriginalWidth());
    }

    @Test
    public void nativeTabInteractionCancelsPreservationAndClearsTheCard()
    {
        reload();
        MenuEntry entry = mock(MenuEntry.class);
        when(entry.getParam1()).thenReturn(InterfaceID.ToaPartydetails.INVOCATIONS_TAB);
        MenuOptionClicked click = new MenuOptionClicked(entry);
        plugin.onMenuOptionClicked(click);
        when(members.isHidden()).thenReturn(true);
        plugin.onBeforeRender(new BeforeRender());
        assertNull(view.header());
        assertFalse(view.awaitingReloadLayout());
        assertFalse(overlay.blocks(new java.awt.Point(150, 155)));
    }
}

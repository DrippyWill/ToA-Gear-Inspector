package com.toa.gear;

import java.awt.Canvas;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import javax.imageio.ImageIO;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class OverlayInteractionTest
{
    private Client client;
    private ApplicantInterface applicants;
    private ApplicantGearOverlay overlay;
    private Widget list;
    private ItemManager items;

    @Before
    public void setup()
    {
        client = mock(Client.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getPlayers()).thenReturn(Collections.emptyList());
        when(client.getCanvasWidth()).thenReturn(765);
        when(client.getCanvasHeight()).thenReturn(503);
        when(client.getMouseCanvasPosition()).thenReturn(new Point(115, 134));
        ToaApplicantGearConfig config = mock(ToaApplicantGearConfig.class);
        when(config.hoverName()).thenReturn(true);
        widget(InterfaceID.ToaPartydetails.APPLICANTS, new Rectangle(10, 80, 470, 210));
        widget(InterfaceID.ToaPartydetails.APPLICANTS_NAME, new Rectangle(16, 90, 114, 32));
        widget(InterfaceID.ToaPartydetails.APPLICANTS_LIST_CONTAINER, new Rectangle(16, 122, 458, 168));
        widget(InterfaceID.ToaPartydetails.UNBLOCK, new Rectangle(16, 310, 80, 22));
        list = widget(InterfaceID.ToaPartydetails.APPLICANTS_LIST, new Rectangle(16, 122, 458, 168));
        Widget name = mock(Widget.class);
        when(name.getType()).thenReturn(WidgetType.TEXT);
        when(name.getText()).thenReturn("Alice");
        when(name.getBounds()).thenReturn(new Rectangle(16, 124, 88, 22));
        when(name.getOriginalWidth()).thenReturn(114);
        when(list.getChildren()).thenReturn(new Widget[]{null, name});
        applicants = new ApplicantInterface(client);
        items = mock(ItemManager.class);
        overlay = new ApplicantGearOverlay(client, config, applicants, new GearPanel(items));
        applicants.update();
    }

    private Widget widget(int id, Rectangle bounds)
    {
        Widget widget = mock(Widget.class);
        when(widget.getBounds()).thenReturn(bounds);
        when(client.getWidget(id)).thenReturn(widget);
        return widget;
    }

    private void render()
    {
        frame();
    }

    private BufferedImage frame()
    {
        BufferedImage image = new BufferedImage(765, 503, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        overlay.render(graphics);
        graphics.dispose();
        return image;
    }

    @Test
    public void overlayUsesOnlyTheFinalFramePassAndSurvivesALateNativeRepaint()
    {
        assertEquals(net.runelite.client.ui.overlay.OverlayLayer.ALWAYS_ON_TOP, overlay.getLayer());
        // The layer must not also register interface callbacks (which would paint it twice).
        assertTrue(overlay.getDrawHooks().isEmpty());
        BufferedImage result = new BufferedImage(765, 503, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        // Simulate an interface repaint after the earlier widget draw stage.
        g.setColor(java.awt.Color.BLACK);
        g.fillRect(0, 0, 765, 503);
        overlay.render(g); // Final frame pass, after the native repaint.
        g.dispose();
        assertNotEquals(java.awt.Color.BLACK.getRGB(), result.getRGB(112, 102));
        assertTrue(overlay.blocks(new java.awt.Point(150, 155)));
    }

    @Test
    public void entireOverlayIsPixelStableAcrossAnAutomaticRebuild()
    {
        widget(InterfaceID.ToaPartydetails.CONTENTS, new Rectangle(10, 40, 484, 350));
        BufferedImage before = frame();
        Widget[] completedRows = list.getChildren();
        when(client.getGameCycle()).thenReturn(500);
        applicants.onScriptPre(ApplicantInterface.AUTO_REFRESH_SCRIPT,
            new Object[]{ApplicantInterface.AUTO_REFRESH_SCRIPT, 0, 0, 0, 0});
        applicants.onScriptPre(ApplicantInterface.INIT_SCRIPT, null);
        when(client.getWidget(InterfaceID.ToaPartydetails.APPLICANTS).isHidden()).thenReturn(true);
        when(list.getChildren()).thenReturn(new Widget[0]);
        applicants.update();
        BufferedImage rebuilding = frame();
        assertArrayEquals(before.getRGB(0, 0, 765, 503, null, 0, 765),
            rebuilding.getRGB(0, 0, 765, 503, null, 0, 765));
        assertTrue(overlay.blocks(new java.awt.Point(150, 155)));
        when(client.getWidget(InterfaceID.ToaPartydetails.APPLICANTS).isHidden()).thenReturn(false);
        when(list.getChildren()).thenReturn(completedRows);
        applicants.onScriptPost(ApplicantInterface.COMPLETE_SCRIPT);
        BufferedImage after = frame();
        assertArrayEquals(before.getRGB(0, 0, 765, 503, null, 0, 765),
            after.getRGB(0, 0, 765, 503, null, 0, 765));
    }

    @Test
    public void popupPersistsOnCardAndDisappearsWhenApplicantIsRemoved()
    {
        render();
        java.awt.Point inside = new java.awt.Point(150, 155);
        assertTrue(overlay.blocks(inside));
        when(client.getMouseCanvasPosition()).thenReturn(new Point(150, 155));
        render();
        assertTrue(overlay.blocks(inside));
        when(list.getChildren()).thenReturn(new Widget[0]);
        applicants.update();
        render();
        assertFalse(overlay.blocks(inside));
    }

    @Test
    public void openNativeMenuHidesCard()
    {
        render();
        when(client.isMenuOpen()).thenReturn(true);
        render();
        assertFalse(overlay.blocks(new java.awt.Point(150, 155)));
    }

    @Test
    public void placeholderSeatCannotOpenOrRetainAnInspectionCard()
    {
        render();
        assertTrue(overlay.blocks(new java.awt.Point(150, 155)));
        when(list.getChildren()[1].getText()).thenReturn("-");
        applicants.update();
        render();
        assertTrue(applicants.rows().isEmpty());
        assertFalse(overlay.blocks(new java.awt.Point(150, 155)));
        when(client.getMouseCanvasPosition()).thenReturn(new Point(115, 134));
        render();
        assertFalse(overlay.blocks(new java.awt.Point(150, 155)));
    }

    @Test
    public void cardConsumesPressReleaseAndClickWithoutBlockingNativeArea()
    {
        render();
        GearMouseListener listener = new GearMouseListener(overlay);
        Canvas canvas = new Canvas();
        MouseEvent press = event(canvas, MouseEvent.MOUSE_PRESSED, 150, 155);
        listener.mousePressed(press);
        assertTrue(press.isConsumed());
        MouseEvent release = event(canvas, MouseEvent.MOUSE_RELEASED, 500, 450);
        listener.mouseReleased(release);
        assertTrue(release.isConsumed());
        MouseEvent click = event(canvas, MouseEvent.MOUSE_CLICKED, 150, 155);
        listener.mouseClicked(click);
        assertTrue(click.isConsumed());
        MouseEvent nativeButton = event(canvas, MouseEvent.MOUSE_PRESSED, 32, 320);
        listener.mousePressed(nativeButton);
        assertFalse(nativeButton.isConsumed());
    }

    private static MouseEvent event(Canvas source, int type, int x, int y)
    {
        return new MouseEvent(source, type, 0, 0, x, y, 1, false, MouseEvent.BUTTON1);
    }

    @Test
    public void rendersAvailableAndUnavailableCardsForVisualInspection() throws Exception
    {
        File folder = new File("build/qa");
        assertTrue(folder.isDirectory() || folder.mkdirs());
        GearPanel panel = new GearPanel(items);
        BufferedImage image = new BufferedImage(740, 310, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        Player player = mock(Player.class);
        PlayerComposition composition = mock(PlayerComposition.class);
        when(player.getPlayerComposition()).thenReturn(composition);
        when(composition.getTransformedNpcId()).thenReturn(-1);
        when(composition.getEquipmentId(any())).thenAnswer(invocation ->
            100 + ((net.runelite.api.kit.KitType) invocation.getArgument(0)).ordinal());
        String[] names = {"Torva full helm", "Infernal cape", "Amulet of rancour", "Tumeken's shadow",
            "Torva platebody", "Empty shield", "Torva platelegs", "Ferocious gloves", "Primordial boots"};
        int index = 0;
        for (GearSlot slot : GearSlot.values())
        {
            int id = 100 + slot.kit.ordinal();
            ItemComposition item = mock(ItemComposition.class);
            when(item.getName()).thenReturn(names[index++]);
            when(items.getItemComposition(id)).thenReturn(item);
            AsyncBufferedImage icon = new AsyncBufferedImage(mock(ClientThread.class), 36, 32, BufferedImage.TYPE_INT_ARGB);
            Graphics2D sprite = icon.createGraphics();
            sprite.setColor(new java.awt.Color(120 + index * 10, 110, 65));
            sprite.fillRect(0, 0, 36, 32); // Exercise the entire native sprite footprint, including its edges.
            sprite.setColor(java.awt.Color.WHITE);
            sprite.drawString(slot.label.substring(0, 1), 12, 21);
            sprite.dispose();
            when(items.getImage(id)).thenReturn(icon);
        }
        Rectangle anchor = new Rectangle(0, 0, 26, 22);
        panel.render(g, new Rectangle(5, 5, GearPanel.WIDTH, GearPanel.HEIGHT),
            new ApplicantRow("Wide Applicant", anchor, anchor, GearSnapshot.read(player)),
            new java.awt.Point(150, 85));
        panel.render(g, new Rectangle(375, 5, GearPanel.WIDTH, GearPanel.HEIGHT),
            new ApplicantRow("Out of range", anchor, anchor, GearSnapshot.read(null)),
            new java.awt.Point(0, 0));
        g.dispose();
        ImageIO.write(image, "png", new File(folder, "equipment-cards.png"));
        assertTrue(new File(folder, "equipment-cards.png").length() > 0);
    }
}

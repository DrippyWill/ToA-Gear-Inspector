package com.toa.gearinspector;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.GameState;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(name = "ToA Gear Inspector",
    description = "Inspect nearby members and applicants in any ToA party",
    tags = {"toa", "tombs of amascut", "party", "applicants", "gear"})
public class ToaGearInspectorPlugin extends Plugin
{
    @Inject private PartyInterface partyInterface;
    @Inject private GearInspectorOverlay overlay;
    @Inject private OverlayManager overlayManager;
    @Inject private ClientThread clientThread;
    @Inject private MouseManager mouseManager;
    @Inject private GearMouseListener mouseListener;
    private volatile boolean running;

    @Provides
    ToaGearInspectorConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ToaGearInspectorConfig.class);
    }

    @Override
    protected void startUp()
    {
        running = true;
        overlayManager.add(overlay);
        mouseManager.registerMouseListener(mouseListener);
    }

    @Override
    protected void shutDown()
    {
        running = false;
        overlayManager.remove(overlay);
        mouseManager.unregisterMouseListener(mouseListener);
        clientThread.invoke(() -> {
            if (!running)
            {
                partyInterface.clear();
                overlay.clearHover();
            }
        });
    }

    @Subscribe
    public void onBeforeRender(BeforeRender event)
    {
        if (running)
        {
            partyInterface.update();
            if (partyInterface.header() == null)
            {
                overlay.clearHover();
            }
        }
    }

    @Subscribe
    public void onScriptPreFired(ScriptPreFired event)
    {
        if (running)
        {
            partyInterface.onScriptPre(event.getScriptId(), event.getScriptEvent() == null
                ? null : event.getScriptEvent().getArguments());
        }
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event)
    {
        if (running)
        {
            partyInterface.onScriptPost(event.getScriptId());
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if ((event.getParam1() >>> 16) == InterfaceID.TOA_PARTYDETAILS)
        {
            // Never bridge an intentional tab/party change or a native accept/reject action.
            partyInterface.cancelRefresh();
            overlay.clearHover();
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() != GameState.LOGGED_IN)
        {
            partyInterface.clear();
            overlay.clearHover();
        }
    }

    @Subscribe
    public void onWidgetClosed(WidgetClosed event)
    {
        if (event.getGroupId() == InterfaceID.TOA_PARTYDETAILS)
        {
            // This event precedes the actual close and can be followed by reopening the
            // same group in the same cycle, even if its old widgets were unloaded.
            // Validate the live table in BeforeRender instead of clearing hover here.
            partyInterface.noteInterfaceClose();
        }
    }
}

package com.toa.gearinspector;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Launches a RuneLite development client with this local plugin registered. */
public final class ToaGearInspectorLauncher
{
    private ToaGearInspectorLauncher() { }

    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(ToaGearInspectorPlugin.class);
        RuneLite.main(args);
    }
}

package com.toa.gear;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Launches a RuneLite development client with this local plugin registered. */
public final class ToaApplicantGearLauncher
{
    private ToaApplicantGearLauncher() { }

    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(ToaApplicantGearPlugin.class);
        RuneLite.main(args);
    }
}

package com.toa.gear;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("toaApplicantGear")
public interface ToaApplicantGearConfig extends Config
{
    @ConfigItem(keyName = "hoverName", name = "Hover player names",
        description = "Also open the gear panel when hovering a member or applicant's name.", position = 0)
    default boolean hoverName()
    {
        return true;
    }

}

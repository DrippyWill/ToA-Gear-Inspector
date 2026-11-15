package com.toa.gearinspector;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("toaGearInspector")
public interface ToaGearInspectorConfig extends Config
{
    @ConfigItem(keyName = "hoverName", name = "Hover player names",
        description = "Also open the gear panel when hovering a member or applicant's name.", position = 0)
    default boolean hoverName()
    {
        return true;
    }

}

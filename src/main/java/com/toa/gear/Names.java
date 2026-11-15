package com.toa.gear;

import java.util.Locale;
import net.runelite.client.util.Text;

final class Names
{
    private Names() { }

    static String display(String name)
    {
        return name == null ? "" : Text.removeTags(name).replace('\u00a0', ' ').trim();
    }

    static String key(String name)
    {
        return display(name).replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    static boolean isPlayerName(String name)
    {
        // Empty native member seats are rendered as "-", sometimes with colour tags.
        // Real names contain at least one letter or digit; spaces / punctuation alone do not.
        return display(name).chars().anyMatch(Character::isLetterOrDigit);
    }
}

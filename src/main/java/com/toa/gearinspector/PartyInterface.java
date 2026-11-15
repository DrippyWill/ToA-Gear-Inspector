package com.toa.gearinspector;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;

/** Client-thread adapter for the native interface; never changes actions, text or sorting. */
@Singleton
final class PartyInterface
{
    static final int COLUMN_WIDTH = 26;
    // Native scripts: init clears rows; 6729 finishes sorting and reveals the selected tab.
    static final int INIT_SCRIPT = 6615;
    static final int COMPLETE_SCRIPT = 6729;
    static final int AUTO_REFRESH_SCRIPT = 6748;
    static final int AUTO_REFRESH_INTERVAL = 500;
    static final int MAX_REFRESH_CYCLES = 50;
    // Both native tables place names at +1, but have different row layouts.
    private enum Table
    {
        APPLICANTS(InterfaceID.ToaPartydetails.APPLICANTS, InterfaceID.ToaPartydetails.APPLICANTS_NAME,
            InterfaceID.ToaPartydetails.APPLICANTS_LIST, InterfaceID.ToaPartydetails.APPLICANTS_LIST_CONTAINER, 20),
        MEMBERS(InterfaceID.ToaPartydetails.MEMBERS, InterfaceID.ToaPartydetails.MEMBERS_NAME,
            InterfaceID.ToaPartydetails.MEMBERS_LIST, InterfaceID.ToaPartydetails.MEMBERS_LIST_CONTAINER, 13);

        final int panel;
        final int nameHeader;
        final int list;
        final int container;
        final int stride;

        Table(int panel, int nameHeader, int list, int container, int stride)
        {
            this.panel = panel;
            this.nameHeader = nameHeader;
            this.list = list;
            this.container = container;
            this.stride = stride;
        }
    }
    private final Client client;
    private final Map<Widget, Integer> originalWidths = new IdentityHashMap<>();
    private List<PartyRow> rows = Collections.emptyList();
    private Rectangle header;
    private Widget refreshRoot;
    private Rectangle refreshRootBounds;
    private int refreshStartCycle;
    private boolean rebuilding;
    private Table lastTable;
    private int reloadCycle = -1;

    @Inject
    PartyInterface(Client client)
    {
        this.client = client;
    }

    void update()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            clear();
            return;
        }
        if (refreshRoot != null && !validRefreshContext())
        {
            cancelAutomaticRefresh();
        }
        if (rebuilding)
        {
            // Retain only row identity/geometry during this explicit native transaction.
            // Equipment remains live, including becoming unavailable when a player leaves range.
            updateGearOnly();
            return;
        }
        Table table = null;
        for (Table candidate : Table.values())
        {
            if (visible(client.getWidget(candidate.panel)))
            {
                table = candidate;
                break;
            }
        }
        if (table == null)
        {
            clear();
            return;
        }
        if (header != null && table == lastTable && reloadCycle == client.getGameCycle())
        {
            // Same-group reloads reset cached widget positions during this cycle.
            // At high FPS several frames can render before the next layout cycle.
            prepareNameWidths(table);
            updateGearOnly();
            return;
        }
        reloadCycle = -1;
        Widget tablePanel = client.getWidget(table.panel);
        Widget nameHeader = client.getWidget(table.nameHeader);
        Widget list = client.getWidget(table.list);
        Widget container = client.getWidget(table.container);
        if (!visible(nameHeader) || !visible(list) || !visible(container))
        {
            clear();
            return;
        }

        Rectangle nameColumn = nameHeader.getBounds();
        if (nameColumn.width < 100 || nameColumn.height <= 0)
        {
            clear();
            return;
        }
        Rectangle clip = container.getBounds().intersection(tablePanel.getBounds());
        header = new Rectangle(nameColumn.x + nameColumn.width - COLUMN_WIDTH,
            nameColumn.y, COLUMN_WIDTH, nameColumn.height);
        Widget[] children = list.getChildren();
        List<PartyRow> next = new ArrayList<>();
        Set<Widget> active = Collections.newSetFromMap(new IdentityHashMap<>());
        Map<String, Player> nearby = nearbyPlayers();
        if (children != null)
        {
            for (int i = 1; i < children.length; i += table.stride)
            {
                Widget nameWidget = children[i];
                if (!visible(nameWidget) || nameWidget.getType() != WidgetType.TEXT)
                {
                    continue;
                }
                String name = Names.display(nameWidget.getText());
                if (!Names.isPlayerName(name))
                {
                    continue;
                }
                // Preserve the full native text for sorting, menu actions and player matching.
                reserveNameWidth(nameWidget);
                active.add(nameWidget);
                Rectangle nameBounds = nameWidget.getBounds();
                Rectangle gearBounds = new Rectangle(header.x, nameBounds.y, COLUMN_WIDTH, nameBounds.height);
                Rectangle clippedGear = gearBounds.intersection(clip);
                Rectangle clippedName = nameBounds.intersection(clip);
                if (clippedGear.width > 0 && clippedGear.height > 0)
                {
                    next.add(new PartyRow(name, clippedName, clippedGear,
                        GearSnapshot.read(nearby.get(Names.key(name)))));
                }
            }
        }
        // Rebuilds replace widget objects; restore/release the previous generation immediately.
        originalWidths.entrySet().removeIf(entry -> {
            if (active.contains(entry.getKey()))
            {
                return false;
            }
            restore(entry.getKey(), entry.getValue());
            return true;
        });
        rows = Collections.unmodifiableList(next);
        lastTable = table;
    }

    void onScriptPre(int scriptId, Object[] arguments)
    {
        if (scriptId == AUTO_REFRESH_SCRIPT)
        {
            // This timer script runs repeatedly, but only dispatches a refresh at 500 cycles.
            // Its root arguments are [script, universe, pausebuttons, refresh, startClock].
            if (arguments != null && arguments.length >= 5 && arguments[4] instanceof Integer
                && client.getGameCycle() - (Integer) arguments[4] >= AUTO_REFRESH_INTERVAL
                && header != null)
            {
                Widget root = client.getWidget(InterfaceID.ToaPartydetails.CONTENTS);
                if (client.getGameState() == GameState.LOGGED_IN && visible(root))
                {
                    refreshRoot = root;
                    refreshRootBounds = new Rectangle(root.getBounds());
                    refreshStartCycle = client.getGameCycle();
                    rebuilding = false;
                }
            }
        }
        else if (scriptId == INIT_SCRIPT)
        {
            rebuilding = refreshRoot != null && validRefreshContext();
            if (!rebuilding)
            {
                cancelAutomaticRefresh();
            }
        }
    }

    void onScriptPost(int scriptId)
    {
        if (scriptId == COMPLETE_SCRIPT)
        {
            cancelAutomaticRefresh();
            // On immediate reload the script finishes before widget draw coordinates settle.
            // Keep the previous coherent display through this cycle; sample again next cycle.
            if (reloadCycle != client.getGameCycle())
            {
                update();
            }
        }
    }

    void cancelRefresh()
    {
        cancelAutomaticRefresh();
        reloadCycle = -1;
    }

    void noteInterfaceClose()
    {
        if (header != null)
        {
            reloadCycle = client.getGameCycle();
        }
    }

    boolean awaitingReloadLayout()
    {
        return reloadCycle != -1;
    }

    private void cancelAutomaticRefresh()
    {
        refreshRoot = null;
        refreshRootBounds = null;
        rebuilding = false;
    }

    private void updateGearOnly()
    {
        Map<String, Player> nearby = nearbyPlayers();
        List<PartyRow> live = new ArrayList<>();
        for (PartyRow row : rows)
        {
            live.add(new PartyRow(row.name, row.nameBounds, row.gearBounds,
                GearSnapshot.read(nearby.get(Names.key(row.name)))));
        }
        rows = Collections.unmodifiableList(live);
    }

    private void prepareNameWidths(Table table)
    {
        Widget list = client.getWidget(table.list);
        Widget[] children = list == null ? null : list.getChildren();
        if (children != null)
        {
            for (int i = 1; i < children.length; i += table.stride)
            {
                Widget name = children[i];
                if (visible(name) && name.getType() == WidgetType.TEXT && Names.isPlayerName(name.getText()))
                {
                    reserveNameWidth(name);
                }
            }
        }
    }

    private void reserveNameWidth(Widget name)
    {
        int original = originalWidths.computeIfAbsent(name, Widget::getOriginalWidth);
        int width = Math.max(1, original - COLUMN_WIDTH);
        if (name.getOriginalWidth() != width)
        {
            name.setOriginalWidth(width);
            name.revalidate();
        }
    }

    private boolean validRefreshContext()
    {
        int elapsed = client.getGameCycle() - refreshStartCycle;
        Widget root = client.getWidget(InterfaceID.ToaPartydetails.CONTENTS);
        return elapsed >= 0 && elapsed <= MAX_REFRESH_CYCLES
            && root == refreshRoot && visible(root) && root.getBounds().equals(refreshRootBounds);
    }

    private Map<String, Player> nearbyPlayers()
    {
        Map<String, Player> nearby = new HashMap<>();
        for (Player player : client.getPlayers())
        {
            if (player != null && player.getName() != null)
            {
                nearby.put(Names.key(player.getName()), player);
            }
        }
        return nearby;
    }

    private static boolean visible(Widget widget)
    {
        return widget != null && !widget.isHidden();
    }

    private static void restore(Widget widget, int width)
    {
        widget.setOriginalWidth(width);
        widget.revalidate();
    }

    void clear()
    {
        cancelRefresh();
        originalWidths.forEach(PartyInterface::restore);
        originalWidths.clear();
        rows = Collections.emptyList();
        header = null;
        lastTable = null;
    }

    List<PartyRow> rows()
    {
        return rows;
    }

    Rectangle header()
    {
        return header == null ? null : new Rectangle(header);
    }
}

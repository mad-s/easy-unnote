package easyunnote;

import java.util.Arrays;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.ClientTick;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Easy Unnote"
)
public class EasyUnnotePlugin extends Plugin
{
	// List of objects that act as a bank, but don't have a "Bank" option
	private static final String[] UNNOTE_OBJECTS = {
		"Bank chest",
		"Bank Chest-wreck",
	};

	// List of NPC names that can unnote items, but don't have a "Bank" option
	private static final String[] UNNOTE_NPCS = {
		"Aisles",
		"Banknote Exchange Merchant",
		"Elder Chaos druid",
		"Phials",
		"Piles",
		"Tiles",
		// The following don't have an unnote option, but are useful to use noted items on
		"Wesley",
		"Rick",
		"Maid",
		"Cook",
		"Butler",
		"Demon butler",
		"Virilis",
	};

	private static final String[] BANKNOTE_LIKE = {
		"Looting bag note",
		"Rune pouch note",
	};

	@Inject
	private Client client;

	@Inject
	private EasyUnnoteConfig config;

	@Provides
	EasyUnnoteConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(EasyUnnoteConfig.class);
	}

	@Subscribe
	public void onClientTick(ClientTick clientTick)
	{
		// The menu is not rebuilt when it is open, so don't swap or else it will
		// repeatedly swap entries
		if (client.getGameState() != GameState.LOGGED_IN || client.isMenuOpen()) {
			return;
		}

		final Widget selectedWidget = client.getSelectedWidget();
		if (selectedWidget == null) {
			return;
		}
		if (selectedWidget.getId() != WidgetInfo.INVENTORY.getId()) {
			return;
		}

		final int itemId = selectedWidget.getItemId();
		if (itemId <= 0 || !client.isWidgetSelected()) {
			return;
		}

		final ItemComposition itemComposition = client.getItemDefinition(itemId);
		if (!canUnnote(itemComposition)) {
			return;
		}

		MenuEntry[] menuEntries = client.getMenuEntries();
		MenuEntry[] newEntries = Arrays.stream(menuEntries)
			.filter(e -> {
				switch (e.getType()) {
					case WIDGET_TARGET_ON_GROUND_ITEM:
					case WIDGET_TARGET_ON_PLAYER:
					case EXAMINE_NPC:
					case EXAMINE_OBJECT:
					case EXAMINE_ITEM_GROUND:
						return false;
					case WIDGET_TARGET_ON_GAME_OBJECT:
						final TileObject gameObject = findObject(e.getParam0(), e.getParam1(), e.getIdentifier());
						if (gameObject == null) {
							return false;
						}
						final ObjectComposition composition = client.getObjectDefinition(gameObject.getId());
						return canUnnote(composition);
					case WIDGET_TARGET_ON_NPC:
						final NPC npc = e.getNpc();
						return canUnnote(npc.getTransformedComposition());
					default:
						return true;
					}
			})
			.toArray(MenuEntry[]::new);
		
		client.setMenuEntries(newEntries);
	}

	private TileObject findObject(int x, int y, int id) {
		Scene scene = client.getScene();
		Tile[][][] tiles = scene.getTiles();
		Tile tile = tiles[client.getPlane()][x][y];
		if (tile != null)
		{
			for (GameObject gameObject : tile.getGameObjects())
			{
				if (gameObject != null && gameObject.getId() == id)
				{
					return gameObject;
				}
			}
			
			WallObject wallObject = tile.getWallObject();
			if (wallObject != null && wallObject.getId() == id)
			{
				return wallObject;
			}
		}

		return null;
	}

	private boolean canUnnote(ItemComposition item) {
		if (item.getNote() != -1) {
			return true;
		}
		return Arrays.stream(BANKNOTE_LIKE).anyMatch(item.getName()::equals);
	}

	private boolean canUnnote(ObjectComposition object) {
		if (object.getImpostorIds() != null) {
			// sus
			object = object.getImpostor();
		}
		if (Arrays.stream(UNNOTE_OBJECTS).anyMatch(object.getName()::equalsIgnoreCase)) {
			return true;
		}
		if ("Grand Exchange booth".equalsIgnoreCase(object.getName())) {
			return config.enableGEBooths();
		}
		if (object.getName().toLowerCase().endsWith("table") || "Counter".equalsIgnoreCase(object.getName())) {
			return config.enableTables();
		}
		return Arrays.stream(object.getActions())
			.anyMatch("Bank"::equalsIgnoreCase);
	}

	private boolean canUnnote(NPCComposition npc) {
		if (Arrays.stream(UNNOTE_NPCS).anyMatch(npc.getName()::equalsIgnoreCase)) {
			return true;
		}
		return Arrays.stream(npc.getActions())
			.anyMatch("Bank"::equalsIgnoreCase);
	}

}

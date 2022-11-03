package easyunnote;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("easyUnnote")
public interface EasyUnnoteConfig extends Config
{
	@ConfigItem(
		keyName = "enableGEBooths",
		name = "Enable GE Booths",
		description = "Allow unnoting at GE Booths."
	)
	default boolean enableGEBooths()
	{
		return false;
	}
}

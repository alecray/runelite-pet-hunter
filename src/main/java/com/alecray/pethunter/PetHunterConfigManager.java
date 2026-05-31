package com.alecray.pethunter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

/**
 * Reads/writes the plugin's persisted state through RuneLite's {@link ConfigManager}. State is
 * stored under the {@link PetHunterConfig#GROUP} group as JSON strings, so it lives per RuneLite
 * profile and therefore follows the logged-in account.
 */
@Slf4j
@Singleton
public class PetHunterConfigManager
{
	private static final String KEY_OBTAINED = "obtainedPets";
	private static final String KEY_TAGS = "customTags";
	private static final String KEY_TASK = "currentTaskKey";

	private static final Type SET_TYPE = new TypeToken<TreeSet<String>>()
	{
	}.getType();
	private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, List<String>>>()
	{
	}.getType();

	private final ConfigManager configManager;
	private final Gson gson;

	@Inject
	public PetHunterConfigManager(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
	}

	public Set<String> getObtained()
	{
		String json = configManager.getConfiguration(PetHunterConfig.GROUP, KEY_OBTAINED);
		if (json == null || json.isEmpty())
		{
			return new TreeSet<>();
		}
		try
		{
			Set<String> set = gson.fromJson(json, SET_TYPE);
			return set != null ? set : new TreeSet<>();
		}
		catch (Exception e)
		{
			log.warn("Pet Hunter: failed to parse obtained pets, resetting", e);
			return new TreeSet<>();
		}
	}

	public void setObtained(Set<String> keys)
	{
		configManager.setConfiguration(PetHunterConfig.GROUP, KEY_OBTAINED, gson.toJson(keys, SET_TYPE));
	}

	public Map<String, List<String>> getCustomTags()
	{
		String json = configManager.getConfiguration(PetHunterConfig.GROUP, KEY_TAGS);
		if (json == null || json.isEmpty())
		{
			return new LinkedHashMap<>();
		}
		try
		{
			Map<String, List<String>> map = gson.fromJson(json, MAP_TYPE);
			return map != null ? map : new LinkedHashMap<>();
		}
		catch (Exception e)
		{
			log.warn("Pet Hunter: failed to parse custom tags, resetting", e);
			return new LinkedHashMap<>();
		}
	}

	public void setCustomTags(Map<String, List<String>> tags)
	{
		configManager.setConfiguration(PetHunterConfig.GROUP, KEY_TAGS, gson.toJson(tags, MAP_TYPE));
	}

	public String getCurrentTaskKey()
	{
		return configManager.getConfiguration(PetHunterConfig.GROUP, KEY_TASK);
	}

	public void setCurrentTaskKey(String key)
	{
		if (key == null || key.isEmpty())
		{
			configManager.unsetConfiguration(PetHunterConfig.GROUP, KEY_TASK);
		}
		else
		{
			configManager.setConfiguration(PetHunterConfig.GROUP, KEY_TASK, key);
		}
	}
}

package com.example.neoblueprints.config;

import com.example.neoblueprints.NeoBlueprintsMod;
import com.example.neoblueprints.item.BlueprintRarity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JSON-backed blueprint configuration.
 *
 * <p>Stored at {@code <gameDir>/config/blueprint-crafting.json}. On first launch
 * a default file is written. Reload at runtime via {@code /blueprint reload}.
 *
 * <p>JSON shape:
 * <pre>
 * {
 *   "_comment": "...",
 *   "blueprints": [
 *     {
 *       "id": "neoblueprints:iron_tools",
 *       "name": "Iron Tools Blueprint",
 *       "rarity": "rare",
 *       "recipes": ["minecraft:iron_pickaxe", ...]
 *     }
 *   ]
 * }
 * </pre>
 *
 * <p>The union of every blueprint's recipes implicitly forms the locked-recipe
 * set; there is no separate "lockedRecipes" list to keep in sync.
 */
public final class BlueprintConfig {

    private static final String CONFIG_FILE = "neoblueprints.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Object LOCK = new Object();
    private static volatile Map<ResourceLocation, BlueprintDefinition> blueprints = Collections.emptyMap();
    private static volatile Map<ResourceLocation, ResourceLocation> recipeToBlueprint = Collections.emptyMap();
    private static volatile Set<ResourceLocation> lockedRecipes = Collections.emptySet();
    private static volatile boolean singleUse = true;
    private static volatile int tempUnlockSeconds = 60;

    private BlueprintConfig() {}

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE);
    }

    /** Loads the config file, creating it from defaults on first run. */
    public static void load() {
        Path path = configPath();
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, defaultJson(), StandardCharsets.UTF_8);
                NeoBlueprintsMod.LOGGER.info("Wrote default blueprint config to {}", path);
            }
            String content = Files.readString(path, StandardCharsets.UTF_8);
            parseAndStore(content);
            NeoBlueprintsMod.LOGGER.info("Loaded {} blueprint(s) from {}", blueprints.size(), path);
        } catch (Exception e) {
            NeoBlueprintsMod.LOGGER.error("Failed to load blueprint config at {}: {}", path, e.toString());
            // Fall back to defaults in memory so the mod stays usable.
            try { parseAndStore(defaultJson()); } catch (Exception ignored) {}
        }
    }

    /** Reload from disk and return the blueprint count. Throws if the file is malformed. */
    public static int reload() throws IOException {
        Path path = configPath();
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.writeString(path, defaultJson(), StandardCharsets.UTF_8);
        }
        parseAndStore(Files.readString(path, StandardCharsets.UTF_8));
        NeoBlueprintsMod.LOGGER.info("Reloaded {} blueprint(s) from {}", blueprints.size(), path);
        return blueprints.size();
    }

    private static void parseAndStore(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray arr = root.has("blueprints") ? root.getAsJsonArray("blueprints") : new JsonArray();

        Map<ResourceLocation, BlueprintDefinition> defs = new LinkedHashMap<>();
        Map<ResourceLocation, ResourceLocation> reverse = new HashMap<>();
        Set<ResourceLocation> locked = new HashSet<>();

        boolean newSingleUse = root.has("singleUseBP") ? root.get("singleUseBP").getAsBoolean() : true;
        int newTempUnlockSeconds = root.has("tempUnlockSeconds") ? root.get("tempUnlockSeconds").getAsInt() : 60;
        if (newTempUnlockSeconds < 5) newTempUnlockSeconds = 5;

        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();

            String idStr = obj.has("id") ? obj.get("id").getAsString() : null;
            ResourceLocation id = parseId(idStr);
            if (id == null) {
                NeoBlueprintsMod.LOGGER.warn("Skipping blueprint entry with invalid id: {}", idStr);
                continue;
            }
            String name = obj.has("name") ? obj.get("name").getAsString() : id.getPath();
            BlueprintRarity rarity = obj.has("rarity")
                    ? BlueprintRarity.fromString(obj.get("rarity").getAsString())
                    : BlueprintRarity.COMMON;

            List<ResourceLocation> recipes = new ArrayList<>();
            if (obj.has("recipes") && obj.get("recipes").isJsonArray()) {
                for (JsonElement re : obj.getAsJsonArray("recipes")) {
                    if (!re.isJsonPrimitive()) continue;
                    ResourceLocation rl = ResourceLocation.tryParse(re.getAsString());
                    if (rl != null) {
                        recipes.add(rl);
                        reverse.putIfAbsent(rl, id);
                        locked.add(rl);
                    } else {
                        NeoBlueprintsMod.LOGGER.warn("Invalid recipe id '{}' in blueprint {}", re.getAsString(), id);
                    }
                }
            }
            defs.put(id, new BlueprintDefinition(id, name, rarity, Collections.unmodifiableList(recipes)));
        }

        synchronized (LOCK) {
            blueprints = Collections.unmodifiableMap(defs);
            recipeToBlueprint = Collections.unmodifiableMap(reverse);
            lockedRecipes = Collections.unmodifiableSet(locked);
            singleUse = newSingleUse;
            tempUnlockSeconds = newTempUnlockSeconds;
        }
    }

    /** Replace the loaded data wholesale (used by client-side sync from server). */
    public static void replaceClientCache(List<BlueprintDefinition> definitions, boolean newSingleUse, int newTempUnlockSeconds) {
        Map<ResourceLocation, BlueprintDefinition> defs = new LinkedHashMap<>();
        Map<ResourceLocation, ResourceLocation> reverse = new HashMap<>();
        Set<ResourceLocation> locked = new HashSet<>();
        for (BlueprintDefinition d : definitions) {
            defs.put(d.id(), d);
            for (ResourceLocation r : d.recipes()) {
                reverse.putIfAbsent(r, d.id());
                locked.add(r);
            }
        }
        synchronized (LOCK) {
            blueprints = Collections.unmodifiableMap(defs);
            recipeToBlueprint = Collections.unmodifiableMap(reverse);
            lockedRecipes = Collections.unmodifiableSet(locked);
            singleUse = newSingleUse;
            tempUnlockSeconds = newTempUnlockSeconds;
        }
    }

    /** Parse a possibly-unprefixed ID, defaulting namespace to {@code neoblueprints}. */
    public static ResourceLocation parseId(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        if (!trimmed.contains(":")) trimmed = NeoBlueprintsMod.MODID + ":" + trimmed;
        return ResourceLocation.tryParse(trimmed);
    }

    public static Set<ResourceLocation> getLockedRecipes() { return lockedRecipes; }
    public static boolean isLocked(ResourceLocation recipeId) { return lockedRecipes.contains(recipeId); }
    public static ResourceLocation getBlueprintFor(ResourceLocation recipeId) { return recipeToBlueprint.get(recipeId); }
    public static boolean isSingleUse() { return singleUse; }
    public static int getTempUnlockSeconds() { return tempUnlockSeconds; }

    public static List<ResourceLocation> getRecipesFor(ResourceLocation blueprintId) {
        BlueprintDefinition d = blueprints.get(blueprintId);
        return d == null ? List.of() : d.recipes();
    }

    public static BlueprintDefinition getDefinition(ResourceLocation blueprintId) {
        return blueprints.get(blueprintId);
    }

    public static BlueprintRarity getRarityFor(ResourceLocation blueprintId) {
        BlueprintDefinition d = blueprints.get(blueprintId);
        return d == null ? BlueprintRarity.COMMON : d.rarity();
    }

    public static String getDisplayName(ResourceLocation blueprintId) {
        BlueprintDefinition d = blueprints.get(blueprintId);
        return d == null ? blueprintId.getPath() : d.name();
    }

    public static Set<ResourceLocation> knownBlueprintIds() { return blueprints.keySet(); }
    public static List<BlueprintDefinition> all() { return new ArrayList<>(blueprints.values()); }

    private static String defaultJson() {
        return """
                {
                  "_comment": [
                    "NeoBlueprints configuration.",
                    "",
                    "singleUseBP (default: true):",
                    "  true  — right-clicking a blueprint consumes it and permanently unlocks its recipes.",
                    "  false — right-clicking a blueprint does NOT consume it; instead it grants a temporary",
                    "          unlock lasting 'tempUnlockSeconds' seconds. The item stays in the player's",
                    "          inventory, allowing trading and lending between players.",
                    "",
                    "tempUnlockSeconds (default: 60):",
                    "  How long a temporary unlock lasts in seconds. Only used when singleUseBP is false.",
                    "  Minimum 5 seconds.",
                    "",
                    "Blueprint fields:",
                    "  id      - unique blueprint id ('neoblueprints:<name>'). Namespace optional; defaults to 'neoblueprints'.",
                    "  name    - human-readable display name.",
                    "  rarity  - one of: common | uncommon | rare | epic | legendary.",
                    "  recipes - list of recipe ids locked behind this blueprint.",
                    "",
                    "Any recipe id appearing in any blueprint's 'recipes' list is automatically locked until unlocked.",
                    "Reload this file at runtime with: /blueprint reload"
                  ],
                  "singleUseBP": true,
                  "tempUnlockSeconds": 60,
                  "blueprints": [
                    {
                      "id": "neoblueprints:iron_tools",
                      "name": "Iron Tools Blueprint",
                      "rarity": "uncommon",
                      "recipes": [
                        "minecraft:iron_pickaxe",
                        "minecraft:iron_sword",
                        "minecraft:iron_axe",
                        "minecraft:iron_shovel",
                        "minecraft:iron_hoe"
                      ]
                    },
                    {
                      "id": "neoblueprints:diamond_tools",
                      "name": "Diamond Tools Blueprint",
                      "rarity": "epic",
                      "recipes": [
                        "minecraft:diamond_pickaxe",
                        "minecraft:diamond_sword",
                        "minecraft:diamond_axe",
                        "minecraft:diamond_shovel",
                        "minecraft:diamond_hoe"
                      ]
                    },
                    {
                      "id": "neoblueprints:enchanting",
                      "name": "Enchanting Blueprint",
                      "rarity": "legendary",
                      "recipes": [
                        "minecraft:enchanting_table"
                      ]
                    }
                  ]
                }
                """;
    }
}

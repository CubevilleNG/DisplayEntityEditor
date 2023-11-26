package goldenshadow.displayentityeditor.inventories;

import goldenshadow.displayentityeditor.DisplayEntityEditor;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class InventoryManager {

    private static final HashMap<UUID, ItemStack[]> savedInventories = new HashMap<>();

    private final File inventoriesDir;

    public InventoryManager(File inventoriesDir) {
        this.inventoriesDir = inventoriesDir;
        loadInventories();
    }

    private void loadInventories() {
        File[] invFiles;
        try {
            invFiles = inventoriesDir.listFiles();
        } catch (NullPointerException e) {
            DisplayEntityEditor.getPlugin().getLogger().severe("Unable to load inventory files!");
            e.printStackTrace();
            return;
        }
        for(File invFile : invFiles) {
            YamlConfiguration invConfig = new YamlConfiguration();
            try {
                invConfig.load(invFile);
            } catch(IOException | InvalidConfigurationException e) {
                DisplayEntityEditor.getPlugin().getLogger().severe("Unable to load inventory file " + invFile.getName());
                e.printStackTrace();
                continue;
            }
            UUID uuid = null;
            try {
                uuid = UUID.fromString(invFile.getName().substring(0, invFile.getName().indexOf(".")));
            } catch(IllegalArgumentException e) {
                DisplayEntityEditor.getPlugin().getLogger().severe("Unable to load inventory file " + invFile.getName() + " as it is not a valid UUID!");
            }
            if(uuid == null) continue;
            ItemStack[] inv = new ItemStack[invConfig.getKeys(false).size()];
            for(String key : invConfig.getKeys(false)) {
                inv[Integer.parseInt(key)] = invConfig.getItemStack(key);
            }
            savedInventories.put(uuid, inv);
            DisplayEntityEditor.needInvReturned.add(uuid);
        }
    }

    public ItemStack[] getInventory(UUID uuid) {
        return savedInventories.get(uuid);
    }

    public void addInventory(UUID uuid, ItemStack[] itemStacks) {
        savedInventories.put(uuid, itemStacks);
        saveInventoryFile(uuid);
    }

    public void removeInventory(UUID uuid) {
        savedInventories.remove(uuid);
        deleteInventoryFile(uuid);
    }

    private File getInventoryFile(UUID uuid) {
        File invFile = new File(inventoriesDir, uuid + ".yml");
        if(!invFile.exists()) {
            try {
                invFile.createNewFile();
                return invFile;
            } catch(IOException e) {
                DisplayEntityEditor.getPlugin().getLogger().severe("Unable to generate inventory file for " + uuid);
                e.printStackTrace();
            }
        }
        return invFile;
    }

    private void saveInventoryFile(UUID uuid) {
        File invFile = getInventoryFile(uuid);
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(invFile);
        ItemStack[] inv = savedInventories.get(uuid);
        for(int i = 0; i < inv.length; i++) {
            yamlConfiguration.set(String.valueOf(i), inv[i] == null ? new ItemStack(Material.AIR) : inv[i]);
        }
        try {
            yamlConfiguration.save(invFile);
        } catch(IOException e) {
            DisplayEntityEditor.getPlugin().getLogger().severe("Unable to save inventory file for " + uuid);
            e.printStackTrace();
        }
    }

    private void deleteInventoryFile(UUID uuid) {
        File invFile = new File(inventoriesDir, uuid + ".yml");
        if(invFile.exists()) {
            invFile.delete();
        }
    }
}

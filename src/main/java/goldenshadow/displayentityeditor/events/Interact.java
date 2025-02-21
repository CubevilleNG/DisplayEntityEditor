package goldenshadow.displayentityeditor.events;

import goldenshadow.displayentityeditor.DisplayEntityEditor;
import goldenshadow.displayentityeditor.Utilities;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.UUID;

public class Interact implements Listener {

    private static final DecimalFormat df = new DecimalFormat("#.####");

    public Interact() {
        df.setRoundingMode(RoundingMode.CEILING);
    }

    /**
     * Used to listener for when a player uses a tool
     * @param event The event
     */
    @EventHandler
    public void interact(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.HAND) {
            Player player = event.getPlayer();
            if (Utilities.hasDataKey(player.getInventory().getItemInMainHand())) {
                event.setCancelled(true);
                if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    cycleInventory(player);
                    event.setCancelled(true);
                    return;
                }
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
                    ItemStack item = player.getInventory().getItemInMainHand();
                    event.setCancelled(true);
                    String toolValue = Utilities.getToolValue(item);
                    if (toolValue != null) {
                        if (toolValue.equals("InventorySpawnItem")) {
                            spawnDisplayEntity(player.getLocation(), EntityType.ITEM_DISPLAY, player.getUniqueId());
                            player.sendMessage(Utilities.getInfoMessageFormat(DisplayEntityEditor.messageManager.getString("item_display_spawned")));
                            return;
                        }
                        if (toolValue.equals("InventorySpawnBlock")) {
                            spawnDisplayEntity(player.getLocation(), EntityType.BLOCK_DISPLAY, player.getUniqueId());
                            player.sendMessage(Utilities.getInfoMessageFormat(DisplayEntityEditor.messageManager.getString("block_display_spawned")));
                            return;
                        }
                        if (toolValue.equals("InventorySpawnText")) {
                            spawnDisplayEntity(player.getLocation(), EntityType.TEXT_DISPLAY, player.getUniqueId());
                            player.sendMessage(Utilities.getInfoMessageFormat(DisplayEntityEditor.messageManager.getString("text_display_spawned")));
                            return;
                        }
                        if (toolValue.equals("InventoryUnlock")) {
                            Display display;
                            if(DisplayEntityEditor.currentSelectionMap.containsKey(player.getUniqueId())) {
                                display = DisplayEntityEditor.currentSelectionMap.get(player.getUniqueId());
                            } else {
                                player.sendMessage(Utilities.getErrorMessageFormat(DisplayEntityEditor.messageManager.getString("nothing_selected")));
                                return;
                            }
                            if(!display.getScoreboardTags().contains("dee:locked")) {
                                player.sendMessage(Utilities.getErrorMessageFormat(DisplayEntityEditor.messageManager.getString("selected_unlock_fail")));
                                return;
                            }
                            /*if (display == null) {
                                player.sendMessage(Utilities.getErrorMessageFormat(DisplayEntityEditor.messageManager.getString("unlock_fail")));
                                return;
                            }*/
                            display.getScoreboardTags().remove("dee:locked");
                            highlightEntity(display);
                            player.sendMessage(Utilities.getInfoMessageFormat(DisplayEntityEditor.messageManager.getString("unlock_success")));
                            return;
                        }
                        if (toolValue.equals("InventoryToolPrecision")) {
                            double d = Utilities.getToolPrecision(player);
                            if (player.isSneaking()) {
                                if (d > 1) {
                                    d = Math.max(0.1, d - 1);
                                } else {
                                    d = Math.max(0.1, d - 0.1);
                                }
                            } else {
                                if (d < 1) {
                                    d = Math.min(10, d + 0.1);
                                } else {
                                    d = Math.min(10, d + 1);
                                }
                            }
                            d = (double) Math.round(d * 1000) / 1000;
                            player.getPersistentDataContainer().set(DisplayEntityEditor.toolPrecisionKey, PersistentDataType.DOUBLE, d);
                            updateItems(player);
                            sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("tool_precision").formatted(df.format(d)));
                            return;
                        }
                        if (toolValue.equals("InventoryDeselect")) {
                            if (!DisplayEntityEditor.currentSelectionMap.containsKey(player.getUniqueId())) {
                                player.sendMessage(Utilities.getErrorMessageFormat(DisplayEntityEditor.messageManager.getString("deselect_fail")));
                                return;
                            }

                            DisplayEntityEditor.currentSelectionMap.remove(player.getUniqueId());
                            player.sendMessage(Utilities.getInfoMessageFormat(DisplayEntityEditor.messageManager.getString("deselect_success")));
                            return;
                        }

                        Display display;
                        if(toolValue.equals("InventorySelect")) {
                            display = Utilities.getNearestDisplayEntity(player.getLocation(), player.isSneaking());
                        } else if(DisplayEntityEditor.currentSelectionMap.containsKey(player.getUniqueId())) {
                            display = DisplayEntityEditor.currentSelectionMap.get(player.getUniqueId());
                        } else {
                            player.sendMessage(Utilities.getErrorMessageFormat(DisplayEntityEditor.messageManager.getString("nothing_selected")));
                            return;
                        }
                        /*if (display == null) {
                            player.sendMessage(Utilities.getErrorMessageFormat(DisplayEntityEditor.messageManager.getString("generic_fail")));
                            return;
                        }*/
                        switch (toolValue) {
                            case "InventorySelect" -> {

                                if (DisplayEntityEditor.currentSelectionMap.containsValue(display)) {
                                    if (DisplayEntityEditor.currentSelectionMap.containsKey(player.getUniqueId()) &&
                                            DisplayEntityEditor.currentSelectionMap.get(player.getUniqueId()).equals(display)) {
                                        player.sendMessage(Utilities.getErrorMessageFormat(DisplayEntityEditor.messageManager.getString("select_fail_identical_selection")));
                                    } else {
                                        player.sendMessage(Utilities.getErrorMessageFormat(DisplayEntityEditor.messageManager.getString("select_fail_someone")));
                                    }
                                    return;
                                }

                                DisplayEntityEditor.currentSelectionMap.put(player.getUniqueId(), display);
                                player.sendMessage(Utilities.getInfoMessageFormat(DisplayEntityEditor.messageManager.getString("select_success")));
                                if (display instanceof TextDisplay) {
                                    String defText = ((TextDisplay) display).getText();
                                    String noColorText = ChatColor.stripColor(defText);
                                    ((TextDisplay) display).setText(ChatColor.BLACK + noColorText);
                                    DisplayEntityEditor.getPlugin().getServer().getScheduler().runTaskLater(DisplayEntityEditor.getPlugin(), () -> {
                                        ((TextDisplay) display).setText(ChatColor.WHITE + noColorText);
                                    }, 10);
                                    DisplayEntityEditor.getPlugin().getServer().getScheduler().runTaskLater(DisplayEntityEditor.getPlugin(), () -> {
                                        ((TextDisplay) display).setText(ChatColor.BLACK + noColorText);
                                    }, 20);
                                    DisplayEntityEditor.getPlugin().getServer().getScheduler().runTaskLater(DisplayEntityEditor.getPlugin(), () -> {
                                        ((TextDisplay) display).setText(ChatColor.WHITE + noColorText);
                                    }, 30);
                                    DisplayEntityEditor.getPlugin().getServer().getScheduler().runTaskLater(DisplayEntityEditor.getPlugin(), () -> {
                                        ((TextDisplay) display).setText(defText);
                                    }, 40);
                                } else {
                                    boolean defGlow = display.isGlowing();
                                    display.setGlowing(!defGlow);
                                    DisplayEntityEditor.getPlugin().getServer().getScheduler().runTaskLater(DisplayEntityEditor.getPlugin(), () -> {
                                        display.setGlowing(defGlow);
                                    }, 40);
                                }
                                return;
                            }
                            case "InventoryGUI" -> {

                                /*if (DisplayEntityEditor.currentEditMap.containsValue(display)) {
                                    player.sendMessage(Utilities.getErrorMessageFormat(DisplayEntityEditor.messageManager.getString("gui_open_fail")));
                                    return;
                                }
                                DisplayEntityEditor.currentEditMap.put(player.getUniqueId(), display);*/

                                if (display instanceof ItemDisplay) {
                                    player.openInventory(DisplayEntityEditor.inventoryFactory.createItemDisplayGUI((ItemDisplay) display));
                                } else if (display instanceof BlockDisplay) {
                                    player.openInventory(DisplayEntityEditor.inventoryFactory.createBlockDisplayGUI((BlockDisplay) display));
                                } else {
                                    player.openInventory(DisplayEntityEditor.inventoryFactory.createTextDisplayGUI((TextDisplay) display));
                                }
                            }
                            case "InventoryRotateYaw" -> {
                                if (player.isSneaking()) {
                                    display.setRotation((float) (display.getLocation().getYaw()-1 * Utilities.getToolPrecision(player)), display.getLocation().getPitch());
                                    sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("yaw").formatted(df.format(display.getLocation().getYaw())));
                                    return;
                                }
                                display.setRotation((float) (display.getLocation().getYaw()+1  * Utilities.getToolPrecision(player)), display.getLocation().getPitch());
                                sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("yaw").formatted(df.format(display.getLocation().getYaw())));
                            }
                            case "InventoryRotatePitch" -> {
                                if (player.isSneaking()) {
                                    display.setRotation(display.getLocation().getYaw(), (float) (display.getLocation().getPitch()-1  * Utilities.getToolPrecision(player)));
                                    sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("pitch").formatted(df.format(display.getLocation().getPitch())));
                                    return;
                                }
                                display.setRotation(display.getLocation().getYaw(), (float) (display.getLocation().getPitch()+1 * Utilities.getToolPrecision(player)));
                                sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("pitch").formatted(df.format(display.getLocation().getPitch())));
                            }
                            case "InventoryMoveX" -> {
                                if (player.isSneaking()) {
                                    display.teleport(display.getLocation().add(-0.1 * Utilities.getToolPrecision(player),0,0));
                                    sendActionbarMessage(player, "X: " + df.format(display.getLocation().getX()));
                                    sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("move_x").formatted(df.format(display.getLocation().getX())));
                                    return;
                                }
                                display.teleport(display.getLocation().add(0.1 * Utilities.getToolPrecision(player),0,0));
                                sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("move_x").formatted(df.format(display.getLocation().getX())));
                            }
                            case "InventoryMoveY" -> {
                                if (player.isSneaking()) {
                                    display.teleport(display.getLocation().add(0,-0.1 * Utilities.getToolPrecision(player),0));
                                    sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("move_y").formatted(df.format(display.getLocation().getY())));
                                    return;
                                }
                                display.teleport(display.getLocation().add(0,0.1 * Utilities.getToolPrecision(player),0));
                                sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("move_y").formatted(df.format(display.getLocation().getY())));
                            }
                            case "InventoryMoveZ" -> {
                                if (player.isSneaking()) {
                                    display.teleport(display.getLocation().add(0,0,-0.1 * Utilities.getToolPrecision(player)));
                                    sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("move_z").formatted(df.format(display.getLocation().getZ())));
                                    return;
                                }
                                display.teleport(display.getLocation().add(0,0,0.1 * Utilities.getToolPrecision(player)));
                                sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("move_z").formatted(df.format(display.getLocation().getZ())));
                            }
                            case "InventoryHighlight" -> highlightEntity(display);
                            case "InventoryCenterPivot" -> {
                                Transformation t = display.getTransformation();
                                if (display instanceof BlockDisplay) {
                                    t.getTranslation().set(-1 * (t.getScale().x() / 2), -1 * (t.getScale().y() / 2), -1 * (t.getScale().z() / 2));
                                } else {
                                    t.getTranslation().set(0,0,0);
                                }
                                display.setTransformation(t);
                                sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("center_pivot"));
                            }
                            case "InventoryTX" -> {
                                Transformation t = display.getTransformation();
                                if (player.isSneaking()) {
                                    t.getTranslation().add((float) (-0.1f * Utilities.getToolPrecision(player)),0,0);
                                } else {
                                    t.getTranslation().add((float) (0.1f * Utilities.getToolPrecision(player)),0,0);
                                }
                                sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("translation_x").formatted(df.format(t.getTranslation().x())));
                                display.setTransformation(t);
                            }
                            case "InventoryTY" -> {
                                Transformation t = display.getTransformation();
                                if (player.isSneaking()) {
                                    t.getTranslation().add(0,(float) (-0.1f * Utilities.getToolPrecision(player)),0);
                                } else {
                                    t.getTranslation().add(0,(float) (0.1f * Utilities.getToolPrecision(player)),0);
                                }
                                sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("translation_y").formatted(df.format(t.getTranslation().y())));
                                display.setTransformation(t);
                            }
                            case "InventoryTZ" -> {
                                Transformation t = display.getTransformation();
                                if (player.isSneaking()) {
                                    t.getTranslation().add(0,0,(float) (-0.1f * Utilities.getToolPrecision(player)));
                                } else {
                                    t.getTranslation().add(0,0,(float) (0.1f * Utilities.getToolPrecision(player)));
                                }
                                sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("translation_z").formatted(df.format(t.getTranslation().z())));
                                display.setTransformation(t);
                            }
                            case "InventorySX" -> {
                                Transformation t = display.getTransformation();
                                if (player.isSneaking()) {
                                    t.getScale().add((float) (-0.1f * Utilities.getToolPrecision(player)),0,0);
                                } else {
                                    t.getScale().add((float) (0.1f * Utilities.getToolPrecision(player)),0,0);
                                }
                                sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("scale_x").formatted(df.format(t.getScale().x())));
                                display.setTransformation(t);
                            }
                            case "InventorySY" -> {
                                Transformation t = display.getTransformation();
                                if (player.isSneaking()) {
                                    t.getScale().add(0,(float) (-0.1f * Utilities.getToolPrecision(player)),0);
                                } else {
                                    t.getScale().add(0,(float) (0.1f * Utilities.getToolPrecision(player)),0);
                                }
                                sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("scale_y").formatted(df.format(t.getScale().y())));
                                display.setTransformation(t);
                            }
                            case "InventorySZ" -> {
                                Transformation t = display.getTransformation();
                                if (player.isSneaking()) {
                                    t.getScale().add(0,0,(float) (-0.1f * Utilities.getToolPrecision(player)));
                                } else {
                                    t.getScale().add(0,0,(float) (0.1f * Utilities.getToolPrecision(player)));
                                }
                                sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("scale_z").formatted(df.format(t.getScale().z())));
                                display.setTransformation(t);
                            }
                            case "InventoryLRX" -> {
                                Transformation t = display.getTransformation();
                                boolean b = Utilities.getData(display, "GUILRNormalize");
                                if (player.isSneaking()) {
                                    t.getLeftRotation().add((float) (-0.1f * Utilities.getToolPrecision(player)),0,0,0);
                                } else {
                                    t.getLeftRotation().add((float) (0.1f * Utilities.getToolPrecision(player)),0,0,0);
                                }
                                if (b) t.getLeftRotation().normalize();
                                sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("left_rot_x").formatted((b ? DisplayEntityEditor.messageManager.getString("normalized") : "") ,df.format(t.getLeftRotation().x())));
                                display.setTransformation(t);
                            }
                            case "InventoryLRY" -> {
                                Transformation t = display.getTransformation();
                                boolean b = Utilities.getData(display, "GUILRNormalize");
                                if (player.isSneaking()) {
                                    t.getLeftRotation().add(0,(float) (-0.1f * Utilities.getToolPrecision(player)),0,0);
                                } else {
                                    t.getLeftRotation().add(0,(float) (0.1f * Utilities.getToolPrecision(player)),0,0);
                                }
                                if (b) t.getLeftRotation().normalize();
                                sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("left_rot_y").formatted((b ? DisplayEntityEditor.messageManager.getString("normalized") : "") ,df.format(t.getLeftRotation().y())));
                                display.setTransformation(t);
                            }
                            case "InventoryLRZ" -> {
                                Transformation t = display.getTransformation();
                                boolean b = Utilities.getData(display, "GUILRNormalize");
                                if (player.isSneaking()) {
                                    t.getLeftRotation().add(0,0,(float) (-0.1f * Utilities.getToolPrecision(player)),0);
                                } else {
                                    t.getLeftRotation().add(0,0,(float) (0.1f * Utilities.getToolPrecision(player)),0);
                                }
                                if (b) t.getLeftRotation().normalize();
                                sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("left_rot_z").formatted((b ? DisplayEntityEditor.messageManager.getString("normalized") : "") ,df.format(t.getLeftRotation().z())));
                                display.setTransformation(t);
                            }
                            case "InventoryRRX" -> {
                                Transformation t = display.getTransformation();
                                boolean b = Utilities.getData(display, "GUIRRNormalize");
                                if (player.isSneaking()) {
                                    t.getRightRotation().add((float) (-0.1f * Utilities.getToolPrecision(player)),0,0,0);
                                } else {
                                    t.getRightRotation().add((float) (0.1f * Utilities.getToolPrecision(player)),0,0,0);
                                }
                                if (b) t.getRightRotation().normalize();
                                sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("right_rot_x").formatted((b ? DisplayEntityEditor.messageManager.getString("normalized") : "") ,df.format(t.getRightRotation().x())));
                                display.setTransformation(t);
                            }
                            case "InventoryRRY" -> {
                                Transformation t = display.getTransformation();
                                boolean b = Utilities.getData(display, "GUIRRNormalize");
                                if (player.isSneaking()) {
                                    t.getRightRotation().add(0,(float) (-0.1f * Utilities.getToolPrecision(player)),0,0);
                                } else {
                                    t.getRightRotation().add(0,(float) (0.1f * Utilities.getToolPrecision(player)),0,0);
                                }
                                if (b) t.getRightRotation().normalize();
                                sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("right_rot_y").formatted((b ? DisplayEntityEditor.messageManager.getString("normalized") : "") ,df.format(t.getRightRotation().y())));
                                display.setTransformation(t);
                            }
                            case "InventoryRRZ" -> {
                                Transformation t = display.getTransformation();
                                boolean b = Utilities.getData(display, "GUIRRNormalize");
                                if (player.isSneaking()) {
                                    t.getRightRotation().add(0,0,(float) (-0.1f * Utilities.getToolPrecision(player)),0);
                                } else {
                                    t.getRightRotation().add(0,0,(float) (0.1f * Utilities.getToolPrecision(player)),0);
                                }
                                if (b) t.getRightRotation().normalize();
                                sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("right_rot_z").formatted((b ? DisplayEntityEditor.messageManager.getString("normalized") : "") ,df.format(t.getRightRotation().z())));
                                display.setTransformation(t);
                            }
                            case "InventoryCenterBlock" -> {

                                if (display instanceof BlockDisplay) {
                                    Transformation t = display.getTransformation();
                                    t.getTranslation().set(-1 * (t.getScale().x() / 2), -1 * (t.getScale().y() / 2), -1 * (t.getScale().z() / 2));
                                    display.setTransformation(t);
                                }

                                Location loc = display.getLocation();
                                loc.setX((int) loc.getX() + (((loc.getX()) < 0 ? -1 : 1) * 0.5));
                                loc.setZ((int) loc.getZ() + (((loc.getZ()) < 0 ? -1 : 1) * 0.5));
                                if (!player.isSneaking()) {
                                    loc.setY((int) loc.getY() + (((loc.getY()) < 0 ? -1 : 1) * 0.5));
                                }
                                display.teleport(loc);
                                sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("center_block").formatted(loc.getX(), loc.getY(), loc.getZ()));
                            }
                            case "InventoryClone" -> {
                                Display clone = (Display) display.getWorld().spawnEntity(display.getLocation(), display.getType(), false);
                                DisplayEntityEditor.currentSelectionMap.put(player.getUniqueId(), clone);
                                cloneEntity(clone, display);
                                sendActionbarMessage(player, DisplayEntityEditor.messageManager.getString("clone"));
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * Used to cycle the players inventory to make it easy to switch tools
     * @param p The player whose inventory should get cycled through
     */
    public static void cycleInventory(Player p) {
        ItemStack[] array = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            array[i] = p.getInventory().getItem(i);
        }
        ItemStack[] shiftedArray = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            int newIndex = (i + 9) % 36;
            shiftedArray[newIndex] = array[i];
        }
        for (int i = 0; i < 36; i++) {
            p.getInventory().setItem(i, shiftedArray[i]);
        }
    }



    /**
     * Used to spawn a new display entity
     * @param location The location of where it should be spawned
     * @param type The specific type of display entity
     */
    private static void spawnDisplayEntity(Location location, EntityType type, UUID player) {
        assert location.getWorld() != null;
        location.setYaw(0);
        location.setPitch(0);
        if (type != EntityType.BLOCK_DISPLAY) {
            location.setX((int) location.getX() + (((location.getX()) < 0 ? -1 : 1) * 0.5));
            location.setZ((int) location.getZ() + (((location.getZ()) < 0 ? -1 : 1) * 0.5));
        }
        if (location.getY() < 0) location.setY(location.getY() + 0.0001);
        Display d = (Display) location.getWorld().spawnEntity(location, type, false);
        DisplayEntityEditor.currentSelectionMap.put(player, d);
        d.setVisualFire(true);

        if (d instanceof ItemDisplay) {
            ((ItemDisplay) d).setItemStack(new ItemStack(Material.DIAMOND));
        }
        if (d instanceof BlockDisplay) {
            ((BlockDisplay) d).setBlock(Bukkit.createBlockData(Material.GRASS_BLOCK));
        }
        if (d instanceof TextDisplay) {
            ((TextDisplay) d).setText("YOUR TEXT HERE");
            d.setBillboard(Display.Billboard.CENTER);
        }
    }

    /**
     * Used to highlight a specific display entity by making it glow and showing particles at its pivot point
     * @param display The entity that should be highlighted
     */
    private static void highlightEntity(Display display) {
        display.setGlowing(true);
        Bukkit.getScheduler().scheduleSyncDelayedTask(DisplayEntityEditor.getPlugin(), () -> display.setGlowing(false), 20L);
        display.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, display.getLocation(), 50,0.2,0.2,0.2,0);
    }

    private static void sendActionbarMessage(Player p, String message) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(net.md_5.bungee.api.ChatColor.DARK_AQUA + message));
    }


    /**
     * Used to clone a display entity
     * @param clone The clone
     * @param template The template
     */
    @SuppressWarnings("deprecation")
    private static void cloneEntity(Display clone, Display template) {
        clone.setBrightness(template.getBrightness());
        clone.setBillboard(template.getBillboard());
        clone.setCustomName(template.getCustomName());
        clone.setGlowColorOverride(template.getGlowColorOverride());
        clone.setGlowing(template.isGlowing());
        clone.setCustomNameVisible(template.isCustomNameVisible());
        clone.setShadowStrength(template.getShadowStrength());
        clone.setShadowRadius(template.getShadowRadius());
        clone.setDisplayHeight(template.getDisplayHeight());
        clone.setDisplayWidth(template.getDisplayWidth());
        clone.setViewRange(template.getViewRange());
        clone.setTransformation(template.getTransformation());
        clone.getLocation().setPitch(template.getLocation().getPitch());
        clone.getLocation().setYaw(template.getLocation().getYaw());
        if (clone instanceof ItemDisplay itemDisplay) {
            itemDisplay.setItemStack(((ItemDisplay) template).getItemStack());
            itemDisplay.setItemDisplayTransform(((ItemDisplay) template).getItemDisplayTransform());
        }
        if (clone instanceof BlockDisplay blockDisplay) {
            blockDisplay.setBlock(((BlockDisplay) template).getBlock());
        }
        if (clone instanceof TextDisplay textDisplay) {
            TextDisplay templateText = (TextDisplay) template;
            textDisplay.setText(templateText.getText());
            textDisplay.setBackgroundColor(templateText.getBackgroundColor());
            textDisplay.setShadowed(templateText.isShadowed());
            textDisplay.setAlignment(templateText.getAlignment());
            textDisplay.setTextOpacity(templateText.getTextOpacity());
            textDisplay.setSeeThrough(templateText.isSeeThrough());
            textDisplay.setDefaultBackground(templateText.isDefaultBackground());
            textDisplay.setLineWidth(templateText.getLineWidth());
        }
    }

    private static void updateItems(Player p) {
        for (int i = 0; i < p.getInventory().getContents().length; i++) {
            ItemStack it = p.getInventory().getContents()[i];
            if (it != null) {
                if (Utilities.hasDataKey(it)) {
                    String s = Utilities.getToolValue(it);
                    if (s != null) {
                        switch (s) {
                            case "InventoryRotateYaw" -> p.getInventory().setItem(i, DisplayEntityEditor.inventoryFactory.getInventoryItems().rotateYaw(p));
                            case "InventoryRotatePitch" -> p.getInventory().setItem(i, DisplayEntityEditor.inventoryFactory.getInventoryItems().rotatePitch(p));
                            case "InventoryMoveX" -> p.getInventory().setItem(i, DisplayEntityEditor.inventoryFactory.getInventoryItems().moveX(p));
                            case "InventoryMoveY" -> p.getInventory().setItem(i, DisplayEntityEditor.inventoryFactory.getInventoryItems().moveY(p));
                            case "InventoryMoveZ" -> p.getInventory().setItem(i, DisplayEntityEditor.inventoryFactory.getInventoryItems().moveZ(p));
                            case "InventoryLRX" -> p.getInventory().setItem(i, DisplayEntityEditor.inventoryFactory.getInventoryItems().leftRotationX(p));
                            case "InventoryLRY" -> p.getInventory().setItem(i, DisplayEntityEditor.inventoryFactory.getInventoryItems().leftRotationY(p));
                            case "InventoryLRZ" -> p.getInventory().setItem(i, DisplayEntityEditor.inventoryFactory.getInventoryItems().leftRotationZ(p));
                            case "InventoryRRX" -> p.getInventory().setItem(i, DisplayEntityEditor.inventoryFactory.getInventoryItems().rightRotationX(p));
                            case "InventoryRRY" -> p.getInventory().setItem(i, DisplayEntityEditor.inventoryFactory.getInventoryItems().rightRotationY(p));
                            case "InventoryRRZ" -> p.getInventory().setItem(i, DisplayEntityEditor.inventoryFactory.getInventoryItems().rightRotationZ(p));
                            case "InventoryTX" -> p.getInventory().setItem(i, DisplayEntityEditor.inventoryFactory.getInventoryItems().translationX(p));
                            case "InventoryTY" -> p.getInventory().setItem(i, DisplayEntityEditor.inventoryFactory.getInventoryItems().translationY(p));
                            case "InventoryTZ" -> p.getInventory().setItem(i, DisplayEntityEditor.inventoryFactory.getInventoryItems().translationZ(p));
                            case "InventorySX" -> p.getInventory().setItem(i, DisplayEntityEditor.inventoryFactory.getInventoryItems().scaleX(p));
                            case "InventorySY" -> p.getInventory().setItem(i, DisplayEntityEditor.inventoryFactory.getInventoryItems().scaleY(p));
                            case "InventorySZ" -> p.getInventory().setItem(i, DisplayEntityEditor.inventoryFactory.getInventoryItems().scaleZ(p));
                        }
                    }
                }
            }
        }
    }

}

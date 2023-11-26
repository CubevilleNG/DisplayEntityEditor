package goldenshadow.displayentityeditor.events;

import goldenshadow.displayentityeditor.DisplayEntityEditor;
import goldenshadow.displayentityeditor.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerJoin implements Listener {

    /**
     * Used to listen for when a player joins
     * @param event The event
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) {
            if (DisplayEntityEditor.getPlugin().getConfig().getBoolean("send-update-message-on-join")) {
                DisplayEntityEditor.
                getVersion(v -> {
                    if (!DisplayEntityEditor.getPlugin().getDescription().getVersion().equals(v)) {
                        player.sendMessage(Utilities.getErrorMessageFormat(DisplayEntityEditor.messageManager.getString("version_check_fail")));
                        player.sendMessage(ChatColor.GRAY + DisplayEntityEditor.messageManager.getString("version_check_disable_hint"));
                    }
                });
            }
        }

        if(DisplayEntityEditor.needInvReturned.contains(player.getUniqueId())) {
            if (DisplayEntityEditor.inventoryManager.getInventory(player.getUniqueId()) == null) throw new RuntimeException("Return inventory didn't exist!");
            player.getInventory().clear();
            ItemStack[] saved = DisplayEntityEditor.inventoryManager.getInventory(player.getUniqueId());
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                System.out.println("setting slot " + i + " with item " + saved[i].getType());
                player.getInventory().setItem(i, saved[i]);
            }
            DisplayEntityEditor.needInvReturned.remove(player.getUniqueId());
            DisplayEntityEditor.inventoryManager.removeInventory(player.getUniqueId());
        }
    }
}

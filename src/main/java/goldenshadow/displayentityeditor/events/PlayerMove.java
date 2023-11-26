package goldenshadow.displayentityeditor.events;

import goldenshadow.displayentityeditor.DisplayEntityEditor;
import goldenshadow.displayentityeditor.Utilities;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMove implements Listener {

    /**
     * Used to listen for when a player moves
     * @param event The event
     */
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if(!DisplayEntityEditor.currentSelectionMap.containsKey(player.getUniqueId())) return;
        if(DisplayEntityEditor.currentSelectionMap.get(player.getUniqueId()).getLocation().distance(player.getLocation()) > 250) {
            DisplayEntityEditor.currentSelectionMap.remove(event.getPlayer().getUniqueId());
            player.sendMessage(Utilities.getInfoMessageFormat(DisplayEntityEditor.messageManager.getString("deselect_success")));
        }
    }
}

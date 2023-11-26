package goldenshadow.displayentityeditor.events;

import goldenshadow.displayentityeditor.DisplayEntityEditor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class PlayerQuit implements Listener {

    /**
     * Used to listen for when a player leaves
     * @param event The event
     */
    @EventHandler
    public void onLeave(PlayerLoginEvent event) {
        DisplayEntityEditor.currentSelectionMap.remove(event.getPlayer().getUniqueId());
        //DisplayEntityEditor.currentEditMap.remove(event.getPlayer().getUniqueId());
    }
}

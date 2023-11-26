package goldenshadow.displayentityeditor.events;

import goldenshadow.displayentityeditor.DisplayEntityEditor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class PlayerChangedWorld implements Listener {

    /**
     * Used to listen for when a player changes world
     * @param event The event
     */
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        DisplayEntityEditor.currentSelectionMap.remove(event.getPlayer().getUniqueId());
    }
}

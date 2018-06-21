package club.without.dereku.levelrestrictions;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedList;

public final class LevelRestrictions extends JavaPlugin implements Listener {

    private static final String PREFIX = ChatColor.LIGHT_PURPLE.toString() + ChatColor.RED.toString();
    private static final String LEVEL_LINE = ChatColor.GOLD.toString() + "Level required: ";

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        final Player player = (Player) sender;
        final ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        if (itemInMainHand == null || itemInMainHand.getType() == Material.AIR) {
            return false;
        }

        final ItemMeta itemMeta = itemInMainHand.getItemMeta();
        final LinkedList<String> lore = new LinkedList<>();
        if (itemMeta.hasLore()) {
            lore.addAll(itemMeta.getLore());
        }

        boolean enabled = !lore.removeIf(line -> line.startsWith(LevelRestrictions.PREFIX) && line.contains(LevelRestrictions.LEVEL_LINE));
        int level = enabled ? (args.length > 0 ? Integer.parseInt(args[0]) : 10) : 0;
        if (enabled) {
            lore.add(LevelRestrictions.PREFIX + LevelRestrictions.LEVEL_LINE + String.valueOf(level));
        }

        itemMeta.setLore(lore);
        itemInMainHand.setItemMeta(itemMeta);

        sender.sendMessage(ChatColor.GOLD + "Level restriction for this item has been " +
                (enabled ? "enabled to level " + level : "disabled") + "."
        );
        return true;
    }
}

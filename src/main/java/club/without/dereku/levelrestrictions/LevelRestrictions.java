package club.without.dereku.levelrestrictions;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class LevelRestrictions extends JavaPlugin implements Listener {

    private static final String PREFIX = ChatColor.LIGHT_PURPLE.toString() + ChatColor.RED.toString();
    private static final String LEVEL_LINE = ChatColor.GOLD.toString() + "Level required: ";
    private static final List<InventoryType.SlotType> SLOT_TYPES = Arrays.asList(
         InventoryType.SlotType.ARMOR, InventoryType.SlotType.QUICKBAR
    );
    private static final List<InventoryAction> ITEM_MOVE_ACTIONS = Arrays.asList(
            InventoryAction.PLACE_ALL, InventoryAction.PLACE_ONE, InventoryAction.PLACE_SOME,
            InventoryAction.SWAP_WITH_CURSOR, InventoryAction.HOTBAR_SWAP
    );

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

        boolean enabled = !lore.removeIf(line ->
                line.startsWith(LevelRestrictions.PREFIX) && line.contains(LevelRestrictions.LEVEL_LINE)
        );

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

    @SuppressWarnings("WeakerAccess")
    public int getLevelRestriction(ItemStack item) {
        if (!item.hasItemMeta()) {
            return -1;
        }
        final ItemMeta itemMeta = item.getItemMeta();
        if (!itemMeta.hasLore()) {
            return -1;
        }

        final Optional<String> levelLine = itemMeta.getLore().stream().filter(line -> line.startsWith(PREFIX))
                .filter(line -> line.contains(LEVEL_LINE)).findAny();

        if (!levelLine.isPresent()) {
            return -1;
        }

        String line = levelLine.get();
        line = line.substring(line.lastIndexOf(':') + 2).trim();
        return Integer.parseInt(line);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        //Now lets make it compatible with all plugins that changes player's inventory
        if (!LevelRestrictions.SLOT_TYPES.contains(event.getSlotType())) {
            return;
        }

        //45 - shield slot
        if (event.getSlotType() == InventoryType.SlotType.QUICKBAR && event.getRawSlot() != 45) {
            return;
        }

        if (!LevelRestrictions.ITEM_MOVE_ACTIONS.contains(event.getAction())) {
            return;
        }

        final ItemStack cursorItem = event.getCursor();
        if (cursorItem == null || cursorItem.getType() == Material.AIR) {
            this.getLogger().warning(event.getAction().toString() + " has no item!");
            return;
        }

        if (!ItemType.isWearableItem(cursorItem)) {
            return;
        }


        final ItemType itemTypeByItem = ItemType.getItemTypeByItem(cursorItem);
        if (itemTypeByItem == null) {
            //Ignore that click.
            return;
        }

        final Player player = (Player) event.getWhoClicked();
        final int levelRestriction = this.getLevelRestriction(cursorItem);

        if (player.getInventory().getItem(itemTypeByItem.getSlot()) != null && event.getAction() != InventoryAction.SWAP_WITH_CURSOR) {
            //Ignore this too, cuz if player already has something in that slot, he can't equip it by swap
            return;
        }

        if (levelRestriction <= player.getLevel()) {
            return;
        }

        if (event.getRawSlot() != itemTypeByItem.getSlot()) {
            return;
        }

        event.setCancelled(true);
        player.sendMessage("You can't equip that item, required level: " + levelRestriction);
    }

    enum ItemType {
        HELMET(5, Material.LEATHER_HELMET, Material.IRON_HELMET, Material.CHAINMAIL_HELMET, Material.GOLD_HELMET, Material.DIAMOND_HELMET, Material.PUMPKIN),
        CHESTPLATE(6, Material.LEATHER_CHESTPLATE, Material.IRON_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.GOLD_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.ELYTRA),
        LEGGINS(7, Material.LEATHER_LEGGINGS, Material.IRON_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.GOLD_LEGGINGS, Material.DIAMOND_LEGGINGS),
        BOOTS(8, Material.LEATHER_BOOTS, Material.IRON_BOOTS, Material.CHAINMAIL_BOOTS, Material.GOLD_BOOTS, Material.DIAMOND_BOOTS),
        SHIELD(45);

        private static final HashMap<Integer, ItemType> ITEM_TYPE_BY_SLOT_ID = new HashMap<>();

        static {
            for (ItemType type : ItemType.values()) {
                ItemType.ITEM_TYPE_BY_SLOT_ID.put(type.getSlot(), type);
            }
        }

        private final int slot;
        private final List<Material> materials;

        ItemType(int slot, Material... materials) {
            this.slot = slot;
            this.materials = Collections.unmodifiableList(Arrays.asList(materials));
        }

        public static ItemType getItemTypeBySlotId(int slot) {
            return ItemType.ITEM_TYPE_BY_SLOT_ID.get(slot);
        }

        public static ItemType getItemTypeByItem(ItemStack itemStack) {
            for (ItemType type : ItemType.values()) {
                if (type.getMaterials().contains(itemStack.getType())) {
                    return type;
                }
            }
            return null;
        }

        public static boolean isWearableItem(ItemStack item) {
            return getItemTypeByItem(item) != null;
        }

        public int getSlot() {
            return slot;
        }

        public List<Material> getMaterials() {
            return materials;
        }
    }
}

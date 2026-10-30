package pl.jedyny;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Na serwerze można zrobić tylko jedną sztukę maca i jedną sztukę każdego
 * przedmiotu z netherytu (miecz, kilof, siekiera, łopata, motyka, włócznia,
 * hełm, napierśnik, getry, buty). Limit liczy się osobno dla każdego typu.
 * Liczy się crafting (stół, siatka 2x2, crafter) oraz kowadło ulepszeń.
 */
public final class OneOfAKind extends JavaPlugin implements Listener {

    private static final Component MSG_TAKEN = Component.text(
            "Ten przedmiot został już stworzony na tym serwerze. Drugiego się nie da.",
            NamedTextColor.RED);
    private static final Component MSG_SHIFT = Component.text(
            "Tego przedmiotu nie da się zrobić shift-klikiem. Kliknij wynik zwykłym kliknięciem.",
            NamedTextColor.RED);

    private final Set<String> crafted = new HashSet<>();
    private File dataFile;

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        dataFile = new File(getDataFolder(), "data.yml");
        crafted.addAll(YamlConfiguration.loadConfiguration(dataFile).getStringList("crafted"));
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Załadowano. Już zrobione: " + (crafted.isEmpty() ? "nic" : crafted));
    }

    // ---------- które przedmioty są limitowane ----------

    private static boolean isLimited(Material m) {
        if (m == Material.MACE) return true;
        String n = m.name();
        return n.startsWith("NETHERITE_")
                && !n.endsWith("_INGOT")
                && !n.endsWith("_SCRAP")
                && !n.endsWith("_BLOCK")
                && !n.contains("TEMPLATE");
    }

    private boolean isTaken(ItemStack item) {
        return item != null && isLimited(item.getType()) && crafted.contains(item.getType().name());
    }

    private void markCrafted(ItemStack item) {
        if (item == null || !isLimited(item.getType())) return;
        if (crafted.add(item.getType().name())) {
            save();
            getLogger().info("Pierwszy egzemplarz zrobiony: " + item.getType().name());
        }
    }

    // ---------- stół rzemieślniczy i siatka 2x2 ----------

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        if (isTaken(e.getInventory().getResult())) {
            e.getInventory().setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent e) {
        ItemStack result = e.getCurrentItem();
        if (result == null) result = e.getRecipe().getResult();
        if (!isLimited(result.getType())) return;

        if (isTaken(result)) {
            e.setCancelled(true);
            e.getWhoClicked().sendMessage(MSG_TAKEN);
            return;
        }
        if (e.isShiftClick()) {
            // shift-klik potrafi zrobić kilka sztuk naraz, więc go tu nie przepuszczamy
            e.setCancelled(true);
            e.getWhoClicked().sendMessage(MSG_SHIFT);
            return;
        }
        markCrafted(result);
    }

    // ---------- crafter (blok) ----------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafter(CrafterCraftEvent e) {
        ItemStack result = e.getResult();
        if (!isLimited(result.getType())) return;
        if (isTaken(result)) {
            e.setCancelled(true);
            return;
        }
        markCrafted(result);
    }

    // ---------- kowadło ulepszeń (tak się robi netheryt) ----------

    @EventHandler
    public void onPrepareSmith(PrepareSmithingEvent e) {
        if (isTaken(e.getInventory().getResult())) {
            e.getInventory().setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSmith(SmithItemEvent e) {
        ItemStack result = e.getCurrentItem();
        if (result == null || !isLimited(result.getType())) return;
        if (isTaken(result)) {
            e.setCancelled(true);
            e.getWhoClicked().sendMessage(MSG_TAKEN);
            return;
        }
        markCrafted(result);
    }

    // ---------- zapis ----------

    private void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("crafted", new ArrayList<>(new TreeSet<>(crafted)));
        try {
            cfg.save(dataFile);
        } catch (IOException ex) {
            getLogger().severe("Nie udało się zapisać data.yml: " + ex.getMessage());
        }
    }

    // ---------- komenda admina ----------

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(Component.text("Już zrobione: "
                    + (crafted.isEmpty() ? "nic" : String.join(", ", new TreeSet<>(crafted)))));
            return true;
        }
        if (args[0].equalsIgnoreCase("reset") && args.length == 2) {
            if (args[1].equalsIgnoreCase("all")) {
                crafted.clear();
            } else if (!crafted.remove(args[1].toUpperCase(Locale.ROOT))) {
                sender.sendMessage(Component.text("Tego nie ma na liście. Zobacz /jedyny list", NamedTextColor.RED));
                return true;
            }
            save();
            sender.sendMessage(Component.text("Zresetowano: " + args[1], NamedTextColor.GREEN));
            return true;
        }
        sender.sendMessage(Component.text("/jedyny list | /jedyny reset <ITEM|all>"));
        return true;
    }
}

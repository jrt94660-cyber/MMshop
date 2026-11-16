package com.mmshop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import java.text.DecimalFormat;
import java.util.*;

public final class MMshop extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private Economy economy;
    private NamespacedKey actionKey, categoryKey, itemKey;
    private final Set<UUID> shopViewers = new HashSet<>();
    @Override public void onEnable() {
        saveDefaultConfig();
        actionKey=new NamespacedKey(this,"action"); categoryKey=new NamespacedKey(this,"category"); itemKey=new NamespacedKey(this,"item");
        setupEconomy(); Bukkit.getPluginManager().registerEvents(this,this);
        getCommand("shop").setExecutor(this); getCommand("shop").setTabCompleter(this);
        getCommand("shopadmin").setExecutor(this); getCommand("shopadmin").setTabCompleter(this);
        getLogger().info("MMshop enabled.");
    }
    private void setupEconomy() {
        RegisteredServiceProvider<Economy> rsp=getServer().getServicesManager().getRegistration(Economy.class);
        economy=rsp==null?null:rsp.getProvider();
    }
    private String c(String s){return ChatColor.translateAlternateColorCodes('&',s==null?"":s);}
    private String msg(String key){return c(getConfig().getString("messages.prefix","")+getConfig().getString("messages."+key,""));}
    private String money(double n){int d=Math.max(0,Math.min(4,getConfig().getInt("settings.decimal-places",2)));return getConfig().getString("settings.currency-symbol","$")+new DecimalFormat("#,##0"+(d>0?"."+"0".repeat(d):"")).format(n);}
    private String pretty(String s){return Arrays.stream(s.split("_")).map(x->x.isEmpty()?x:x.substring(0,1).toUpperCase()+x.substring(1)).reduce((a,b)->a+" "+b).orElse(s);}
    private ItemStack tagged(Material m,String name,List<String> lore,String action,String cat,String item){
        ItemStack stack=new ItemStack(m); ItemMeta meta=stack.getItemMeta(); meta.setDisplayName(c(name)); meta.setLore(lore.stream().map(this::c).toList());
        meta.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,action);
        if(cat!=null)meta.getPersistentDataContainer().set(categoryKey,PersistentDataType.STRING,cat);
        if(item!=null)meta.getPersistentDataContainer().set(itemKey,PersistentDataType.STRING,item);
        stack.setItemMeta(meta); return stack;
    }
    private Inventory create(String title){return Bukkit.createInventory(null,54,c(title));}
    private void openMain(Player p){
        Inventory inv=create(getConfig().getString("settings.title","&8ᴍᴀʀᴋᴇᴛ"));
        ConfigurationSection cats=getConfig().getConfigurationSection("categories");
        if(cats!=null)for(String id:cats.getKeys(false)){
            ConfigurationSection cat=cats.getConfigurationSection(id); Material m=Material.matchMaterial(cat.getString("icon","CHEST")); if(m==null)m=Material.CHEST;
            inv.setItem(Math.max(0,Math.min(53,cat.getInt("slot",20))),tagged(m,cat.getString("name",id),List.of("&8ᴄᴀᴛᴇɢᴏʀʏ","&7Browse the available items.","","&a➜ Click to browse"),"category",id,null));
        }
        p.openInventory(inv); shopViewers.add(p.getUniqueId());
    }
    private void openCategory(Player p,String id){
        ConfigurationSection cat=getConfig().getConfigurationSection("categories."+id); if(cat==null){p.sendMessage(msg("invalid-item"));return;}
        Inventory inv=create(cat.getString("name","&8Shop")); ConfigurationSection items=cat.getConfigurationSection("items");
        if(items!=null)for(String key:items.getKeys(false)){
            ConfigurationSection d=items.getConfigurationSection(key); Material m=Material.matchMaterial(d.getString("material","STONE")); if(m==null)continue;
            double buy=d.getDouble("buy",-1), sell=d.getDouble("sell",-1)*getConfig().getDouble("settings.sell-multiplier",.5);
            List<String> lore=new ArrayList<>(d.getStringList("lore")); lore.add("");
            lore.add("&8&m--------------------"); lore.add("&aᴘᴜʀᴄʜᴀsᴇ"); if(buy>=0)lore.add("&fPrice: &a"+money(buy));
            lore.add(""); lore.add("&cᴠᴀʟᴜᴇ"); if(sell>=0)lore.add("&fSell: &c"+money(sell));
            lore.add(""); lore.add("&eLeft-click &7to purchase"); lore.add("&eRight-click &7to sell");
            inv.setItem(Math.max(0,Math.min(53,d.getInt("slot",10))),tagged(m,d.getString("name","&f"+pretty(key)),lore,"product",id,key));
        }
        p.openInventory(inv); shopViewers.add(p.getUniqueId());
    }
    private void sound(Player p){if(getConfig().getBoolean("settings.sounds",true))p.playSound(p.getLocation(),Sound.UI_BUTTON_CLICK,.5f,1.2f);}
    @Override public boolean onCommand(CommandSender s,Command cmd,String label,String[] a){
        if(cmd.getName().equalsIgnoreCase("shop")){
            if(!(s instanceof Player p)){s.sendMessage("Players only.");return true;}
            if(!p.hasPermission("mmshop.use")){p.sendMessage(msg("no-permission"));return true;}
            if(a.length==0)openMain(p);else openCategory(p,a[0].toLowerCase(Locale.ROOT));return true;
        }
        if(!s.hasPermission("mmshop.admin")){s.sendMessage(msg("no-permission"));return true;}
        if(a.length==0){s.sendMessage(c("&b/shopadmin reload | setprice <category> <item> <buy|sell> <amount> | additem <category> <material> <buy> <sell> | removeitem <category> <item>"));return true;}
        switch(a[0].toLowerCase(Locale.ROOT)){
            case "reload"->{reloadConfig();setupEconomy();s.sendMessage(msg("reloaded"));}
            case "setprice"->{
                if(a.length!=5){s.sendMessage(c("&c/shopadmin setprice <category> <item> <buy|sell> <amount>"));return true;}
                String path="categories."+a[1]+".items."+a[2]+"."+a[3].toLowerCase();
                if(!getConfig().contains(path)||!(a[3].equalsIgnoreCase("buy")||a[3].equalsIgnoreCase("sell"))){s.sendMessage(msg("invalid-item"));return true;}
                try{double v=Double.parseDouble(a[4]);if(v<0||!Double.isFinite(v))throw new NumberFormatException();getConfig().set(path,v);saveConfig();s.sendMessage(c("&aUpdated price to "+money(v)));}
                catch(NumberFormatException ex){s.sendMessage(c("&cEnter a valid non-negative price."));}
            }
            case "additem"->{
                if(a.length!=5){s.sendMessage(c("&c/shopadmin additem <category> <material> <buy> <sell>"));return true;}
                if(!getConfig().isConfigurationSection("categories."+a[1])){s.sendMessage(msg("invalid-item"));return true;}
                Material m=Material.matchMaterial(a[2]);if(m==null||!m.isItem()){s.sendMessage(c("&cInvalid material."));return true;}
                try{double b=Double.parseDouble(a[3]),v=Double.parseDouble(a[4]);if(b<0||v<0)throw new NumberFormatException();
                    String k=m.name().toLowerCase(Locale.ROOT),base="categories."+a[1]+".items."+k;
                    getConfig().set(base+".material",m.name());getConfig().set(base+".name","&f"+pretty(k));getConfig().set(base+".buy",b);getConfig().set(base+".sell",v);getConfig().set(base+".slot",firstFree(a[1]));getConfig().set(base+".lore",List.of("&7A new item in the market."));saveConfig();s.sendMessage(c("&aAdded "+k+"."));
                }catch(NumberFormatException ex){s.sendMessage(c("&cPrices must be valid non-negative numbers."));}
            }
            case "removeitem"->{
                if(a.length!=3){s.sendMessage(c("&c/shopadmin removeitem <category> <item>"));return true;}
                String path="categories."+a[1]+".items."+a[2];if(!getConfig().contains(path))s.sendMessage(msg("invalid-item"));else{getConfig().set(path,null);saveConfig();s.sendMessage(c("&aItem removed."));}
            }
            default->s.sendMessage(c("&cUnknown subcommand."));
        }return true;
    }
    private int firstFree(String cat){Set<Integer> used=new HashSet<>();ConfigurationSection sec=getConfig().getConfigurationSection("categories."+cat+".items");if(sec!=null)for(String k:sec.getKeys(false))used.add(sec.getInt(k+".slot",10));for(int i=10;i<45;i++)if(!used.contains(i))return i;return 44;}
    private List<String> keys(String path,String prefix){ConfigurationSection sec=getConfig().getConfigurationSection(path);return sec==null?List.of():sec.getKeys(false).stream().filter(x->x.toLowerCase().startsWith(prefix.toLowerCase())).toList();}
    @Override public List<String> onTabComplete(CommandSender s,Command cmd,String alias,String[] a){
        if(cmd.getName().equalsIgnoreCase("shop"))return a.length==1?keys("categories",a[0]):List.of();
        if(a.length==1)return List.of("reload","setprice","additem","removeitem").stream().filter(x->x.startsWith(a[0].toLowerCase())).toList();
        if(a.length==2)return keys("categories",a[1]);
        if(a.length==3&&(a[0].equalsIgnoreCase("setprice")||a[0].equalsIgnoreCase("removeitem")))return keys("categories."+a[1]+".items",a[2]);
        if(a.length==4&&a[0].equalsIgnoreCase("setprice"))return List.of("buy","sell").stream().filter(x->x.startsWith(a[3].toLowerCase())).toList();
        return List.of();
    }
    @EventHandler public void click(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p)||!shopViewers.contains(p.getUniqueId()))return;e.setCancelled(true);
        if(e.getClickedInventory()==null||e.getClickedInventory()!=e.getView().getTopInventory())return;
        ItemStack it=e.getCurrentItem();if(it==null||!it.hasItemMeta())return;ItemMeta meta=it.getItemMeta();
        String action=meta.getPersistentDataContainer().get(actionKey,PersistentDataType.STRING);if(action==null)return;
        String cat=meta.getPersistentDataContainer().get(categoryKey,PersistentDataType.STRING);
        if(action.equals("category")){sound(p);openCategory(p,cat);return;}
        String key=meta.getPersistentDataContainer().get(itemKey,PersistentDataType.STRING);if(cat!=null&&key!=null)trade(p,cat,key,e.isRightClick());
    }
    @EventHandler public void drag(InventoryDragEvent e){if(e.getWhoClicked() instanceof Player p&&shopViewers.contains(p.getUniqueId()))e.setCancelled(true);}
    @EventHandler public void close(InventoryCloseEvent e){shopViewers.remove(e.getPlayer().getUniqueId());}
    private void trade(Player p,String cat,String key,boolean sell){
        if(economy==null){p.sendMessage(msg("economy-missing"));return;}
        ConfigurationSection d=getConfig().getConfigurationSection("categories."+cat+".items."+key);if(d==null)return;
        Material m=Material.matchMaterial(d.getString("material","STONE"));if(m==null)return;
        if(sell&&!getConfig().getBoolean("settings.sell-enabled",true)||!sell&&!getConfig().getBoolean("settings.buy-enabled",true)){p.sendMessage(msg("invalid-item"));return;}
        double unit=sell?d.getDouble("sell",-1)*getConfig().getDouble("settings.sell-multiplier",.5):d.getDouble("buy",-1);if(unit<0){p.sendMessage(msg("invalid-item"));return;}
        int amount=sell?Math.min(m.getMaxStackSize(),count(p,m)):Math.min(m.getMaxStackSize(),Math.max(1,getConfig().getInt("settings.max-buy-amount",64)));
        if(amount<1){p.sendMessage(c("&cYou do not have any "+pretty(key)+" to sell."));return;}
        double total=unit*amount;
        if(sell){if(count(p,m)<amount)return;remove(p,m,amount);economy.depositPlayer(p,total);p.sendMessage(msg("sold").replace("%amount%",String.valueOf(amount)).replace("%item%",pretty(key)).replace("%price%",money(total)));}
        else{if(!economy.has(p,total)){p.sendMessage(msg("not-enough"));return;}if(!space(p,m,amount)){p.sendMessage(msg("inventory-full"));return;}economy.withdrawPlayer(p,total);p.getInventory().addItem(new ItemStack(m,amount));p.sendMessage(msg("purchased").replace("%amount%",String.valueOf(amount)).replace("%item%",pretty(key)).replace("%price%",money(total)));}
        sound(p);
    }
    private int count(Player p,Material m){int n=0;for(ItemStack i:p.getInventory().getStorageContents())if(i!=null&&i.getType()==m)n+=i.getAmount();return n;}
    private boolean space(Player p,Material m,int amount){int n=0;for(ItemStack i:p.getInventory().getStorageContents()){if(i==null)n+=m.getMaxStackSize();else if(i.getType()==m)n+=m.getMaxStackSize()-i.getAmount();}return n>=amount;}
    private void remove(Player p,Material m,int amount){for(int s=0;s<36&&amount>0;s++){ItemStack i=p.getInventory().getItem(s);if(i==null||i.getType()!=m)continue;int take=Math.min(amount,i.getAmount());i.setAmount(i.getAmount()-take);amount-=take;if(i.getAmount()==0)p.getInventory().setItem(s,null);}}
}
package ravioli.gravioli.tekkit.machine;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import ravioli.gravioli.tekkit.Tekkit;
import ravioli.gravioli.tekkit.machine.machines.MachineFilter;
import ravioli.gravioli.tekkit.machine.transport.MovingItem;
import ravioli.gravioli.tekkit.machine.transport.MovingItemSet;
import ravioli.gravioli.tekkit.machine.transport.Pipe;
import ravioli.gravioli.tekkit.manager.MachineManager;
import ravioli.gravioli.tekkit.storage.Persistent;
import ravioli.gravioli.tekkit.storage.Sqlite;
import ravioli.gravioli.tekkit.util.CommonUtils;
import ravioli.gravioli.tekkit.util.InventoryUtils;

public abstract class MachineBase implements Machine, Listener, Runnable {
    @Persistent
    private Location location;

    @Persistent
    private UUID owner;

    private int id = -1;
    protected int task;

    protected boolean[] acceptableInputs = new boolean[BlockFace.values().length];
    protected boolean[] acceptableOutputs = new boolean[BlockFace.values().length];

    public void save() {
        Sqlite db = Tekkit.getInstance().getSqlite();
        try (PreparedStatement statement = db.getConnection().prepareStatement("SELECT * FROM `" + this.getName() + "` WHERE `id` = ?")) {
            statement.setInt(1, this.id);

            ResultSet results = statement.executeQuery();
            if (results.next()) {
                this.mapFieldsToStatement(statement, false);
            } else {
                this.mapFieldsToStatement(statement, true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void mapFieldsToStatement(PreparedStatement statement, boolean insert) {
        String sql = insert? "INSERT INTO `" + this.getName() + "` (" : "UPDATE `" + this.getName() + "` set";

        LinkedHashMap<String, Field> types = new LinkedHashMap<String, Field>();
        HashMap<String, Boolean> fieldAccessibility = new HashMap<String, Boolean>();

        for (Field field : CommonUtils.getAllFields(this.getClass())) {
            if (field.getAnnotation(Persistent.class) == null) {
                continue;
            }
            boolean accessibility = field.isAccessible();

            field.setAccessible(true);
            String name = field.getName();

            types.put(name, field);
            fieldAccessibility.put(name, accessibility);

            sql += insert? "`" + name + "`, " : " `" + name + "` = ?,";
        }
        if (insert) {
            sql = sql.substring(0, sql.length() - 2) + ") VALUES (";
            for (int i = 0; i < types.size(); i++) {
                sql += "?, ";
            }
            sql = sql.substring(0, sql.length() - 2) + ")";
        } else {
            sql = sql.substring(0, sql.length() - 1) + " WHERE `id` = ?";
        }

        int count = 1;
        try (PreparedStatement statement2 = insert?
                Tekkit.getInstance().getSqlite().getConnection().prepareStatement(sql, new String[] {"id"}) :
                Tekkit.getInstance().getSqlite().getConnection().prepareStatement(sql)) {
            for (Map.Entry<String, Field> entrySet : types.entrySet()) {
                String name = entrySet.getKey();
                Field field = entrySet.getValue();

                Class type = field.getType();
                Object object = field.get(this);

                if (object == null) {
                    statement.setString(count, "null");
                    count++;
                    continue;
                }
                if (type.isAssignableFrom(Integer.TYPE)) {
                    statement2.setInt(count, (Integer) object);
                } else if (type.isAssignableFrom(Long.TYPE)) {
                    statement2.setLong(count, (Long) object);
                } else if (type.isAssignableFrom(String.class)) {
                    statement2.setString(count, (String) object);
                } else if (type.isAssignableFrom(Location.class)) {
                    statement2.setString(count, CommonUtils.locationToString((Location) object));
                } else if (type.isAssignableFrom(Inventory.class)) {
                    Inventory inventory = (Inventory) object;
                    statement2.setString(count, InventoryUtils.itemStackArrayToBase64(inventory.getContents()));
                } else if (type.isAssignableFrom(Boolean.TYPE)) {
                    statement2.setBoolean(count, (Boolean) object);
                } else {
                    statement2.setString(count, object.toString());
                }
                count++;

                field.setAccessible(fieldAccessibility.get(name));
            }
            if (!insert) {
                statement2.setInt(count, this.id);
            }
            statement2.executeUpdate();

            if (insert) {
                ResultSet results = statement2.getGeneratedKeys();
                if (results.next()) {
                    this.id = results.getInt(1);
                }
            }
        } catch (SQLException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void saveAsync() {
        Tekkit.getInstance().getSqlite().runAsync(() -> this.save());
    }

    public void load(ResultSet results) throws SQLException, IllegalAccessException, IOException {
        this.id = results.getInt("id");
        for (Field field : CommonUtils.getAllFields(this.getClass())) {
            if (field.getAnnotation(Persistent.class) == null) {
                continue;
            }
            boolean accessible = field.isAccessible();
            field.setAccessible(true);

            Class type = field.getType();
            String name = field.getName();

            if (type.isAssignableFrom(Integer.TYPE)) {
                field.setInt(this, results.getInt(name));
            } else if (type.isAssignableFrom(Long.TYPE)) {
                field.setLong(this, results.getLong(name));
            } else if (type.isAssignableFrom(String.class)) {
                field.set(this, results.getString(name));
            } else if (type.isAssignableFrom(Location.class)) {
                field.set(this, CommonUtils.stringToLocation(results.getString(name)));
            } else if (type.isAssignableFrom(Inventory.class)) {
                Inventory inventory = (Inventory) field.get(this);
                inventory.setContents(InventoryUtils.itemStackArrayFromBase64(results.getString(name)));
            } else if (type.isAssignableFrom(Boolean.TYPE)) {
                field.set(this, results.getBoolean(name));
            } else if (type.isAssignableFrom(UUID.class)) {
                field.set(this, UUID.fromString(results.getString(name)));
            } else if (type.isAssignableFrom(MovingItemSet.class)) {
                MovingItemSet set = new MovingItemSet();
                String result = results.getString(name);
                if (!result.isEmpty()) {
                    String[] items = result.split(":");
                    for (int i = 0; i < items.length; ++i) {
                        String item = items[i];
                        String[] itemData = item.split("\\|");
                        ItemStack itemStack = InventoryUtils.itemStackArrayFromBase64(itemData[0])[0];
                        Location location = CommonUtils.stringToLocation(itemData[1]);
                        BlockFace input = BlockFace.valueOf(itemData[2]);
                        BlockFace output = BlockFace.valueOf(itemData[3]);
                        MovingItem movingItem = new MovingItem(itemStack, location, input);
                        movingItem.output = output;
                        movingItem.moveTo(location);
                        set.getItems().add(movingItem);
                    }
                }
                field.set(this, set);
            } else if (type.isEnum()) {
                field.set(this, Enum.valueOf(type, results.getString(name)));
            }
            field.setAccessible(accessible);
        }
        this.enable();
    }

    public boolean acceptableInput(BlockFace face) {
        return this.acceptableInputs[face.ordinal()];
    }

    public boolean acceptableOutput(BlockFace face) {
        return this.acceptableOutputs[face.ordinal()];
    }

    public void onCreate() {};

    public void onDestroy() {};

    public void onEnable() {};

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, Tekkit.getInstance());
        Tekkit.getMachineManager().addMachine(this);

        this.onEnable();
    }

    public void create(Player player, Location location) {
        this.owner = player.getUniqueId();
        this.location = location;

        this.onCreate();
        this.enable();

        this.saveAsync();
    }

    public void destroy(boolean doDrop) {
        if (doDrop) {
            this.getDrops().forEach(drop -> this.location.getWorld().dropItemNaturally(this.location, drop));
            if (this.doDrop()) {
                this.location.getWorld().dropItemNaturally(this.location, this.getRecipe().getResult());
            }
        }
        if (this.getBlock().getState() instanceof InventoryHolder) {
            InventoryHolder holder = (InventoryHolder) this.getBlock().getState();
            holder.getInventory().clear();
        }
        this.getBlock().setTypeIdAndData(0, (byte) 0, true);

        Tekkit.getInstance().getSqlite().runAsync(() -> {
            try (PreparedStatement statement = Tekkit.getInstance().getSqlite().getConnection().prepareStatement("DELETE FROM `" + this.getName() + "` WHERE `id` = ?")) {
                statement.setInt(1, this.id);
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        HandlerList.unregisterAll(this);
        this.stopTask();
        this.onDestroy();

        Tekkit.getMachineManager().removeMachine(this);
    }

    public UUID getOwner() {
        return this.owner;
    }

    public int getId() {
        return this.id;
    }

    public Location getLocation() {
        return this.location;
    }

    public Block getBlock() {
        return this.location.getBlock();
    }

    public World getWorld() {
        return this.location.getWorld();
    }

    protected void updateTask(long tickrate) {
        this.stopTask();
        this.task = Bukkit.getScheduler().scheduleSyncRepeatingTask(Tekkit.getInstance(), this, tickrate, tickrate);
    }

    protected void stopTask() {
        Bukkit.getScheduler().cancelTask(this.task);
    }

    protected void startTaskLater(long delay) {
        this.stopTask();
        this.task = Bukkit.getScheduler().scheduleSyncDelayedTask(Tekkit.getInstance(), this, delay);
    }

    public void onMachinePieceBreak(Block block) {}

    protected MachineBase checkBlockMachinePiece(Block block) {
        Object machine = block.getMetadata("machine").get(0).value();
        if (block.hasMetadata("machine") &&  machine instanceof MachineBase) {
            return (MachineBase) machine;
        }
        return null;
    }

    protected void routeItem(BlockFace outputFace, ItemStack ... items) {
        Block output = this.getBlock().getRelative(outputFace);
        MachineBase machine = MachineManager.getMachineByLocation(output.getLocation());
        if (output.getState() instanceof InventoryHolder && machine == null) {
            InventoryHolder holder = (InventoryHolder) output.getState();
            HashMap<Integer, ItemStack> drops = holder.getInventory().addItem(items);
            drops.forEach((k, v) -> this.location.getWorld().dropItemNaturally(this.location, v));
        } else {
            ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
            if (machine instanceof Pipe) {
                Pipe pipe = (Pipe) machine;
                if (pipe.acceptableInput(outputFace.getOppositeFace())) {
                    for (ItemStack item : items) {
                        MovingItem movingItem = new MovingItem(item, output.getLocation(), outputFace.getOppositeFace());
                        pipe.addItem(movingItem, outputFace.getOppositeFace());
                    }
                } else {
                    drops.addAll(Arrays.asList(items));
                }
            } else if (machine instanceof MachineFilter) {
                MachineFilter filter = (MachineFilter) machine;
                if (filter.acceptableInput(outputFace.getOppositeFace())) {
                    for (ItemStack item : items) {
                        if (filter.canTransport(item)) {
                            filter.addItem(item, outputFace.getOppositeFace());
                            continue;
                        }
                        drops.add(item);
                    }
                } else {
                    drops.addAll(Arrays.asList(items));
                }
            } else {
                drops.addAll(Arrays.asList(items));
            }
            drops.forEach(drop -> {this.location.getWorld().dropItemNaturally(output.getLocation(), drop);System.out.println("drep");});
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getLocation().equals(this.location)) {
            this.destroy(true);
        }
    }
}
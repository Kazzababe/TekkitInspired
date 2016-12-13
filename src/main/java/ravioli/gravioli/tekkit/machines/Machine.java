package ravioli.gravioli.tekkit.machines;

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
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import ravioli.gravioli.tekkit.Tekkit;
import ravioli.gravioli.tekkit.database.utils.DatabaseObject;
import ravioli.gravioli.tekkit.database.utils.DatabaseSerializable;
import ravioli.gravioli.tekkit.machines.standard.MachineFilter;
import ravioli.gravioli.tekkit.machines.transport.MovingItem;
import ravioli.gravioli.tekkit.machines.transport.pipes.Pipe;
import ravioli.gravioli.tekkit.utils.CommonUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Represents a machine
 */
public abstract class Machine implements Listener, Runnable {
    private Tekkit plugin;

    @DatabaseObject
    private Location location;
    @DatabaseObject
    private UUID owner;

    private String worldName;
    private int id = -1;
    private int task;
    private boolean loaded;

    protected boolean[] acceptableInputs = new boolean[BlockFace.values().length];
    protected boolean[] acceptableOutputs = new boolean[BlockFace.values().length];

    public Machine(Tekkit plugin) {
        this.plugin = plugin;
    }

    /**
     * Gets an instance of the Tekkit plugin
     *
     * @return Tekkit plugin
     */
    public Tekkit getPlugin() {
        return plugin;
    }

    /**
     * Gets whether or not the given block face of the machine can have blocks inputted from it
     *
     * @param face block face to check
     * @return whether the machine can accept items from that face
     */
    public boolean acceptableInput(BlockFace face) {
        return this.acceptableInputs[face.ordinal()];
    }

    /**
     * Gets whether or not the given block face of the machine can have blocks outputted from it
     *
     * @param face block face to check
     * @return whether the machine outputs blocks from that face
     */
    public boolean acceptableOutput(BlockFace face) {
        return this.acceptableOutputs[face.ordinal()];
    }

    /**
     * Whether or not the machine has been loaded into the world
     *
     * @return if the machine is loaded or not
     */
    public boolean isLoaded() {
        return this.loaded;
    }

    /**
     * Gets the unique id of the player that created the machine
     *
     * @return the owners uuid
     */
    public UUID getOwner() {
        return owner;
    }

    /**
     * Gets the id of the machine
     *
     * @return the machine's id
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the name of the world the machine is located in
     * Will always return the name the world is in, regardless of whether or not the machine has been loaded
     *
     * @return the machine's world
     */
    public String getWorldName() {
        return worldName;
    }

    /**
     * Saves the machine to the database asynchronously
     */
    public void saveAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            save();
        });
    }

    /**
     * Saves the machine to the database
     */
    public void save() {
        boolean inserting = id == -1;
        String sql = inserting ?
                "INSERT INTO `" + getTableName() + "` (" :
                "UPDATE `" + getTableName() + "` set";

        LinkedHashMap<String, Field> types = new LinkedHashMap<String, Field>();
        HashMap<String, Boolean> fieldAccessibility = new HashMap<String, Boolean>();

        for (Field field : CommonUtils.getAllFields(getClass())) {
            if (field.getAnnotation(DatabaseObject.class) == null) {
                continue;
            }
            boolean accessibility = field.isAccessible();
            field.setAccessible(true);

            String name = field.getName();
            types.put(name, field);
            fieldAccessibility.put(name, accessibility);

            sql += inserting ?
                    "`" + name + "`, " :
                    " `" + name + "` = ?,";
        }
        if (inserting) {
            sql = sql.substring(0, sql.length() - 2) + ") VALUES (";
            for (int i = 0; i < types.size(); i++) {
                sql += "?, ";
            }
            sql = sql.substring(0, sql.length() - 2) + ")";
        } else {
            sql = sql.substring(0, sql.length() - 1) + " WHERE `id` = ?";
        }

        int count = 1;
        try (PreparedStatement statement = inserting ?
                plugin.getSqlite().getConnection().prepareStatement(sql, new String[]{"id"}) :
                plugin.getSqlite().getConnection().prepareStatement(sql)) {
            for (Map.Entry<String, Field> entry : types.entrySet()) {
                String name = entry.getKey();
                Field field = entry.getValue();

                Class type = field.getType();
                Object object = field.get(this);

                boolean serializableInterface = false;
                for (Class c : type.getInterfaces()) {
                    if (c.equals(DatabaseSerializable.class)) {
                        serializableInterface = true;
                        break;
                    }
                }
                if (type.isPrimitive()) {
                    statement.setObject(count, object);
                } else if (serializableInterface) {
                    statement.setString(count, (String) object.getClass().getMethod(
                            "serialize").invoke(object));
                } else if (MachinesManager.hasSerializer(type)) {
                    statement.setString(count, MachinesManager.getSerializer(type).serialize(object));
                } else if (type.isEnum()) {
                    statement.setString(count, object.toString());
                }
                count++;

                field.setAccessible(fieldAccessibility.get(name));
            }
            if (!inserting) {
                statement.setInt(count, id);
            }
            statement.executeUpdate();

            if (inserting) {
                ResultSet results = statement.getGeneratedKeys();
                if (results.next()) {
                    id = results.getInt(1);
                }
            }
        } catch (SQLException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads basic information for the machine from the database
     */
    public void preload(ResultSet results) throws SQLException {
        id = results.getInt("id");
        worldName = results.getString("location").split(",")[0];

        if (MachinesManager.unloadedMachines.containsKey(worldName)) {
            MachinesManager.unloadedMachines.get(worldName).add(this);
        } else {
            MachinesManager.unloadedMachines.put(worldName, new ArrayList());
            MachinesManager.unloadedMachines.get(worldName).add(this);
        }
    }

    /**
     * Loads the machine from the database
     */
    public void load(ResultSet results) throws SQLException, IllegalAccessException, IOException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, InstantiationException {
        for (Field field : CommonUtils.getAllFields(this.getClass())) {
            DatabaseObject annotation = field.getAnnotation(DatabaseObject.class);
            if (annotation == null) {
                continue;
            }
            boolean accessible = field.isAccessible();
            field.setAccessible(true);

            Class type = field.getType();
            String name = field.getName();

            if (type.isPrimitive()) {
                field.set(this, results.getObject(name));
            } else if (type.isAssignableFrom(String.class)) {
                field.set(this, results.getString(name));
            } else if (type.isAssignableFrom(DatabaseSerializable.class)) {
                field.set(this, field.get(this).getClass().getMethod("deserialize", String.class).invoke(null, results.getString(name)));
            } else if (MachinesManager.hasSerializer(type)) {
                field.set(this, MachinesManager.getSerializer(type).deserialize(results.getString(name)));
            } else if (type.isEnum()) {
                field.set(this, Enum.valueOf(type, results.getString(name)));
            }
        }
        enable();
    }

    /**
     * Deletes the machine from the database
     * Machine deletion is always done asynchronously
     */
    public void delete() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement statement = plugin.getSqlite().getConnection().prepareStatement("DELETE FROM `" + getTableName() + "` WHERE `id` = ?")) {
                statement.setInt(1, id);
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Start or update the machines task with a given tickrate
     *
     * @param tickrate machines tickrate
     */
    protected void updateTask(long tickrate) {
        stopTask();
        task = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, tickrate, tickrate);
    }

    /**
     * Start the machines task with a delay
     *
     * @param delay delay in ticks
     */
    protected void startTaskLater(long delay) {
        stopTask();
        task = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this, delay);
    }

    /**
     * Stop the machines currently running task
     */
    protected void stopTask() {
        Bukkit.getScheduler().cancelTask(task);
    }

    @Override
    public void run() {
        if (getLocation().getChunk().isLoaded()) {
            runMachine();
        }
    }

    /**
     * Used to create a machine
     */
    public void create(Player player, Location location) {
        owner = player.getUniqueId();
        this.location = location;
        worldName = getWorld().getName();

        onCreate();
        enable();
    }

    /**
     * Used to enable a machine
     */
    public void enable() {
        plugin.registerListener(this);
        MachinesManager.addMachine(this);

        loaded = true;
        onEnable();
    }

    /**
     * Used to destroy a machine
     *
     * @param drop whether or not the machine drops its items.
     */
    public void destroy(boolean drop) {
        if (drop) {
            getDrops().forEach(item -> getWorld().dropItem(location, item));
            getWorld().dropItem(location, getRecipe().getResult());
        }
        // If the block the machine encompasses is an inventory block, clear it's inventory before breaking it
        if (getBlock().getState() instanceof InventoryHolder) {
            ((InventoryHolder) getBlock().getState()).getInventory().clear();
        }
        getBlock().setTypeIdAndData(0, (byte) 0, true);

        HandlerList.unregisterAll(this);
        stopTask();
        onDestroy();

        MachinesManager.removeMachine(this);
        if (MachinesManager.markedForDelete.containsKey(worldName)) {
            MachinesManager.markedForDelete.get(worldName).add(this);
        } else {
            MachinesManager.markedForDelete.put(worldName, new ArrayList());
            MachinesManager.markedForDelete.get(worldName).add(this);
        }
    }

    protected void routeItem(BlockFace outputFace, ItemStack... items) {
        Block output = this.getBlock().getRelative(outputFace);
        Machine machine = MachinesManager.getMachineByLocation(output.getLocation());
        if (output.getState() instanceof InventoryHolder && machine == null) {
            InventoryHolder holder = (InventoryHolder) output.getState();
            HashMap<Integer, ItemStack> drops = holder.getInventory().addItem(items);
            drops.forEach((k, v) -> getWorld().dropItemNaturally(location, v));
        } else {
            List<ItemStack> drops = new ArrayList<ItemStack>();
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
            drops.forEach(drop -> getWorld().dropItemNaturally(output.getLocation(), drop));
        }
    }

    /**
     * Checks if a block is a machine block
     *
     * @param block block to check
     * @return the machine the block represents
     */
    protected Machine checkBlockMachinePiece(Block block) {
        Object machine = block.getMetadata("machine").get(0).value();
        if (block.hasMetadata("machine") && machine instanceof Machine) {
            return (Machine) machine;
        }
        return null;
    }

    /**
     * Gets the machine's location
     *
     * @return the location the machine is at
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Gets the block the machine is at
     *
     * @return the machines block
     */
    public Block getBlock() {
        return location.getBlock();
    }

    /**
     * Gets the world the machine is located in
     *
     * @return the machine's world
     */
    public World getWorld() {
        return location.getWorld();
    }

    /**
     * This method is called whenever a machine block is broken
     *
     * @param block block being broken
     */
    public void onMachinePieceBreak(Block block) {
    }

    /**
     * This method is called whenever a machine is created
     */
    public void onCreate() {
    }

    /**
     * This method is called whenever a machine is loaded
     */
    public void onEnable() {
    }

    /**
     * This method is called whenever a machine is destroyed
     */
    public void onDestroy() {
    }

    /**
     * This method is called to perform the machines task
     */
    public abstract void runMachine();

    /**
     * Gets all the items that will drop when the machine is destroyed
     *
     * @return Items to drop when the machine is destroyed
     */
    public abstract List<ItemStack> getDrops();

    /**
     * Gets the recipe required in order to make the machine
     *
     * @return the machine's crafting recipe
     */
    public abstract Recipe getRecipe();

    /**
     * Gets the name of the machine as displayed in the database
     *
     * @return name of the machine's table in the db
     */
    public abstract String getTableName();

    /**
     * Gets the name of the machine
     * The name of the machine should be a consecutive {@link String}
     *
     * @return the name of the machine
     */
    public abstract String getName();

    /**
     * Destroys the machine when it's block is broken
     *
     * @param event the block break event
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getLocation().equals(location)) {
            this.destroy(true);
        }
    }
}

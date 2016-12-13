package ravioli.gravioli.tekkit.database;

import ravioli.gravioli.tekkit.Tekkit;
import ravioli.gravioli.tekkit.database.utils.DatabaseObject;
import ravioli.gravioli.tekkit.machines.Machine;
import ravioli.gravioli.tekkit.utils.CommonUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.sql.*;

public class Sqlite {
    private Tekkit plugin;
    private Connection connection;

    public Sqlite(Tekkit plugin) {
        this.plugin = plugin;
        initialize();
    }

    private void initialize() {
        plugin.getDataFolder().mkdir();
        connection = createConnection();
    }

    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = createConnection();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public Connection createConnection() {
        Connection connection = null;
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + File.separator + "tekkit.db");
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public void createTable(Machine machine) {
        String sql = "CREATE TABLE IF NOT EXISTS `" + machine.getTableName() + "`(id INTEGER PRIMARY KEY AUTOINCREMENT";
        for (Field field : CommonUtils.getAllFields(machine.getClass())) {
            if (field.getAnnotation(DatabaseObject.class) != null) {
                Class type = field.getType();

                String rowType = "TEXT NOT NULL";
                if (type.isAssignableFrom(Integer.TYPE)) {
                    rowType = "INT NOT NULL";
                } else if (type.isAssignableFrom(Long.TYPE)) {
                    rowType = "INTEGER NOT NULL";
                } else if (type.isAssignableFrom(Boolean.TYPE)) {
                    rowType = "BOOLEAN DEFAULT";
                }
                sql += ", `" + field.getName()  + "` " + rowType;
            }
        }
        sql += ")";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads basic information for a machine
     * The actual loading is done when a world is loaded
     *
     * @param machine the machine type to be loaded
     */
    public void preloadMachines(Machine machine) {
        try (PreparedStatement statement = this.connection.prepareStatement("SELECT * FROM `" + machine.getTableName() + "`")) {
            ResultSet results = statement.executeQuery();
            while (results.next()) {
                machine.getClass().getConstructor(Tekkit.class).newInstance(plugin).preload(results);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads a single machine
     *
     * @param machine the machine to load
     */
    public void loadMachine(Machine machine) {
        try (PreparedStatement statement = this.connection.prepareStatement("SELECT * FROM `" + machine.getTableName() + "` WHERE `id` = ?")) {
            statement.setInt(1, machine.getId());
            ResultSet results = statement.executeQuery();

            machine.load(results);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void runAsync(Runnable runnable) {
        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, runnable);
    }
}

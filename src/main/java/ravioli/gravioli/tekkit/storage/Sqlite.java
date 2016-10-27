package ravioli.gravioli.tekkit.storage;

import java.io.File;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.bukkit.inventory.Inventory;
import ravioli.gravioli.tekkit.Tekkit;
import ravioli.gravioli.tekkit.machine.Machine;
import ravioli.gravioli.tekkit.machine.MachineBase;
import ravioli.gravioli.tekkit.machine.utilities.Persistent;
import ravioli.gravioli.tekkit.util.CommonUtils;

public class Sqlite {
    private Tekkit plugin;
    private Connection connection;

    public Sqlite(Tekkit plugin) {
        this.plugin = plugin;
        this.initialize();
    }

    private void initialize() {
        this.plugin.getDataFolder().mkdir();
        this.connection = this.createConnection();
    }

    public Connection getConnection() {
        try {
            if (this.connection == null || this.connection.isClosed()) {
                this.connection = this.createConnection();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return this.connection;
    }

    public Connection createConnection() {
        Connection connection = null;
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + this.plugin.getDataFolder().getAbsolutePath() + File.separator + "tekkit.db");
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public void createTable(Machine machine) {
        String sql = "CREATE TABLE IF NOT EXISTS `" + machine.getName() + "`(id INTEGER PRIMARY KEY AUTOINCREMENT";
        for (Field field : CommonUtils.getAllFields(machine.getClass())) {
            if (field.getAnnotation(Persistent.class) != null) {
                Class type = field.getType();
                String name = field.getName();

                if (type.isAssignableFrom(Integer.TYPE)) {
                    sql = sql + ", `" + name + "` INT NOT NULL";
                } else if (type.isAssignableFrom(Long.TYPE)) {
                    sql = sql + ", `" + name + "` INTEGER NOT NULL";
                } else if (type.isAssignableFrom(String.class) ||
                        type.isAssignableFrom(org.bukkit.Location.class) ||
                        type.isAssignableFrom(Inventory.class)) {
                    sql = sql + ", `" + name + "` TEXT NOT NULL";
                } else if (type.isAssignableFrom(Boolean.TYPE)) {
                    sql = sql + ", `" + name + "` BOOLEAN DEFAULT 0";
                } else {
                    sql = sql + ", `" + name + "` TEXT NOT NULL";
                }
            }
        }
        sql = sql + ")";
        try (PreparedStatement statement = this.connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void loadMachines(MachineBase machine) {
        try (PreparedStatement statement = this.connection.prepareStatement("SELECT * FROM `" + machine.getName() + "`")) {
            ResultSet results = statement.executeQuery();
            while (results.next()) {
                machine.getClass().newInstance().load(results);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void runAsync(Runnable runnable) {
        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, runnable);
    }
}
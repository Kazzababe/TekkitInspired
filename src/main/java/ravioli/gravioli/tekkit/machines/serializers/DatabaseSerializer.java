package ravioli.gravioli.tekkit.machines.serializers;

/**
 * This class is intended to be used for pre-existing classes in bukkit
 * If you're using your own classes, it's recommended to use {@link ravioli.gravioli.tekkit.database.utils.DatabaseSerializable}
 */
public abstract class DatabaseSerializer<T> {
    public abstract String serialize(T object);
    public abstract T deserialize(String object);
}

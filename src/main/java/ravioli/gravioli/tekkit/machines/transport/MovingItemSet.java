package ravioli.gravioli.tekkit.machines.transport;

import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import ravioli.gravioli.tekkit.database.utils.DatabaseSerializable;

import java.util.*;

public class MovingItemSet extends AbstractSet<MovingItem> implements DatabaseSerializable {
    private Set<MovingItem> items = Sets.newConcurrentHashSet();

    @Override
    public Iterator<MovingItem> iterator() {
        return items.iterator();
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean add(MovingItem item) {
        return items.add(item);
    }

    @Override
    public boolean remove(Object object) {
        return items.remove(object);
    }

    @Override
    public String serialize() {
        String[] data = new String[size()];

        int index = 0;
        Iterator<MovingItem> iterator = iterator();
        while (iterator.hasNext()) {
            MovingItem item = iterator.next();
            data[index] = item.serialize();
            index++;
        }
        return StringUtils.join(data, "\\|");
    }

    public static MovingItemSet deserialize(String object) {
        String[] data = object.split("\\|");

        MovingItemSet set = new MovingItemSet();
        for (int i = 0; i < data.length; i++) {
            set.add(MovingItem.deserialize(data[i]));
        }
        return set;
    }
}

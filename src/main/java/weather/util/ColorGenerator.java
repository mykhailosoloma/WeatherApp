package weather.util;

import java.util.Iterator;
import java.util.List;

public class ColorGenerator implements Iterator<String> {
    private final List<String> colors = List.of(
            "#fce4ec", "#e3f2fd", "#fff3e0", "#e8f5e9", "#ede7f6"
    );

    private int index = 0;

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public String next() {
        String color = colors.get(index);
        index = (index + 1) % colors.size();
        return color;
    }
}

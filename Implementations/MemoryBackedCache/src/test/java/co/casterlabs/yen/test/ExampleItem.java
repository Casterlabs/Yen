package co.casterlabs.yen.test;

import co.casterlabs.yen.Cacheable;
import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
public class ExampleItem implements Cacheable {
    public final long value;

    @Override
    public String id() {
        return String.valueOf(this.value);
    }

}

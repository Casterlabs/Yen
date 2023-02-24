package co.casterlabs.yen.test;

import co.casterlabs.rakurai.json.annotating.JsonClass;
import co.casterlabs.yen.Cacheable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@AllArgsConstructor
@JsonClass(exposeAll = true)
public class ExampleItem implements Cacheable {
    private @Getter long value;

    @Override
    public String id() {
        return String.valueOf(this.value);
    }

}

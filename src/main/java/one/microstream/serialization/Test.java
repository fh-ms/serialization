
package one.microstream.serialization;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import one.microstream.persistence.binary.jdk8.types.BinaryHandlersJDK8;
import one.microstream.persistence.binary.types.BinaryPersistence;
import one.microstream.persistence.binary.types.BinaryPersistenceFoundation;


public class Test
{
	public static void main(final String[] args)
	{
		final BinaryPersistenceFoundation<?> foundation = BinaryPersistence.Foundation();
		BinaryHandlersJDK8.registerJDK8TypeHandlers(foundation);
		final Serializer serializer = Serializer.get(foundation);
		
		final Map<String, Object>           map        = Map.of("A", 100, "B", 200.0, "C", "value 3");
		final LinkedHashMap<String, Object> linkedMap  = map.entrySet().stream()
			.sorted(Entry.comparingByKey())
			.collect(toLinkedMap());
		
		final byte[]                        data       = serializer.serialize(linkedMap);
		final LinkedHashMap<String, Object> linkedMap2 = serializer.deserialize(data);
		
		System.out.println(linkedMap.equals(linkedMap2));
	}
	
	
	public static <K, U> Collector<Map.Entry<K, U>, ?, LinkedHashMap<K,U>> toLinkedMap()
	{
		return toLinkedMap(
			e -> e.getKey()  ,
			e -> e.getValue()
		);
	}
	
	
	public static <T, K, U> Collector<T, ?, LinkedHashMap<K,U>> toLinkedMap(
        final Function<? super T, ? extends K> keyMapper,
        final Function<? super T, ? extends U> valueMapper)
    {
        return Collectors.toMap(
            keyMapper  ,
            valueMapper,
            (u, v) -> {
                throw new IllegalStateException(String.format("Duplicate key %s", u));
            },
            LinkedHashMap::new
        );
    }
}

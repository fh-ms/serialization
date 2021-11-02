
package one.microstream.serialization;

import java.time.LocalDate;


public class Test
{
	public static void main(final String[] args)
	{
		final Serializer serializer = Serializer.get();
		
		final Person     p1         = new Person("John Doe", LocalDate.of(1980, 12, 24));
		final byte[]     data       = serializer.serialize(p1);
		final Person     p2         = serializer.deserialize(data);
		
		System.out.println(p1.equals(p2));
	}
}

package one.microstream.serialization;

import java.net.InetSocketAddress;
import java.time.LocalDate;

import one.microstream.communication.binary.types.ComBinary;
import one.microstream.communication.types.ComChannel;
import one.microstream.communication.types.ComClient;

public class Client
{
	public static void main(final String[] args)
	{
		final ComClient<?> client = ComBinary.Foundation()
			.setClientTargetAddress(new InetSocketAddress("localhost", 1337))
			.createClient();

		final ComChannel channel = client.connect();
		System.out.println(
			"Host reply: " + channel.request(new Person("John Doe", LocalDate.of(1990, 1, 15)))
		);

	}
}

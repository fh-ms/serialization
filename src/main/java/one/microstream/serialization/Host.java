package one.microstream.serialization;

import java.net.InetSocketAddress;

import one.microstream.communication.binary.types.ComBinary;
import one.microstream.communication.types.ComHost;

public class Host
{
	public static void main(final String[] args)
	{
		final ComHost<?> host = ComBinary.Foundation()
			.setHostBindingAddress(new InetSocketAddress("localhost", 1337))
			.setHostChannelAcceptor(hostChannel -> {
				// sessionless / stateless greeting service
				final Person person = (Person)hostChannel.receive();
				hostChannel.send("Welcome, " + person.getName());
				hostChannel.close();
			})
			.registerEntityTypes(Person.class)
			.createHost();
		host.run();
	}
}

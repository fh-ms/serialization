
package one.microstream.serialization;

import java.time.LocalDate;
import java.util.Objects;


public class Person
{
	private final String    name;
	private final LocalDate dateOfBirth;
	
	public Person(final String name, final LocalDate dateOfBirth)
	{
		super();
		this.name        = name;
		this.dateOfBirth = dateOfBirth;
	}

	public String getName()
	{
		return this.name;
	}

	public LocalDate getDateOfBirth()
	{
		return this.dateOfBirth;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(this.name, this.dateOfBirth);
	}

	@Override
	public boolean equals(final Object obj)
	{
		if(this == obj)
		{
			return true;
		}
		if(!(obj instanceof Person))
		{
			return false;
		}
		final Person other = (Person)obj;
		return Objects.equals(this.name, other.name) && Objects.equals(this.dateOfBirth, other.dateOfBirth);
	}
		
}

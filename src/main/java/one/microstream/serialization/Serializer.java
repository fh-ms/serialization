
package one.microstream.serialization;

/*-
 * #%L
 * microstream-cache
 * %%
 * Copyright (C) 2019 - 2021 MicroStream Software
 * %%
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * 
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is
 * available at https://www.gnu.org/software/classpath/license.html.
 * 
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 * #L%
 */

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.WeakHashMap;

import one.microstream.X;
import one.microstream.collections.types.XGettingCollection;
import one.microstream.memory.XMemory;
import one.microstream.persistence.binary.types.Binary;
import one.microstream.persistence.binary.types.BinaryPersistence;
import one.microstream.persistence.binary.types.BinaryPersistenceFoundation;
import one.microstream.persistence.binary.types.ChunksWrapper;
import one.microstream.persistence.exceptions.PersistenceExceptionTransfer;
import one.microstream.persistence.types.PersistenceContextDispatcher;
import one.microstream.persistence.types.PersistenceIdSet;
import one.microstream.persistence.types.PersistenceManager;
import one.microstream.persistence.types.PersistenceSource;
import one.microstream.persistence.types.PersistenceTarget;
import one.microstream.persistence.types.PersistenceTypeDictionaryManager;
import one.microstream.persistence.types.PersistenceTypeHandlerManager;
import one.microstream.storage.types.Database;


public interface Serializer extends Closeable
{
	public byte[] serialize(Object object);
	
	public <T> T deserialize(byte[] data);
	
	@Override
	public void close();
	
	public static Serializer get()
	{
		return get(Thread.currentThread().getContextClassLoader());
	}
	
	public static Serializer get(final Class<?>... whitelist)
	{
		return get(
			Thread.currentThread().getContextClassLoader(),
			BinaryPersistence.Foundation()                ,
			X.List(whitelist)
		);
	}
	
	public static Serializer get(final ClassLoader classLoader)
	{
		return get(
			classLoader                   ,
			BinaryPersistence.Foundation(),
			X.empty()
		);
	}
	
	public static Serializer get(final BinaryPersistenceFoundation<?> foundation)
	{
		return get(
			Thread.currentThread().getContextClassLoader(),
			foundation                                    ,
			X.empty()
		);
	}
	
	public static Serializer get(
		final ClassLoader                    classLoader,
		final BinaryPersistenceFoundation<?> foundation ,
		final Iterable<Class<?>>             whitelist
	)
	{
		return Static.get(classLoader, foundation, whitelist);
	}
	
	public static class Static
	{
		private final static WeakHashMap<ClassLoader, Serializer> cache = new WeakHashMap<>();
				
		static synchronized Serializer get(
			final ClassLoader                    classLoader,
			final BinaryPersistenceFoundation<?> foundation ,
			final Iterable<Class<?>>             whitelist
		)
		{
			return cache.computeIfAbsent(
				classLoader,
				cl -> new Serializer.Default(foundation, whitelist)
			);
		}
		
		private Static()
		{
			throw new Error();
		}
	}
	
	public static class Default implements Serializer
	{
		private final BinaryPersistenceFoundation<?> foundation;
		private final Iterable<Class<?>>             whitelist;
		private PersistenceManager<Binary>           persistenceManager;
		private Binary                               input;
		private Binary                               output;
		
		Default(final BinaryPersistenceFoundation<?> foundation, final Iterable<Class<?>> whitelist)
		{
			super();
			
			this.foundation = foundation;
			this.whitelist  = whitelist ;
		}
		
		@Override
		public synchronized byte[] serialize(final Object object)
		{
			this.lazyInit();
			this.persistenceManager.store(object);
			return this.toBytes(this.output.buffers());
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public synchronized <T> T deserialize(final byte[] data)
		{
			this.lazyInit();
			this.input = this.toBinary(data);
			return (T)this.persistenceManager.get();
		}
		
		@Override
		public synchronized void close()
		{
			if(this.persistenceManager != null)
			{
				this.persistenceManager.objectRegistry().truncateAll();
				this.persistenceManager.close();
				this.persistenceManager = null;
				this.input              = null;
				this.output             = null;
			}
		}
		
		private void lazyInit()
		{
			if(this.persistenceManager == null)
			{
				final PersistenceSourceBinary source = ()   -> X.Constant(this.input);
				final PersistenceTargetBinary target = data -> this.output = data;
				
				final BinaryPersistenceFoundation<?> foundation = this.foundation
					.setPersister(Database.New(Serializer.class.getName()))
					.setPersistenceSource(source)
					.setPersistenceTarget(target)
					.setContextDispatcher(
						PersistenceContextDispatcher.LocalObjectRegistration()
					);
				
				foundation.setTypeDictionaryManager(
					PersistenceTypeDictionaryManager.Transient(
						foundation.getTypeDictionaryCreator()
					)
				);
				
				final PersistenceTypeHandlerManager<Binary> typeHandlerManager = foundation.getTypeHandlerManager();
				typeHandlerManager.initialize();
				this.whitelist.forEach(typeHandlerManager::ensureTypeHandler);
				
				this.persistenceManager = foundation.createPersistenceManager();
			}
			else
			{
				this.persistenceManager.objectRegistry().truncateAll();
			}
		}
		
		private byte[] toBytes(final ByteBuffer[] buffers)
		{
			int size = 0;
			for(final ByteBuffer buffer : buffers)
			{
				size += buffer.remaining();
			}
			final byte[] bytes = new byte[size];
			int pos = 0;
			for(final ByteBuffer buffer : buffers)
			{
				final int bufferSize = buffer.remaining();
				buffer.get(bytes, pos, bufferSize);
				pos += bufferSize;
			}
			return bytes;
		}
		
		private Binary toBinary(final byte[] data)
		{
			final ByteBuffer buffer = XMemory.allocateDirectNative(data.length);
			buffer.put(data);
			return ChunksWrapper.New(buffer);
		}
		
		
		static interface PersistenceSourceBinary extends PersistenceSource<Binary>
		{
			@Override
			default XGettingCollection<? extends Binary> readByObjectIds(final PersistenceIdSet[] oids)
				throws PersistenceExceptionTransfer
			{
				return null;
			}
		}
		
		
		static interface PersistenceTargetBinary extends PersistenceTarget<Binary>
		{
			@Override
			default boolean isWritable()
			{
				return true;
			}
		}
		
	}
	
}

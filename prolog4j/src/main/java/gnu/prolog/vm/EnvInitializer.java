/* GNU Prolog for Java
 * Copyright (C) 1997-1999  Constantine Plotnikov
 * Copyright (C) 2009       Michiel Hendriks
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA. The text of license can be also found 
 * at http://www.gnu.org/copyleft/lgpl.html
 */
package gnu.prolog.vm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Environment Initializers can be used to initialize the prolog environment
 * with additional builtin predicates. Create a subclass of this class and
 * create a file <code>META-INF/services/gnu.prolog.vm.envinitializer</code>
 * containing the fully qualified classname of your class.
 * 
 * @author Michiel Hendriks
 */
public abstract class EnvInitializer
{
	private static final Logger LOGGER = Logger.getLogger(EnvInitializer.class.getName());

	/**
	 * Run all initializers
	 *
	 * @param environment
	 */
	public static void runInitializers(final Environment environment)
	{
		try
		{
			final var urls = EnvInitializer.class.getClassLoader()
				.getResources("META-INF/services/gnu.prolog.vm.envinitializer");
			while (urls.hasMoreElements())
			{
				try (final var reader = new BufferedReader(
						new InputStreamReader(urls.nextElement().openStream(),
							StandardCharsets.UTF_8)))
				{
					String line;
					while ((line = reader.readLine()) != null)
					{
						line = line.trim();
						if (line.isEmpty() || line.startsWith("#"))
						{
							continue;
						}
						loadInitializer(line, environment);
					}
				}
			}
		}
		catch (IOException ex)
		{
			LOGGER.log(Level.WARNING, "Failed to load initializer resources", ex);
		}
	}

	private static void loadInitializer(final String name, final Environment environment)
	{
		try
		{
			final var cls = Class.forName(name);
			if (EnvInitializer.class.isAssignableFrom(cls))
			{
				final var init = cls.asSubclass(EnvInitializer.class)
					.getDeclaredConstructor().newInstance();
				init.initialize(environment);
			}
		}
		catch (ReflectiveOperationException ex)
		{
			LOGGER.log(Level.WARNING, "Failed to load initializer: " + name, ex);
		}
	}

	public EnvInitializer()
	{}

	public abstract void initialize(Environment environment);
}

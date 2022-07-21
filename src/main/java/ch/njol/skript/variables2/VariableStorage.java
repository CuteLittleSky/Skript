package ch.njol.skript.variables2;

import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.config.SectionNode;
import ch.njol.util.Closeable;

/**
 * Variable storage. Implementations of this class will handle
 * getting, setting and saving of variables.
 */
public abstract class VariableStorage implements Closeable {

	protected StorageConfiguration configuration;

	/**
	 * Used internally by Skript to modify the configuration field.
	 */
	final void reloadConfiguration(StorageConfiguration configuration) {
		loadConfiguration(configuration);
		this.configuration = configuration;
	}

	/**
	 * Skript supports reloading of storage configurations so thus any storage implementation must abide by configuration changes.
	 * The StorageConfiguration field is updated after this method to allow for comparing changes. The argument provided is the new updated configuration.
	 * 
	 * @param configuration The new StorageConfiguration
	 */
	abstract void loadConfiguration(StorageConfiguration configuration);

	/**
	 * If this stroage requires a file.
	 * 
	 * @return boolean if this storage requires a file.
	 */
	abstract boolean requiresFile();

	/**
	 * Return the StorageConfiguration for this storage which contains the user's values from the config.sk
	 * 
	 * @return A NonNull StorageConfiguration for this storage.
	 */
	public StorageConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * Called after creation of the class.
	 * 
	 * @return boolean true if the initialize was successful.
	 */
	public abstract boolean initialize();

	/**
	 * (Re)connects to the database (not called on the first connect - do this in {@link #load_i(SectionNode)}).
	 * 
	 * @return Whether the connection could be re-established. An error should be printed by this method prior to returning false.
	 */
	protected abstract boolean connect();

	/**
	 * Disconnects from the database.
	 */
	protected abstract void disconnect();

	/**
	 * Gets a variable with given name.
	 * @param name Name of variable.
	 * @param event Event associated with the variable.
	 * @param local If it is a local variable or not.
	 * @return Variable or null if not found.
	 */
	@Nullable
	public abstract Object getVariable(String name, @Nullable Event event, boolean local);

	/**
	 * Sets a variable with given name.
	 * @param name Name of variable.
	 * @param event Event associated with the variable.
	 * @param local If it is a local variable or not.
	 * @param value New value. Can be null to remove the variable.
	 */
	public abstract void setVariable(String name, @Nullable Event event, boolean local, @Nullable Object value);

	/**
	 * Flushes all variables to storage from memory. This should be called on
	 * server shutdown, but rarely else.
	 */
	public abstract void flush();

}

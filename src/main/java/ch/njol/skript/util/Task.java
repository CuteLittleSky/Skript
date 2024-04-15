/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright Peter Güttinger, SkriptLang team and contributors
 */
package ch.njol.skript.util;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.eclipse.jdt.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

import ch.njol.skript.Skript;
import ch.njol.util.Closeable;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask.ExecutionState;

public abstract class Task implements Runnable, Closeable {
	
	private final Plugin plugin;
	private final boolean async;
	private long period = -1;
	
	private @NotNull ScheduledTask taskID;
	
	public Task(Plugin plugin, long delay, long period) {
		this(plugin, delay, period, false);
	}
	
	public Task(Plugin plugin, long delay, long period, boolean async) {
		this.plugin = plugin;
		this.period = period;
		this.async = async;
		schedule(delay);
	}
	
	public Task(Plugin plugin, long delay) {
		this(plugin, delay, false);
	}
	
	public Task(Plugin plugin, long delay, boolean async) {
		this.plugin = plugin;
		this.async = async;
		schedule(delay);
	}
	
	/**
	 * Only call this if the task is not alive.
	 * 
	 * @param delay
	 */
	private void schedule(long delay) {
		assert !isAlive();
		if (!Skript.getInstance().isEnabled())
			return;
		
		if (period == -1) {
			if (async) {
				if (delay <= 0) {
					taskID = Bukkit.getAsyncScheduler().runNow(plugin, task -> this.run());
				} else {
					taskID = Bukkit.getAsyncScheduler().runDelayed(plugin, task -> this.run(), (delay / 20) * 1000, TimeUnit.MILLISECONDS);
				}
			} else {
				if (delay <= 0) {
					taskID = Bukkit.getGlobalRegionScheduler().run(plugin, task -> this.run());
				} else {
					taskID = Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> this.run(), delay);
				}
			}
		} else {
			if (async) {
				taskID = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> this.run(), (delay / 20) * 1000, (period / 20) * 1000, TimeUnit.MILLISECONDS);
			} else {
				taskID = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> this.run(), delay, period);
			}
		}
		assert taskID != null;
	}

	/**
	 * @return Whether this task is still running, i.e. whether it will run later or is currently running.
	 */
	public final boolean isAlive() {
		if (taskID == null)
			return false;
		return taskID.getExecutionState() == ExecutionState.RUNNING;
	}

	/**
	 * Cancels this task.
	 */
	public final void cancel() {
		if (taskID != null) {
			taskID.cancel();
			taskID = null;
		}
	}
	
	@Override
	public void close() {
		cancel();
	}
	
	/**
	 * Re-schedules the task to run next after the given delay. If this task was repeating it will continue so using the same period as before.
	 * 
	 * @param delay
	 */
	public void setNextExecution(final long delay) {
		assert delay >= 0;
		cancel();
		schedule(delay);
	}
	
	/**
	 * Sets the period of this task. This will re-schedule the task to be run next after the given period if the task is still running.
	 * 
	 * @param period Period in ticks or -1 to cancel the task and make it non-repeating
	 */
	public void setPeriod(final long period) {
		assert period == -1 || period > 0;
		if (period == this.period)
			return;
		this.period = period;
		if (isAlive()) {
			cancel();
			if (period != -1)
				schedule(period);
		}
	}
	
}

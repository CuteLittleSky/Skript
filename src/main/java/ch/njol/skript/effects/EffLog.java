/*
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
 * 
 * Copyright 2011-2013 Peter Güttinger
 * 
 */

package ch.njol.skript.effects;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.ExceptionUtils;
import ch.njol.util.Kleenean;

/**
 * @author Peter Güttinger
 */
@SuppressWarnings("serial")
@Name("Log")
@Description({"Writes text into a .log file. Skript will write these files to /plugins/Skript/logs.",
		"NB: Using 'server.log' as the log file will write to the default server log."})
@Examples({"on place of TNT:",
		"	log \"%player% placed TNT in %world% at %location of block%\" to \"tnt/placement\""})
@Since("2.0")
public class EffLog extends Effect {
	static {
		Skript.registerEffect(EffLog.class, "log %strings% [(to|in) [file[s]] %-strings%]");
	}
	
	private final static File logsFolder = new File(Skript.getInstance().getDataFolder(), "logs");
	
	private final static HashMap<String, PrintWriter> writers = new HashMap<String, PrintWriter>();
	static {
		Skript.closeOnDisable(new Closeable() {
			@Override
			public void close() throws IOException {
				for (final PrintWriter pw : writers.values())
					pw.close();
			}
		});
	}
	
	private Expression<String> messages;
	private Expression<String> files;
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean init(final Expression<?>[] exprs, final int matchedPattern, final Kleenean isDelayed, final ParseResult parser) {
		messages = (Expression<String>) exprs[0];
		files = (Expression<String>) exprs[1];
		return true;
	}
	
	@Override
	protected void execute(final Event e) {
		for (final String message : messages.getArray(e)) {
			if (files == null) {
				Skript.info("[" + getTrigger().getScript().getName() + "] " + message);
			} else {
				for (String s : files.getArray(e)) {
					s = s.toLowerCase();
					if (!s.endsWith(".log"))
						s += ".log";
					if (s.equals("server.log")) {
						Bukkit.getLogger().log(Level.INFO, message);
						continue;
					}
					@SuppressWarnings("resource")
					PrintWriter w = writers.get(s);
					if (w == null) {
						final File f = new File(logsFolder, s);
						try {
							f.getParentFile().mkdirs();
							w = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
							writers.put(s, w);
						} catch (final IOException ex) {
							Skript.error("Cannot write to log file '" + s + "' (" + f.getPath() + "): " + ExceptionUtils.toString(ex));
							return;
						}
					}
					w.println("[" + SkriptConfig.formatDate(System.currentTimeMillis()) + "] " + message);
					w.flush();
				}
			}
		}
	}
	
	@Override
	public String toString(final Event e, final boolean debug) {
		return "log " + messages.toString(e, debug) + " to " + files.toString(e, debug);
	}
}

package steve6472.brigit;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**********************
 * Created by steve6472
 * On date: 30.08.2023
 * Project: Brigit
 *
 ***********************/
public final class Brigit extends JavaPlugin implements Listener
{
	private static Brigit INSTANCE;

	private List<BrigitCommand> commands;

	public Brigit() {}

	private static Brigit getInstance()
	{
		return INSTANCE;
	}

	@Override
	public void onEnable()
	{
		INSTANCE = this;

		Bukkit.getPluginManager().registerEvents(INSTANCE, this);
		commands = new ArrayList<>();
	}

	private CommandDispatcher<CommandSourceStack> getVanillaDispatcher()
	{
		return getVanillaServer().vanillaCommandDispatcher.getDispatcher();
	}

	public static void addBrigitCommand(BrigitCommand command)
	{
		/*
		 * Remove old version of command
		 * (Upon reload command would be duplicated)
		 */
		getInstance().getVanillaDispatcher().getRoot().removeCommand(command.getName());

		/* Registers new command */
		command.register(getInstance().getVanillaDispatcher());

		/* Add command to list so we can ignore bukkit permissions */
		getInstance().commands.add(command);
	}

	/*
	 * Bukkit permissions fix
	 */

	/**
	 * Called upon server reload
	 * Reloads permissions for all online Players
	 *
	 * @param e ServerLoadEvent
	 */
	@EventHandler
	public void reload(ServerLoadEvent e)
	{
		for (Player p : Bukkit.getOnlinePlayers())
		{
			updatePermissions(p);
		}
	}

	/**
	 * Updates permissions for newly connected player
	 *
	 * @param event PlayerJoinEvent
	 */
	@EventHandler
	public void updatePermissions(PlayerJoinEvent event)
	{
		updatePermissions(event.getPlayer());
	}

	/**
	 * Updates permissions for specific player
	 *
	 * @param player Player
	 */
	private void updatePermissions(Player player)
	{
		player.recalculatePermissions();
		for (BrigitCommand comm : commands)
		{
			if (comm.getPermissionLevel() == 0)
			{
				player.addAttachment(getInstance(), "minecraft.command." + comm.getName(), true);
			} else if (player.isOp())
			{
				player.addAttachment(getInstance(), "minecraft.command." + comm.getName(), true);
			}
		}
	}

	/*
	 * Util
	 */

	private MinecraftServer minecraftServer;

	private MinecraftServer getVanillaServer()
	{
		if (minecraftServer != null)
			return minecraftServer;

		try
		{
			// Go around Version-Specific Bukkit
			Server server = Bukkit.getServer();
			Method getServer = server.getClass().getMethod("getServer");
			Object declaredServer = getServer.invoke(server);
			if (declaredServer instanceof MinecraftServer mcServer)
				return minecraftServer = mcServer;

			throw new RuntimeException("Could not get MinecraftServer!");
		} catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e)
		{
			throw new RuntimeException(e);
		}
	}
}

package steve6472.brigit;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**********************
 * Created by steve6472
 * On date: 30.08.2023
 * Project: Brigit
 *
 ***********************/
public final class Brigit extends JavaPlugin implements Listener
{
	private static Brigit INSTANCE;

	private Map<String, List<BrigitCommand>> commands;

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
		commands = new HashMap<>();
	}

	public static void addBrigitCommand(@NotNull Plugin plugin, @NotNull BrigitCommand command)
	{
		getInstance().getLogger().info("Adding command " + command.getName() + " from " + plugin.getName());
		/*
		 * Remove old version of command
		 * (Upon reload command would be duplicated)
		 */
		removeFromServerSafe(command);

		/* Registers new command */
		command.register(getInstance().getVanillaDispatcher());

		/* Add command to list so we can ignore bukkit permissions */
		List<BrigitCommand> brigitCommands = getInstance().commands.computeIfAbsent(plugin.getName(), (pl) -> new ArrayList<>());
		brigitCommands.removeIf(c -> c.getName().equals(command.getName()));
		brigitCommands.add(command);

		registerToCommandMapSafe(command);
	}

	public static void removeCommands(@NotNull Plugin plugin)
	{
		getInstance().getLogger().info("Removing commands from plugin " + plugin.getName());

		List<BrigitCommand> commandList = getInstance().commands.get(plugin.getName());
		if (commandList == null)
			return;

		commandList.forEach(Brigit::removeFromServerSafe);

		getInstance().commands.remove(plugin.getName());
	}

	public static void removeCommand(@NotNull Plugin plugin, @NotNull BrigitCommand command)
	{
		getInstance().getLogger().info("Removing command %s from %s".formatted(command.getName(), plugin.getName()));

		List<BrigitCommand> commandList = getInstance().commands.get(plugin.getName());
		if (commandList == null)
			return;

		new ArrayList<>(commandList).forEach(c ->
		{
			if (c.getName().equals(command.getName()))
			{
				removeFromServerSafe(command);
				commandList.remove(command);
			}
		});

		if (commandList.isEmpty())
			getInstance().commands.remove(plugin.getName());
	}

//	public static void updateAllCommands()
//	{
//		for (Player player : Bukkit.getOnlinePlayers())
//		{
//			player.updateCommands();
//		}
//	}

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
		commands.values().forEach(cmdsList -> cmdsList.forEach(comm ->
		{
			if (comm.getPermissionLevel() == 0)
			{
				player.addAttachment(getInstance(), "minecraft.command." + comm.getName(), true);
			} else if (player.isOp())
			{
				player.addAttachment(getInstance(), "minecraft.command." + comm.getName(), true);
			}
		}));
	}

	/*
	 * Util
	 */

	private static void removeFromServerSafe(@NotNull BrigitCommand command)
	{
		try
		{
			removeFromServer(command);
		} catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e)
		{
			throw new RuntimeException(e);
		}
	}

	private static void registerToCommandMapSafe(@NotNull BrigitCommand command)
	{
		try
		{
			registerToCommandMap(command);
		} catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException |
		         IllegalAccessException e)
		{
			throw new RuntimeException(e);
		}
	}

	private static void removeFromServer(@NotNull BrigitCommand command) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException
	{
		getInstance().getVanillaDispatcher().getRoot().removeCommand(command.getName());

		Object commandMap = getCommandMap();
		Class<?> commandMapClass = commandMap.getClass();
		Method getKnownCommands = commandMapClass.getMethod("getKnownCommands");
		Object knownCommands = getKnownCommands.invoke(commandMap);
		Method remove = knownCommands.getClass().getMethod("remove", Object.class);

		remove.invoke(knownCommands, command.getName());
		remove.invoke(knownCommands, "minecraft:" + command.getName());
	}

	private static void registerToCommandMap(@NotNull BrigitCommand command)
		throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException
	{
		CommandNode<CommandSourceStack> child = getInstance()
			.getVanillaDispatcher()
			.getRoot()
			.getChild(command.getName());

		String[] split = Bukkit.getServer().getClass().getPackage().getName().split("\\.");
		String version = split[split.length - 1];

		/* Construct VanillaCommandWrapper */
		Class<?> vanillaCommandWrapperClass = Class.forName("org.bukkit.craftbukkit." + version + ".command.VanillaCommandWrapper");
		Constructor<?> vanillaCommandWrapperConstructor = vanillaCommandWrapperClass.getConstructor(Commands.class, CommandNode.class);
		Object vanillaCommandWrapper = vanillaCommandWrapperConstructor.newInstance(getInstance().getVanillaCommands(), child);

		/* Register VanillaCommandWrapper to CommandMap */
		Object commandMap = getCommandMap();
		Class<?> commandMapClass = commandMap.getClass();
		Method register = commandMapClass.getMethod("register", String.class, Command.class);
		//noinspection JavaReflectionInvocation
		register.invoke(commandMap, "minecraft", vanillaCommandWrapper);
	}

	private static Object getCommandMap() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
	{
		Server server = getInstance().getServer();
		Method getCommandMap = server.getClass().getMethod("getCommandMap");
		return getCommandMap.invoke(server);
	}

	private CommandDispatcher<CommandSourceStack> getVanillaDispatcher()
	{
		return getVanillaServer().vanillaCommandDispatcher.getDispatcher();
	}

	private Commands getVanillaCommands()
	{
		return getVanillaServer().vanillaCommandDispatcher;
	}

	private MinecraftServer minecraftServer;

	private MinecraftServer getVanillaServer()
	{
		if (minecraftServer != null)
			return minecraftServer;

		try
		{
			// Go around Version-Specific Bukkit
			Server server = getInstance().getServer();
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

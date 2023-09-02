package steve6472.brigit;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.SlotArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**********************
 * Created by steve6472
 * On date: 30.08.2023
 * Project: Brigit
 *
 ***********************/
public abstract class BrigitCommand
{
	public BrigitCommand()
	{
		if (getName() == null || getName().isBlank())
			throw new NullPointerException("Command name not specified and will not be registered");
	}

	public abstract void register(CommandDispatcher<CommandSourceStack> dispatcher);

	public abstract String getName();

	public abstract int getPermissionLevel();

	/*
	 * Quick argument constructors
	 */

	protected LiteralArgumentBuilder<CommandSourceStack> literal(String s)
	{
		return LiteralArgumentBuilder.literal(s);
	}

	protected <T> RequiredArgumentBuilder<CommandSourceStack, T> argument(String name, ArgumentType<T> type)
	{
		return RequiredArgumentBuilder.argument(name, type);
	}

	protected CompletableFuture<Suggestions> suggest(Iterable<String> source, SuggestionsBuilder builder)
	{
		String s = builder.getRemaining().toLowerCase(Locale.ROOT);

		for (String s1 : source)
		{
			if (s1.toLowerCase(Locale.ROOT).startsWith(s))
			{
				builder.suggest(s1);
			}
		}

		return builder.buildFuture();
	}

	/* Integer Argument */

	protected IntegerArgumentType integer()
	{
		return IntegerArgumentType.integer();
	}

	protected IntegerArgumentType integer(int min)
	{
		return IntegerArgumentType.integer(min);
	}

	protected IntegerArgumentType integer(int min, int max)
	{
		return IntegerArgumentType.integer(min, max);
	}

	protected int getInteger(CommandContext<CommandSourceStack> context, String name)
	{
		return IntegerArgumentType.getInteger(context, name);
	}

	/* Double Argument */

	protected DoubleArgumentType doubleArg()
	{
		return DoubleArgumentType.doubleArg();
	}

	protected DoubleArgumentType doubleArg(int min)
	{
		return DoubleArgumentType.doubleArg(min);
	}

	protected DoubleArgumentType doubleArg(int min, int max)
	{
		return DoubleArgumentType.doubleArg(min, max);
	}

	protected double getDouble(CommandContext<CommandSourceStack> context, String name)
	{
		return DoubleArgumentType.getDouble(context, name);
	}

	/* Long Argument */

	protected LongArgumentType longArg()
	{
		return LongArgumentType.longArg();
	}

	protected LongArgumentType longArg(long min)
	{
		return LongArgumentType.longArg(min);
	}

	protected LongArgumentType longArg(long min, long max)
	{
		return LongArgumentType.longArg(min, max);
	}

	protected long getLong(CommandContext<CommandSourceStack> context, String name)
	{
		return LongArgumentType.getLong(context, name);
	}

	/* String Argument */

	protected StringArgumentType string()
	{
		return StringArgumentType.string();
	}

	protected String getString(CommandContext<CommandSourceStack> context, String name)
	{
		return StringArgumentType.getString(context, name);
	}

	/* Bool Argument */

	protected BoolArgumentType bool()
	{
		return BoolArgumentType.bool();
	}

	protected boolean getBool(CommandContext<CommandSourceStack> context, String name)
	{
		return BoolArgumentType.getBool(context, name);
	}

	/* Entity Argument */

	protected EntityArgument multipleEntities()
	{
		return EntityArgument.entities();
	}

	protected EntityArgument singleEntity()
	{
		return EntityArgument.entity();
	}

	protected EntityArgument singlePlayer()
	{
		return EntityArgument.player();
	}

	protected EntityArgument multiplePlayers()
	{
		return EntityArgument.players();
	}

	protected Collection<? extends Entity> getEntities(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException
	{
		return EntityArgument.getEntities(context, name).stream().map(net.minecraft.world.entity.Entity::getBukkitEntity).collect(Collectors.toList());
	}

	protected Entity getSingleEntity(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException
	{
		return EntityArgument.getEntity(context, name).getBukkitEntity();
	}

	protected Player getSinglePlayer(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException
	{
		return EntityArgument.getPlayer(context, name).getBukkitEntity();
	}

	protected Collection<Player> getPlayers(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException
	{
		return EntityArgument.getPlayers(context, name).stream().map(ServerPlayer::getBukkitEntity).collect(Collectors.toList());
	}

	/* Enchantment Argument */

	protected ResourceArgument<net.minecraft.world.item.enchantment.Enchantment> itemEnchantment(CommandBuildContext commandBuildContext)
	{
		return ResourceArgument.resource(commandBuildContext, Registries.ENCHANTMENT);
	}

	protected Enchantment getEnchantment(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException
	{
		//noinspection deprecation
		return Enchantment.getByKey(new NamespacedKey("minecraft", ResourceArgument.getEnchantment(context, name).key().location().getPath()));
	}

	/* Effect Argument */

	protected ResourceArgument<MobEffect> potionEffect(CommandBuildContext commandBuildContext)
	{
		return ResourceArgument.resource(commandBuildContext, Registries.MOB_EFFECT);
	}

	protected PotionEffectType getPotionEffect(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException
	{
		//noinspection deprecation
		return PotionEffectType.getByKey(new NamespacedKey("minecraft", ResourceArgument.getMobEffect(context, name).key().location().getPath()));
	}

	/* Entity Type Argument */

	protected ResourceArgument<EntityType<?>> entityType(CommandBuildContext commandBuildContext)
	{
		return ResourceArgument.resource(commandBuildContext, Registries.ENTITY_TYPE);
	}

	protected org.bukkit.entity.EntityType getEntityType(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException
	{
		//noinspection deprecation
		return getBukkitEntity(new NamespacedKey("minecraft", ResourceArgument.getEntityType(context, name).key().location().getPath()));
	}

	private static org.bukkit.entity.EntityType[] ENTITY_TYPES;

	private org.bukkit.entity.EntityType getBukkitEntity(NamespacedKey key)
	{
		if (ENTITY_TYPES == null)
			ENTITY_TYPES = org.bukkit.entity.EntityType.values();

		for (var entityType : ENTITY_TYPES)
		{
			if (entityType.getKey().equals(key))
				return entityType;
		}

		// Hopefully can never happen
		// Brigadier should not allow this
		return null;
	}

	/* Slot Argument */

	protected SlotArgument itemSlot()
	{
		return SlotArgument.slot();
	}

	protected int getSlot(CommandContext<CommandSourceStack> context, String name)
	{
		return SlotArgument.getSlot(context, name);
	}

	/* Block Pos Argument */

	protected BlockPosArgument blockPos()
	{
		return BlockPosArgument.blockPos();
	}

	protected Location getLoadedBlockPos(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException
	{
		BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, name);
		return new Location(getWorld(context), pos.getX(), pos.getY(), pos.getZ());
	}

	protected Location getBlockPos(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException
	{
		BlockPos pos = BlockPosArgument.getSpawnablePos(context, name);
		return new Location(getWorld(context), pos.getX(), pos.getY(), pos.getZ());
	}

	/* NBT Argument */
	protected NbtPathArgument nbtPath()
	{
		return NbtPathArgument.nbtPath();
	}

	protected NbtPathArgument.NbtPath getNBTPath(CommandContext<CommandSourceStack> context, String name)
	{
		return NbtPathArgument.getPath(context, name);
	}





	/*
	 * Command Source Stack getters
	 */

	protected boolean isPlayer(CommandSourceStack stack)
	{
		return stack.getEntity() instanceof ServerPlayer;
	}

	protected Entity getEntity(CommandSourceStack stack) throws CommandSyntaxException
	{
		var entity = stack.getEntity();
		if (entity == null)
			throw Exceptions.NO_ENTITY.create(stack);
		return entity.getBukkitEntity();
	}

	protected Player getPlayer(CommandSourceStack stack) throws CommandSyntaxException
	{
		if (!isPlayer(stack))
			throw Exceptions.NOT_A_PLAYER.create(stack);
		else
			return (Player) stack.getBukkitSender();
	}

	protected World getWorld(CommandSourceStack stack)
	{
		ServerLevel level = stack.getLevel();
		return level.getWorld();
	}

	/*
	 * Context getters
	 */

	protected boolean isPlayer(CommandContext<CommandSourceStack> context)
	{
		return isPlayer(context.getSource());
	}

	protected Player getPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		return getPlayer(context.getSource());
	}

	protected World getWorld(CommandContext<CommandSourceStack> context)
	{
		return getWorld(context.getSource());
	}
}

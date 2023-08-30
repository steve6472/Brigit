package steve6472.brigit;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

/**********************
 * Created by steve6472
 * On date: 30.08.2023
 * Project: Brigit
 *
 ***********************/
class Exceptions
{
	public static final DynamicCommandExceptionType NOT_A_PLAYER = new DynamicCommandExceptionType(c -> new LiteralMessage("Entity is not a Player"));
	public static final DynamicCommandExceptionType NO_ENTITY = new DynamicCommandExceptionType(c -> new LiteralMessage("No entity selected"));
}

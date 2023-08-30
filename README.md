# Brigit
 
```java
Brigit.addBrigitCommand(new BrigitCommand()
{
	@Override
	public void register(CommandDispatcher<CommandSourceStack> commandDispatcher)
	{
		commandDispatcher.register(literal(getName()).executes(c -> {
			getPlayer(c).sendMessage("Briggit");
			return 1;
		}));
	}
	@Override
	public String getName()
	{
		return "brigit";
	}
	@Override
	public int getPermissionLevel()
	{
		return 0;
	}
});
```

package com.rogermiranda1000.mineit.mineable_gems;

/**
 * We need to check if ConfigurationSection contains the mine argument, and change the return element if needed.
 * @author [Icyene](https://bukkit.org/threads/tutorial-advanced-beyond-reflection-aspectj-tracing.98400/)
 * @author Roger Miranda
 */
public aspect CustomDropAspect {
    pointcut readCustomDropExecuted():
    execution(public * me.Mohamad82.MineableGems.Core.DropReader.readCustomDrop(..));

    after(): readCustomDropExecuted() {
        System.out.printf("Enters on method: %s. \n", thisJoinPoint.getSignature());

        Object[] arguments = thisJoinPoint.getArgs();
        for(int i =0; i < arguments.length; i++){
            Object argument = arguments[i];
            if(argument !=null){
                System.out.printf("With argument of type %s and value %s. \n", argument.getClass().toString(), argument);
            }
        }
        System.out.printf("Exits method: %s. \n", thisJoinPoint.getSignature());
    }
}

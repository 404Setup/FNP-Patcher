package one.pkg.kreno_fpatcher;

import net.fabricmc.api.ModInitializer;
import one.pkg.kreno_fpatcher.util.culling.ServerCullingManager;
import one.pkg.libsl.api.event.entity.ServerPlayerEvents;
import one.pkg.libsl.api.event.lifecycle.ServerLifecycleEvents;

public class ModMain implements ModInitializer {
    private static final String MOD_ID = "kreno_fpatcher";

    @Override
    public void onInitialize() {
        safetyCheck();

        ServerPlayerEvents.LEAVE.register(ServerCullingManager::removePlayer);
        ServerLifecycleEvents.STOPPING.register((_) -> ServerCullingManager.onEnd());
    }

    // This is a deliberate check.
    protected void safetyCheck() {
        try {
            Class.forName("org.bukkit.advancement.Advancement");
            throw new SecurityException("Unsupported mod detected: bukkit");
        } catch (ClassNotFoundException ignored) {
        }
    }
}

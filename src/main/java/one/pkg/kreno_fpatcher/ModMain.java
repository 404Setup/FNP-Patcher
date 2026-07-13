package one.pkg.kreno_fpatcher;

import net.fabricmc.api.ModInitializer;

public class ModMain implements ModInitializer {
    private static final String MOD_ID = "kreno_fpatcher";

    @Override
    public void onInitialize() {
        safetyCheck();
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

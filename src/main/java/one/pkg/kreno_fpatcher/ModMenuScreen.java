package one.pkg.kreno_fpatcher;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import net.minecraft.client.gui.screens.Screen;
import one.pkg.libsl.api.ui.seeui.SeeUIBuilder;

public class ModMenuScreen implements ConfigScreenFactory<Screen> {
    @Override
    public Screen create(Screen screen) {
        return SeeUIBuilder.builder()
                .clazz(ModConfig.class)
                .lastScreen(screen)
                .useOreUI(ModConfig.GUI.isOreUI())
                .onSaved(() -> {
                    try {
                        ModConfig.config.saveAllConfigurations();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .build();
    }
}

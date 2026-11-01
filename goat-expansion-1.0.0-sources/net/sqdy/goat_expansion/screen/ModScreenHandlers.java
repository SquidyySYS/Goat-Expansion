package net.sqdy.goat_expansion.screen;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.sqdy.goat_expansion.GoatExpansion;

public class ModScreenHandlers {

    public static final MenuType<GoatScreenHandler> GOAT_SCREEN_HANDLER = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(GoatExpansion.MOD_ID, "goat_screen_handler"),
            new MenuType<>(GoatScreenHandler::new, FeatureFlags.DEFAULT_FLAGS)
    );

    public static final MenuType<GoatPouchScreenHandler> GOAT_POUCH_SCREEN_HANDLER = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(GoatExpansion.MOD_ID, "goat_pouch_screen_handler"),
            new MenuType<>(GoatPouchScreenHandler::new, FeatureFlags.DEFAULT_FLAGS)
    );

    public static void register() {
    }

    public static void registerScreenHandlers() {
    }
}
package net.sqdy.goat_expansion.item;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.sqdy.goat_expansion.GoatExpansion;

import java.util.function.Function;

public class ModItems {
    public static final int CAP_MAX_DURABILITY = 85;
    public static final int PROSTHETIC_MAX_DURABILITY = 400;

    public static final Item GOAT_SADDLE = registerItem("goat_saddle", Item::new, new Item.Properties().stacksTo(1));
    public static final Item GOAT_POUCH = registerItem("goat_pouch", GoatPouchItem::new, new Item.Properties().stacksTo(1));
    public static final Item IRON_HORN_CAP = registerItem("iron_horn_cap", Item::new, new Item.Properties().stacksTo(1).durability(CAP_MAX_DURABILITY));
    public static final Item IRON_PROSTHETIC_HORNS = registerItem("iron_prosthetic_horns", Item::new, new Item.Properties().stacksTo(1).durability(PROSTHETIC_MAX_DURABILITY));
    public static final Item POTION_GOAT_HORN = registerItem("potion_goat_horn", PotionGoatHornItem::new, new Item.Properties().stacksTo(16));

    public static final ResourceKey<CreativeModeTab> GOAT_TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.tryParse(GoatExpansion.MOD_ID + ":goat_expansion_tab")
    );

    public static final CreativeModeTab GOAT_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            GOAT_TAB_KEY,
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 7)
                    .title(Component.literal("Goat Expansion"))
                    .icon(() -> new ItemStack(GOAT_SADDLE))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(GOAT_SADDLE);
                        output.accept(GOAT_POUCH);
                        output.accept(IRON_HORN_CAP);
                        output.accept(IRON_PROSTHETIC_HORNS);
                        output.accept(POTION_GOAT_HORN);
                    })
                    .build()
    );

    private static Item registerItem(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        Identifier id = Identifier.tryParse(GoatExpansion.MOD_ID + ":" + name);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        Item item = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void registerModItems() {
        GoatExpansion.LOGGER.info("Goat Expansion: Items and Creative Tab registered!");
    }
}
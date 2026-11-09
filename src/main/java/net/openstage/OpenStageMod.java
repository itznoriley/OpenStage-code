package net.openstage;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.openstage.block.ModelBlock;
import net.openstage.block.entity.ModelBlockEntity;
import net.openstage.core.scene.ActorEntity;
import net.openstage.core.scene.SceneManager;
import net.openstage.network.OpenStageNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenStageMod implements ModInitializer {
    public static final String MOD_ID = "openstage";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final ResourceLocation ACTOR_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "actor");

    public static final EntityType<ActorEntity> ACTOR_ENTITY_TYPE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ACTOR_ID,
            EntityType.Builder.<ActorEntity>of(ActorEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(ACTOR_ID.toString())
    );

    public static final ResourceLocation MODEL_BLOCK_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "model_block");

    public static final Block MODEL_BLOCK = Registry.register(
            BuiltInRegistries.BLOCK,
            MODEL_BLOCK_ID,
            new ModelBlock(BlockBehaviour.Properties.of().strength(1.5f).noOcclusion())
    );

    public static final BlockItem MODEL_BLOCK_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            MODEL_BLOCK_ID,
            new BlockItem(MODEL_BLOCK, new Item.Properties())
    );

    public static final BlockEntityType<ModelBlockEntity> MODEL_BLOCK_ENTITY_TYPE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            MODEL_BLOCK_ID,
            BlockEntityType.Builder.of(ModelBlockEntity::new, MODEL_BLOCK).build(null)
    );

    @Override
    public void onInitialize() {
        LOGGER.info("[OpenStage] Initialising OpenStage v1.0-release ...");

        FabricDefaultAttributeRegistry.register(ACTOR_ENTITY_TYPE, ActorEntity.createActorAttributes());

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            entries.accept(MODEL_BLOCK_ITEM);
        });

        OpenStageNetwork.registerServerHandlers();

        net.openstage.command.OpenStageCommands.register();

        SceneManager.getInstance();

        LOGGER.info("[OpenStage] Initialisation complete – ready to animate!");
    }
}


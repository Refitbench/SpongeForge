/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.GetCollisionBoxesEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.ModCandidate;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.VillagerRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.server.timings.TimeTracker;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.TileEntityType;
import org.spongepowered.api.command.args.ChildCommandElementExecutor;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.type.Profession;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.crafting.CraftingGridInventory;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.common.bridge.world.DimensionTypeBridge;
import org.spongepowered.common.bridge.world.ForgeITeleporterBridge;
import org.spongepowered.common.command.SpongeCommandFactory;
import org.spongepowered.common.data.persistence.NbtTranslator;
import org.spongepowered.common.entity.SpongeProfession;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.context.ItemDropData;
import org.spongepowered.common.event.tracking.phase.block.BlockPhase;
import org.spongepowered.common.event.tracking.phase.plugin.BasicPluginContext;
import org.spongepowered.common.event.tracking.phase.plugin.PluginPhase;
import org.spongepowered.common.item.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.item.inventory.util.InventoryUtil;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;
import org.spongepowered.common.mixin.core.world.WorldAccessor;
import org.spongepowered.common.mixin.plugin.tileentityactivation.TileEntityActivation;
import org.spongepowered.common.registry.type.ItemTypeRegistryModule;
import org.spongepowered.common.registry.type.block.TileEntityTypeRegistryModule;
import org.spongepowered.common.registry.type.entity.ProfessionRegistryModule;
import org.spongepowered.common.registry.type.world.PortalAgentRegistryModule;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.SpawnerSpawnType;
import org.spongepowered.common.util.TristateUtil;
import org.spongepowered.common.world.WorldManager;
import org.spongepowered.mod.SpongeMod;
import org.spongepowered.mod.bridge.block.BlockBridge_Forge;
import org.spongepowered.mod.bridge.event.EventBusBridge_Forge;
import org.spongepowered.mod.bridge.item.ItemStackBridge_Forge;
import org.spongepowered.mod.bridge.registry.VillagerProfessionBridge_Forge;
import org.spongepowered.mod.command.SpongeForgeCommandFactory;
import org.spongepowered.mod.event.SpongeModEventManager;
import org.spongepowered.mod.event.SpongeToForgeEventData;
import org.spongepowered.mod.item.inventory.adapter.IItemHandlerAdapter;
import org.spongepowered.mod.mixin.core.fml.common.registry.VillagerRegistryAccessor;
import org.spongepowered.mod.plugin.SpongeModPluginContainer;
import org.spongepowered.mod.util.WrappedArrayList;
import zone.rong.mixinbooter.util.Environment;

import java.util.*;
import java.util.concurrent.FutureTask;
import java.util.function.Predicate;

import javax.annotation.Nullable;

/**
 * Contains default Vanilla implementations for features that are only
 * available in Forge. SpongeForge overwrites the methods in this class
 * with calls to the Forge methods.
 */
public final class SpongeImplHooks {

    public static boolean isVanilla() {
        return false;
    }

    public static boolean isClientAvailable() {
        return SpongeMod.isClient();
    }

    public static boolean isDeobfuscatedEnvironment() {
        return Environment.inDev();
    }

    public static String getModIdFromClass(final Class<?> clazz) {
        final String className = clazz.getName();
        if (className.startsWith("net.minecraft.")) {
            return SpongeImpl.GAME_ID;
        }
        if (className.startsWith("org.spongepowered.")) {
            return SpongeImpl.ECOSYSTEM_ID;
        }
        ASMDataTable data = SpongeMod.instance.getData();
        if (data == null) {
            return "unknown";
        }
        return data.getCandidatesFor(clazz.getPackage().getName())
                .stream()
                .map(ModCandidate::getContainedMods)
                .flatMap(Collection::stream)
                .findFirst()
                .map(ModContainer::getModId)
                .orElse("unknown");
    }

    // Entity

    public static boolean isCreatureOfType(final Entity entity, final EnumCreatureType type) {
        return entity.isCreatureType(type, false);
    }

    public static boolean isFakePlayer(final Entity entity) {
        return entity instanceof FakePlayer;
    }

    public static void fireServerConnectionEvent(final NetworkManager netManager) {
        FMLCommonHandler.instance().fireServerConnectionEvent(netManager);
    }

    public static void firePlayerJoinSpawnEvent(final EntityPlayerMP playerMP) {
        ((EventBusBridge_Forge) MinecraftForge.EVENT_BUS).forgeBridge$post(new EntityJoinWorldEvent(playerMP, playerMP.getEntityWorld()), true);
    }

    public static void handlePostChangeDimensionEvent(final EntityPlayerMP playerIn, final WorldServer fromWorld, final WorldServer toWorld) {
        FMLCommonHandler.instance().firePlayerChangedDimensionEvent(playerIn, fromWorld.provider.getDimension(), toWorld.provider.getDimension());
    }

    public static boolean checkAttackEntity(final EntityPlayer entityPlayer, final Entity targetEntity) {
        return ForgeHooks.onPlayerAttackTarget(entityPlayer, targetEntity);
    }

    public static double getBlockReachDistance(final EntityPlayerMP player) {
        return player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue();
    }

    public static double getEntityReachDistanceSq(final EntityPlayerMP player, Entity entity) {
        double reach = getBlockReachDistance(player);
        return reach * reach;
    }

    // Entity registry

    @Nullable
    public static Class<? extends Entity> getEntityClass(final ResourceLocation name) {
        return EntityList.getClass(name);
    }

    @Nullable
    public static String getEntityTranslation(final ResourceLocation name) {
        return EntityList.getTranslationName(name);
    }

    public static int getEntityId(final Class<? extends Entity> entityClass) {
        return EntityList.getID(entityClass);
    }

    // Block

    public static boolean isBlockFlammable(final Block block, final IBlockAccess world, final BlockPos pos, final EnumFacing face) {
        return block.isFlammable(world, pos, face);
    }

    public static int getBlockLightOpacity(final IBlockState state, final IBlockAccess world, final BlockPos pos) {
        return state.getLightOpacity(world, pos);
    }

	public static int getChunkPosLight(final IBlockState blockState, final World world, final BlockPos pos) {
        if (((BlockBridge_Forge) blockState.getBlock()).forgeBridge$requiresLocationCheckForLightValue()) {
            return blockState.getLightValue(world, pos);
        }
        return blockState.getLightValue();
	}
    // Tile entity

    @Nullable
    public static TileEntity createTileEntity(final Block block, final net.minecraft.world.World world, final IBlockState state) {
        return block.createTileEntity(world, state);
    }

    public static boolean hasBlockTileEntity(final Block block, final IBlockState state) {
        return block.hasTileEntity(state);
    }

    public static boolean shouldRefresh(final TileEntity tile, final net.minecraft.world.World world, final BlockPos pos, final IBlockState oldState, final IBlockState newState) {
        return tile.shouldRefresh(world, pos, oldState, newState);
    }

    public static void onTileChunkUnload(final TileEntity te) {
        if (te == null) {
            return;
        }
        if (!te.getWorld().isRemote) {
            try (final PhaseContext<?> o = BlockPhase.State.TILE_CHUNK_UNLOAD.createPhaseContext().source(te)) {
                o.buildAndSwitch();
                te.onChunkUnload();
            }
        }
    }

    // World

    public static Iterator<Chunk> getChunkIterator(final WorldServer world) {
        return world.getPersistentChunkIterable(world.getPlayerChunkMap().getChunkIterator());
    }

    public static void registerPortalAgentType(@Nullable final ForgeITeleporterBridge teleporter) {
        if (teleporter == null) {
            return;
        }

        // handle mod registration
        PortalAgentRegistryModule.getInstance().validatePortalAgent(teleporter);
    }

    // World provider

    public static boolean canDoLightning(final WorldProvider provider, final net.minecraft.world.chunk.Chunk chunk) {
        return provider.canDoLightning(chunk);
    }

    public static boolean canDoRainSnowIce(final WorldProvider provider, final net.minecraft.world.chunk.Chunk chunk) {
        return provider.canDoRainSnowIce(chunk);
    }

    public static int getRespawnDimension(final WorldProvider targetDimension, final EntityPlayerMP player) {
        return targetDimension.getRespawnDimension(player);
    }

    public static BlockPos getRandomizedSpawnPoint(final WorldServer world) {
        return world.provider.getRandomizedSpawnPoint();
    }

    // Item stack merging

    public static void addItemStackToListForSpawning(final Collection<ItemDropData> itemStacks, @Nullable final ItemDropData itemStack) {
        // This is the hook that can be overwritten to handle merging the item stack into an already existing item stack
        if (itemStack != null) {
            itemStacks.add(itemStack);
        }
    }

    public static MapStorage getWorldMapStorage(final World world) {
        return world.getPerWorldStorage();
    }

    public static int countEntities(final WorldServer worldServer, final net.minecraft.entity.EnumCreatureType type, final boolean forSpawnCount) {
        return worldServer.countEntities(type, forSpawnCount);
    }

    public static int getMaxSpawnPackSize(final EntityLiving entityLiving) {
        return ForgeEventFactory.getMaxSpawnPackSize(entityLiving);
    }

    public static SpawnerSpawnType canEntitySpawnHere(final EntityLiving entityLiving, final boolean entityNotColliding) {
        final World world = entityLiving.world;
        final float x = (float) entityLiving.posX;
        final float y = (float) entityLiving.posY;
        final float z = (float) entityLiving.posZ;
        final Event.Result canSpawn = ForgeEventFactory.canEntitySpawn(entityLiving, world, x, y, z, false);
        if (canSpawn == Event.Result.ALLOW || (canSpawn == Event.Result.DEFAULT && (entityLiving.getCanSpawnHere()) && entityNotColliding)) {
            if (!ForgeEventFactory.doSpecialSpawn(entityLiving, world, x, y, z)) {
                return SpawnerSpawnType.NORMAL;
            }
            return SpawnerSpawnType.SPECIAL;
        }

        return SpawnerSpawnType.NONE;
    }

    @Nullable
    public static Object onUtilRunTask(final FutureTask<?> task, final Logger logger) {
        final PhaseTracker phaseTracker = PhaseTracker.getInstance();
        try (final BasicPluginContext context = PluginPhase.State.SCHEDULED_TASK.createPhaseContext()
                .source(task))  {
            context.buildAndSwitch();
            final Object o = Util.runTask(task, logger);
            return o;
        } catch (Exception e) {
            phaseTracker
                .printMessageWithCaughtException("Exception during phase body", "Something happened trying to run the main body of a phase", e);

            return null;
        }
    }

    public static void onEntityError(final Entity entity, final CrashReport crashReport) {
        if (ForgeModContainer.removeErroringEntities) {
            FMLLog.log.log(Level.ERROR, crashReport.getCompleteReport());
            entity.getEntityWorld().removeEntity(entity);
        } else {
            throw new ReportedException(crashReport);
        }
    }

    public static void onTileEntityError(final TileEntity tileEntity, final CrashReport crashReport) {
        if (ForgeModContainer.removeErroringTileEntities) {
            FMLLog.log.log(Level.ERROR, crashReport.getCompleteReport());
            tileEntity.invalidate();
            tileEntity.getWorld().removeTileEntity(tileEntity.getPos());
        } else {
            throw new ReportedException(crashReport);
        }
    }

    public static void blockExploded(final Block block, final World world, final BlockPos blockpos, final Explosion explosion) {
        block.onBlockExploded(world, blockpos, explosion);
    }

    /**
     * A method for forge compatibility when mods tend to set the flag
     * to true to mark a world restoring so entity item drops don't
     * get spawned (other entities do get spawned).
     *
     * @param world The world to check
     * @return True if the current phase state is restoring, or the world is restoring in forge.
     */
    @SuppressWarnings("unused") // overridden to be used in MixinSpongeImplHooks.
    public static boolean isRestoringBlocks(final World world) {
        return world.restoringBlockSnapshots || PhaseTracker.getInstance().getCurrentState().isRestoring();
    }

    public static void onTileEntityChunkUnload(final net.minecraft.tileentity.TileEntity tileEntity) {
        tileEntity.onChunkUnload();
    }

    public static boolean canConnectRedstone(final Block block, final IBlockState state, final IBlockAccess world, final BlockPos pos, @Nullable final EnumFacing side) {
        return block.canConnectRedstone(state, world, pos, side);
    }
    // Crafting

    public static Optional<ItemStack> getContainerItem(final ItemStack itemStack) {
        final net.minecraft.item.ItemStack nmsStack = ItemStackUtil.toNative(itemStack);
        final net.minecraft.item.ItemStack nmsContainerStack = ForgeHooks.getContainerItem(nmsStack);

        return nmsContainerStack.isEmpty() ? Optional.empty() : Optional.of(ItemStackUtil.fromNative(nmsContainerStack));
    }

    public static Optional<CraftingRecipe> findMatchingRecipe(final CraftingGridInventory inventory, final org.spongepowered.api.world.World world) {
        final IRecipe recipe = CraftingManager.findMatchingRecipe(InventoryUtil.toNativeInventory(inventory), ((World) world));
        return Optional.ofNullable(((CraftingRecipe) recipe));
    }

    public static Collection<CraftingRecipe> getCraftingRecipes() {
        return ForgeRegistries.RECIPES.getValues().stream()
                .map(CraftingRecipe.class::cast)
                .collect(ImmutableList.toImmutableList());
    }

    public static Optional<CraftingRecipe> getRecipeById(final String id) {
        final IRecipe recipe = ForgeRegistries.RECIPES.getValue(new ResourceLocation(id));
        if (recipe == null) {
            return Optional.empty();
        }
        return Optional.of(((CraftingRecipe) recipe));
    }

    public static void register(final ResourceLocation name, final IRecipe recipe) {
        recipe.setRegistryName(name);
        ForgeRegistries.RECIPES.register(recipe);
    }

    @Nullable
    public static PluginContainer getActiveModContainer() {
        ModContainer activeModContainer = Loader.instance().activeModContainer();
        return activeModContainer == null ? null : Sponge.getPluginManager().getPlugin(activeModContainer.getModId()).orElse(null);
    }

    public static PluginContainer getActiveModContainerOrMinecraft() {
        PluginContainer pluginContainer = getActiveModContainer();
        return pluginContainer != null ? pluginContainer : SpongeImpl.getMinecraftPlugin();
    }

    public static Text getAdditionalCommandDescriptions() {
        return Text.of(SpongeCommandFactory.INDENT, SpongeCommandFactory.title("mods"), SpongeCommandFactory.LONG_INDENT, "List currently installed mods");
    }

    public static void registerAdditionalCommands(final ChildCommandElementExecutor flagChildren, final ChildCommandElementExecutor nonFlagChildren) {
        nonFlagChildren.register(SpongeForgeCommandFactory.createSpongeModsCommand(), "mods");
    }

    public static Predicate<? super PluginContainer> getPluginFilterPredicate() {
        return plugin -> !SpongeCommandFactory.CONTAINER_LIST_STATICS.contains(plugin.getId()) && plugin instanceof SpongeModPluginContainer;
    }

    // Borrowed from Forge, with adjustments by us

    @Nullable
    public static RayTraceResult rayTraceEyes(final EntityLivingBase entity, final double length) {
        final Vec3d startPos = new Vec3d(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
        final Vec3d endPos = startPos.add(entity.getLookVec().scale(length));
        return entity.world.rayTraceBlocks(startPos, endPos);
    }

    public static boolean shouldKeepSpawnLoaded(final net.minecraft.world.DimensionType dimensionType, final int dimensionId) {
        final WorldServer worldServer = WorldManager.getWorldByDimensionId(dimensionId).orElse(null);
        return worldServer != null && ((WorldProperties) worldServer.getWorldInfo()).doesKeepSpawnLoaded();

    }

    public static void setShouldLoadSpawn(final net.minecraft.world.DimensionType dimensionType, final boolean keepSpawnLoaded) {
        ((DimensionTypeBridge) (Object) dimensionType).setShouldLoadSpawn(keepSpawnLoaded);
    }

    public static BlockPos getBedLocation(final EntityPlayer playerIn, final int dimension) {
        return playerIn.getBedLocation(dimension);
    }

    public static boolean isSpawnForced(final EntityPlayer playerIn, final int dimension) {
        return playerIn.isSpawnForced(dimension);
    }

    public static Inventory toInventory(final Object inventory, @Nullable final Object forgeItemHandler) {
        if (forgeItemHandler instanceof Inventory) {
            return (Inventory) forgeItemHandler;
        }

        // Prefer forge IItemHandler for interaction with modded inventory
        if (inventory instanceof ICapabilityProvider) {
            IItemHandler itemHandler = ((ICapabilityProvider) inventory).getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (itemHandler != null) {
                return (Inventory) itemHandler;
            }
        }

        if (inventory instanceof Inventory) {
            return ((Inventory) inventory);
        }

        final String fallbackName = forgeItemHandler == null ? "no forgeItemHandler" : forgeItemHandler.getClass().getName();
        SpongeImpl.getLogger().error("Unknown inventory " + inventory.getClass().getName() + " and " + fallbackName + " report this to Sponge");
        return null;
    }

    public static InventoryAdapter findInventoryAdapter(final Object inventory) {
        // If the inventory provides a IItemHandler take that one first
        if (inventory instanceof ICapabilityProvider) {
            IItemHandler itemHandler = ((ICapabilityProvider) inventory).getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (itemHandler instanceof InventoryAdapter) {
                return (InventoryAdapter) itemHandler;
            }
            if (itemHandler != null) {
                return new IItemHandlerAdapter(itemHandler); // TODO caching?
            }
        }

        // If the inventory directly implements IItemHandler we have to wrap it to get an adapter
        if (inventory instanceof IItemHandler) {
            return new IItemHandlerAdapter((IItemHandler) inventory); // TODO caching?
        }

        // If the inventory directly implements IInventory we wrap in an InvWrapper
        if (inventory instanceof IInventory) {
            return (InventoryAdapter) new InvWrapper((IInventory) inventory); // TODO caching?
        }

        // This should never happen
        SpongeImpl.getLogger().error("Unknown inventory " + inventory.getClass().getName() + " report this to Sponge");
        throw new IllegalArgumentException("Unknown inventory " + inventory.getClass().getName() + " report this to Sponge");
    }

    public static void onTileEntityInvalidate(final TileEntity te) {
        try (final PhaseContext<?> o = BlockPhase.State.TILE_ENTITY_INVALIDATING.createPhaseContext().source(te)) {
            o.buildAndSwitch();
            te.invalidate();
        }
    }

    public static void capturePerEntityItemDrop(final PhaseContext<?> phaseContext, final Entity owner, final EntityItem entityitem) {
            final ArrayListMultimap<UUID, EntityItem> map = phaseContext.getPerEntityItemEntityDropSupplier().get();
            final ArrayList<EntityItem> entityItems = new WrappedArrayList(owner, map.get(owner.getUniqueID()));
            // Re-assigns the list, to ensure that the list is being used.
            final ArrayList<EntityItem> capturedDrops = owner.capturedDrops;
            if (capturedDrops != entityItems) {
                owner.capturedDrops = entityItems;
                // If the list was not empty, go ahead and populate sponge's since we had to re-assign the list.
                if (!capturedDrops.isEmpty()) {
                    entityItems.addAll(capturedDrops);
                }
            }
            entityItems.add(entityitem);
    }

    /**
     * @author gabizou - April 21st, 2018
     * Gets the enchantment modifier for looting on the entity living base from the damage source, but in forge cases, we need to use their hooks.
     *
     * @param target
     * @param entity
     * @param cause
     * @return
     */
    public static int getLootingEnchantmentModifier(final EntityLivingBase target, final EntityLivingBase entity, final DamageSource cause) {
        return ForgeHooks.getLootingLevel(target, entity, cause);
    }

    public static double getWorldMaxEntityRadius(final WorldServer worldServer) {
        return WorldServer.MAX_ENTITY_RADIUS;
    }

    /**
     * Provides the {@link Profession} to set onto the villager. Since forge has it's own
     * villager profession system, sponge has to bridge the compatibility and
     * the profession may not be "properly" registered.
     * @param professionId
     * @return
     */
    public static Profession validateProfession(final int professionId) {
        final VillagerRegistry.VillagerProfession
                profession =
                ((VillagerRegistryAccessor) VillagerRegistry.instance()).accessor$getRegistry().getObjectById(professionId);
        if (profession == null) {
            throw new RuntimeException("Attempted to set villager profession to unregistered profession: " + professionId);
        }
        final VillagerProfessionBridge_Forge mixinProfession = (VillagerProfessionBridge_Forge) profession;
        return mixinProfession.forgeBridge$getSpongeProfession().orElseGet(() -> {
            final SpongeProfession newProfession = new SpongeProfession(professionId, mixinProfession.forgeBridge$getId(), mixinProfession.forgeBridge$getProfessionName());
            mixinProfession.forgeBridge$setSpongeProfession(newProfession);
            ProfessionRegistryModule.getInstance().registerAdditionalCatalog(newProfession);
            return newProfession;
        });
    }

    public static void onUseItemTick(final EntityLivingBase entity, final net.minecraft.item.ItemStack stack, final int activeItemStackUseCount) {
        if (!stack.isEmpty() && activeItemStackUseCount > 0) {
            stack.getItem().onUsingTick(stack, entity, activeItemStackUseCount);
        }
    }

    public static void onTETickStart(final TileEntity te) {
        TimeTracker.TILE_ENTITY_UPDATE.trackStart(te);
    }

    public static void onTETickEnd(final TileEntity te) {
        TimeTracker.TILE_ENTITY_UPDATE.trackEnd(te);
    }

    public static void onEntityTickStart(final Entity entity) {
        TimeTracker.ENTITY_UPDATE.trackStart(entity);
    }

    public static void onEntityTickEnd(final Entity entity) {
        TimeTracker.ENTITY_UPDATE.trackEnd(entity);
    }

    public static boolean isMainThread() {
        // Return true when the server isn't yet initialized, this means on a client
        // that the game is still being loaded. This is needed to support initialization
        // events with cause tracking.
        return !Sponge.isServerAvailable() || Sponge.getServer().isMainThread();
    }

    // Overridden by SpongeImplHooksMixin_ItemNameOverflowPrevention for exploit check
    public static boolean creativeExploitCheck(final Packet<?> packetIn, final EntityPlayerMP playerMP) {
        return false;
    }

    public static String getImplementationId() {
        return "spongeforge";
    }

    /**
     * @author gabizou - July 31st, 2018
     * @reason Due to ForgeMultiPart having some modifications to tile entity registration,
     * we sometimes cannot guarantee that we'll have a valid type. If this is the case,
     * we sometimes need to "register" one for our own uses for plugins to validate.
     *
     * Refer to https://github.com/TheCBProject/ForgeMultipart/issues/33
     */
    public static TileEntityType getTileEntityType(final Class<? extends TileEntity> aClass) {
        final ResourceLocation location = TileEntity.getKey((Class<? extends TileEntity>) aClass);
        if (location == null) {
            // Means it's not properly registered either....
            return null;
        }

        final TileEntityType translated = SpongeImpl.getRegistry().getTranslated(aClass, TileEntityType.class);
        if (translated == null) {
            // this is a rare case where we don't even have access to the correct tile entity type
            // through normal registrations. So, instead, what we do is we have to re-create the type
            // for this tile entity class and then say "ok, here's a newly registered one"
            return TileEntityTypeRegistryModule.getInstance().doTileEntityRegistration(aClass, location.getPath());
        }
        return translated;
    }

    /**
     * @author gabizou - April 23rd, 2019 - 1.12.2
     * @reason Eliminate an extra overwrite for SpongeForge for PlayerInteractionManagerMixin#processRightClickBlock.
     *
     * @param spongeEvent The sponge event
     * @return The forge event
     */
    @Nullable
    public static Object postForgeEventDataCompatForSponge(final InteractBlockEvent.Secondary spongeEvent) {
        return ((SpongeModEventManager) Sponge.getEventManager()).extendedPost(spongeEvent, false, false);
    }

    // Some mods such as OpenComputers open a GUI on client-side
    // To workaround this, we will always send a SPacketCloseWindow to client if interacting with a TE
    // However, we skip closing under two circumstances:
    //
    // * If an inventory has already been opened on the server (e.g. by a plugin),
    // since we don't want to undo that

    // * If the event was cancelled by a Forge mod. In this case, we adhere to Forge's normal
    // bheavior, which is to leave any GUIs open on the client. Some mods, like Quark, modify
    // Vanilla blocks (such as noteblocks) by opening a custom GUI on the client interaction event,
    // and then cancelling the interaction event on the server.
    //
    // In the second case, we have two conflicting goals. First, we want to ensure that Sponge protection
    // plugins are ablee to fully prevent interactions with a block. This means sending a close
    // window packet to the client when the event is cancelled, since we can't know what
    // client-side only GUIs (no Container) a mod may have opened.
    //
    // However, we don't want to break mods that rely on the fact that cancelling
    // a server-side interaction events leaves any client GUIs open.
    //
    // To resolve this issue, we only send a close window packet if the event was not cancelled
    // by a Forge event listener.
    // SpongeForge - end
    /**
     * @author gabizou - April 23rd, 2019 - 1.12.2
     * @reason Eliminate an extra overwrite for SpongeForge for PlayerInteractionManagerMixin#processRightClickBlock.
     *
     * @param worldIn The world
     * @param pos The position
     * @param eventData The event data, if it was created
     * @param player The player
     */
    public static void shouldCloseScreen(final World worldIn, final BlockPos pos, @Nullable final Object eventData, final EntityPlayerMP player) {
        if (worldIn.getTileEntity(pos) != null && player.openContainer instanceof ContainerPlayer && (eventData == null || !((SpongeToForgeEventData) eventData).getForgeEvent().isCanceled())) {
            player.closeScreen();
        }
    }

    /**
     * @author gabizou - April 23rd, 2019 - 1.12.2
     * @reason Eliminate an extra overwrite for SpongeForge for PlayerInteractionManagerMixin#processRightClickBlock.
     *
     * @param forgeEventObject The forge event object, if it was created
     * @return The result as a result of the event data
     */
    public static EnumActionResult getInteractionCancellationResult(@Nullable final Object forgeEventObject) {
        // If a Forge event wasn't fired (due to no Forge mods listening for event), we want
        // to act as though an un-cancelled, unmodified Forge event was still fired.
        // Forge's PlayerInteractEvent#getCancellationResult() defaults to EnumActionResult.PASS,
        // so we return that.
        final SpongeToForgeEventData eventData = (SpongeToForgeEventData) forgeEventObject;
        return eventData == null ? EnumActionResult.PASS : ((PlayerInteractEvent) eventData.getForgeEvent()).getCancellationResult(); // SpongeForge - return event result

    }

    /**
     * @author gabizou - April 23rd, 2019 - 1.12.2
     * @reason Eliminate an extra overwrite for SpongeForge for PlayerInteractionManagerMixin#processRightClickBlock.
     *
     * @param worldIn The world in
     * @param pos The position
     * @param player The player
     * @param main The main hand item
     * @param off The offhand item
     * @return Whether to bypass sneaking state, forge has an extra hook on the item class
     */
    public static boolean doesItemSneakBypass(final World worldIn, final BlockPos pos, final EntityPlayer player, final net.minecraft.item.ItemStack main, final net.minecraft.item.ItemStack off) {
        boolean bypass;
        bypass = main.isEmpty() || main.getItem().doesSneakBypassUse(main, worldIn, pos, player);
        bypass = bypass && (off.isEmpty() || off.getItem().doesSneakBypassUse(off, worldIn, pos, player));
        return bypass;
    }

    /**
     * @author gabizou - April 23rd, 2019 - 1.12.2
     * @reason Eliminate an extra overwrite for SpongeForge for PlayerInteractionManagerMixin#processRightClickBlock.
     *
     * @param player The player interacting
     * @param event The sponge event
     * @param result The current result
     * @param worldIn The world
     * @param pos the position
     * @param hand the hand used
     * @return Null so that the rest of the method continues processing? TODO - Zidane and Morph, please check this...
     */
    @Nullable
    public static EnumActionResult getEnumResultForProcessRightClickBlock(final EntityPlayerMP player,
        final InteractBlockEvent.Secondary event, EnumActionResult result, final World worldIn, final BlockPos pos,
        final EnumHand hand) {
        result = TristateUtil.toActionResult(event.getUseItemResult());

        // SpongeForge - start
        // Same issue as above with OpenComputers
        // This handles the event not cancelled and block not activated
        // We only run this if the event was changed. If the event wasn't changed,
        // we need to keep the GUI open on the client for Forge compatibility.
        if (result != EnumActionResult.SUCCESS && worldIn.getTileEntity(pos) != null && hand == EnumHand.MAIN_HAND) {
            player.closeScreen();
        }
        // SpongeForge - end
        return null;
    }

    /**
     * @author gabizou - April 23rd, 2019 - 1.12.2
     * @reason Eliminate an extra overwrite for SpongeForge for PlayerInteractionManagerMixin#processRightClickBlock.
     *
     * @param player The player
     * @param stack The item stack to check
     * @param worldIn The world
     * @param pos the position
     * @param hand the hand used
     * @param facing Facing direction
     * @param hitX hit x pos
     * @param hitY hit y pos
     * @param hitZ hit z pos
     * @return The result of the item stack's hook method
     */
    public static EnumActionResult onForgeItemUseFirst(
        final EntityPlayer player, final net.minecraft.item.ItemStack stack, final World worldIn, final BlockPos pos,
        final EnumHand hand, final EnumFacing facing, final float hitX,
        final float hitY, final float hitZ) {
        return stack.onItemUseFirst(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
    }

    /**
     * @author gabizou - May 10th, 2019 - 1.12.2
     * @reason Forge events are getting wrapped in various cases that end up causing corner cases where the effective side
     * @param object The event
     * @return False by default, means all server sided events or common events are allowed otherwise.
     */
    public static boolean isEventClientEvent(final Object object) {
        final SideOnly annotation = object.getClass().getAnnotation(SideOnly.class);
        return annotation != null && annotation.value() == Side.CLIENT;
    }


    /**
     * @author gabizou - May 28th, 2019 - 1.12.2
     * @reason Forge has custom items, and normally, we throw an event for any
     * and all listeners. The problem is that since Forge just blindly calls
     * events, and Sponge only throws events if there are listeners, the custom
     * item hook does not get called for direct spawned entities, so, we need
     * to explicitly call the custom item creation hooks here.
     *
     * @param entity The vanilla entity item
     * @return The custom item entity for the dropped item
     */
    @SuppressWarnings("ConstantConditions")
    @Nullable
    public static Entity getCustomEntityIfItem(final Entity entity) {
        final net.minecraft.item.ItemStack stack =
                entity instanceof EntityItem ? ((EntityItem) entity).getItem() : net.minecraft.item.ItemStack.EMPTY;
        final Item item = stack.getItem();

        if (item.hasCustomEntity(stack)) {
            final Entity newEntity = item.createEntity(entity.getEntityWorld(), entity, stack); // Sponge - use world from entity
            if (newEntity != null) {
                entity.setDead();

                return newEntity;
            }
        }
        return null;
    }

    /**
     * For use with {@link TileEntityActivation}.
     *
     * @param tile The tile to tick
     * @return True whether to tick or false, not to
     */
    public static boolean shouldTickTile(final ITickable tile) {
        return true;
    }

    /**
     * Used for compatibility with Forge where Forge uses wrapped Items
     * since they allow for registry replacements.
     *
     * @param mixinItem_api The item
     * @return The resource location id
     */
    @Nullable
    public static ResourceLocation getItemResourceLocation(final Item mixinItem_api) {
        return mixinItem_api.getRegistryName();
    }

    public static void registerItemForSpongeRegistry(final int id, final ResourceLocation name, final Item item) {
        final Item registered;
        final ResourceLocation nameForObject = Item.REGISTRY.getNameForObject(item);
        if (nameForObject == null) {
            registered = checkNotNull(Item.REGISTRY.getObject(name), "Someone replaced a vanilla item with a null item!!!");
        } else {
            registered = item;
        }
        ItemTypeRegistryModule.getInstance().registerAdditionalCatalog((ItemType) registered);
    }

    public static void writeItemStackCapabilitiesToDataView(final DataContainer container, final net.minecraft.item.ItemStack stack) {
        final CapabilityDispatcher capabilities = ((ItemStackBridge_Forge) (Object) stack).forgeBridge$getCapabilities();
        if (capabilities != null) {
            final NBTTagCompound caps = capabilities.serializeNBT();
            if (caps != null && !caps.isEmpty()) {
                final DataContainer capsView = NbtTranslator.getInstance().translate(caps);
                container.set(Constants.Sponge.UNSAFE_NBT.then(Constants.Forge.FORGE_CAPS), capsView);
            }
        }
    }

    public static boolean canEnchantmentBeAppliedToItem(final Enchantment enchantment, final net.minecraft.item.ItemStack stack) {
        return (stack.getItem() == ItemTypes.BOOK) ? enchantment.isAllowedOnBooks() : enchantment.canApply((net.minecraft.item.ItemStack) (Object) stack);
    }

    public static void setCapabilitiesFromSpongeBuilder(final ItemStack stack, final NBTTagCompound compoundTag) {
        final CapabilityDispatcher capabilities = ((ItemStackBridge_Forge) stack).forgeBridge$getCapabilities();
        if (capabilities != null) {
            capabilities.deserializeNBT(compoundTag);
        }
    }

    public static TileEntity onChunkGetTileDuringRemoval(final WorldServer worldServer, final BlockPos pos) {
        if (((WorldAccessor) worldServer).accessor$getIsOutsideBuildHeight(pos)) {
            return null;
        } else {
            TileEntity tileentity2 = null;

            if (((WorldAccessor) worldServer).accessor$getProcessingLoadedTiles()) {
                tileentity2 = ((WorldAccessor) worldServer).accessPendingTileEntityAt(pos);
            }

            if (tileentity2 == null) {
                // Sponge - Instead of creating the tile entity, just check if it's there. If the
                // tile entity doesn't exist, don't create it since we're about to just wholesale remove it...
                // tileentity2 = this.getChunk(pos).getTileEntity(pos, Chunk.EnumCreateEntityType.IMMEDIATE);
                tileentity2 = worldServer.getChunk(pos).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
            }

            if (tileentity2 == null) {
                tileentity2 =  ((WorldAccessor) worldServer).accessPendingTileEntityAt(pos);
            }

            return tileentity2;
        }
    }

    /**
     * @author JBYoshi
     * @reason Forge compatibility
     * @param world The world in which the event takes place
     * @param entityIn The entity that called collisions
     * @param aabb The bounding box
     * @param collided The entities that were collided with
     */
    public static void onForgeCollision(final World world, @Nullable final Entity entityIn, final AxisAlignedBB aabb,
            final List<AxisAlignedBB> collided) {
        MinecraftForge.EVENT_BUS.post(new GetCollisionBoxesEvent(world, entityIn, aabb, collided));
    }
}

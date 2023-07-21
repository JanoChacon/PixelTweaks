package com.strangeone101.pixeltweaks.tweaks;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.pixelmonmod.pixelmon.api.config.PixelmonConfigProxy;
import com.pixelmonmod.pixelmon.api.registries.PixelmonBlocks;
import com.pixelmonmod.pixelmon.api.util.Scheduling;
import com.pixelmonmod.pixelmon.api.util.helpers.RandomHelper;
import com.pixelmonmod.pixelmon.blocks.ZygardeCellBlock;
import com.pixelmonmod.pixelmon.blocks.tileentity.ZygardeCellTileEntity;
import com.pixelmonmod.pixelmon.items.ZygardeCubeItem;
import com.pixelmonmod.pixelmon.listener.ZygardeCellsListener;
import com.strangeone101.pixeltweaks.PixelTweaks;
import com.strangeone101.pixeltweaks.mixin.ZygardeListenerMixin;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.pathfinding.PathType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.state.Property;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ZygardeCellSpawner {

    private static ResourceLocation LOGS_RESOURCELOCATION = new ResourceLocation(PixelTweaks.MODID, "zygarde_cell_placement/logs");
    private static ResourceLocation LEAVES_RESOURCELOCATION = new ResourceLocation(PixelTweaks.MODID, "zygarde_cell_placement/leaves");
    private static ResourceLocation GRASS_RESOURCELOCATION = new ResourceLocation(PixelTweaks.MODID, "zygarde_cell_placement/grass");

    private static ITag<Block> LOGS;
    private static ITag<Block> LEAVES;
    private static ITag<Block> GRASS;

    private static Set<UUID> CLOSE_SPAWN_COOLDOWN = new HashSet<>();

    public ZygardeCellSpawner() {


        Scheduling.schedule(1, () -> {
            try {
                //Get the tasks in the task scheduler
                Class<?> clazz = Scheduling.class;
                Field field = clazz.getDeclaredField("tasks");
                field.setAccessible(true);
                ArrayList<Scheduling.ScheduledTask> tasks = (ArrayList<Scheduling.ScheduledTask>) field.get(null);

                PixelTweaks.LOGGER.debug("Tasks: " + tasks.size());
                //Remove all Zygarde tasks
                for (Scheduling.ScheduledTask task : tasks) {
                    String name = task.task.getClass().getName();
                    if (name.startsWith("com.pixelmonmod.pixelmon.listener.ZygardeCellsListener$$Lambda$")) {
                        task.repeats = false; //Set them to remove next tick. Easiest way to remove them without causing a ConcurrentModificationException
                    }
                    PixelTweaks.LOGGER.debug(task.task.getClass().getName());
                }

                //Get the list of blocks that it can spawn on
                ZygardeListenerMixin.getSpawnableBlocks().clear(); //Clear the existing list

                ZygardeListenerMixin.getSpawnableBlocks().addAll(GRASS.getAllElements()); //Add all the blocks from the tag
                ZygardeListenerMixin.getSpawnableBlocks().addAll(LOGS.getAllElements()); //Add all the blocks from the tag
                ZygardeListenerMixin.getSpawnableBlocks().addAll(LEAVES.getAllElements()); //Add all the blocks from the tag

                if (!PixelmonConfigProxy.getSpawning().isSpawnZygardeCells()) {
                    PixelTweaks.LOGGER.info("Zygarde cell spawning disabled! Modify the pixelmon spawning.yml config to change this");
                    return; //If spawning is disabled, don't do anything else
                }

                Scheduling.schedule(20 * 5, (task) -> {
                    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                    if (server != null && server.isServerRunning()) {
                        for (ServerPlayerEntity player : server.getPlayerList().getPlayers()) {
                            if (ZygardeCellsListener.checkForCube(player)) {
                                ZygardeListenerMixin.getHasCube().add(player.getUniqueID());
                            }
                        }
                    }
                }, true);

                Scheduling.schedule(20 * 15, spawnZygardeTask(), true);
                PixelTweaks.LOGGER.info("Zygarde cell spawner initialized!");

            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }, false);
    }

    public Consumer<Scheduling.ScheduledTask> spawnZygardeTask() {
        return (task) -> {
            if (PixelmonConfigProxy.getSpawning().isSpawnZygardeCells()) {
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server != null && server.isServerRunning()) {
                    if (ZygardeListenerMixin.getHasCube().isEmpty()) {
                        return;
                    }

                    UUID random = (UUID) RandomHelper.getRandomElementFromArray(ZygardeListenerMixin.getHasCube().toArray(new UUID[0]));
                    ServerPlayerEntity player = server.getPlayerList().getPlayerByUUID(random);
                    if (player == null || player.isSpectator()) {
                        return;
                    }

                    List<Chunk> chunks = Lists.newArrayList();
                    int distance = Math.min(server.getPlayerList().getViewDistance(), 7);
                    int x1 = player.chunkCoordX + distance;
                    int z1 = player.chunkCoordZ + distance;
                    int x2 = player.chunkCoordX - distance;
                    int z2 = player.chunkCoordZ - distance;

                    for(int x = x1; x >= x2; --x) {
                        for(int z = z1; z >= z2; --z) {
                            if (x < player.chunkCoordX - 1 || x > player.chunkCoordX + 1 || z < player.chunkCoordZ - 1 || z > player.chunkCoordZ + 1) {
                                Chunk chunk = player.getServerWorld().getChunkProvider().getChunkNow(x, z);
                                if (chunk != null) {
                                    chunks.add(chunk);
                                }
                            }
                        }
                    }

                    if (!chunks.isEmpty()) {
                        chunks.sort((c1, c2) -> {
                            int i = Math.abs(c1.getPos().x - player.chunkCoordX);
                            int j = Math.abs(c1.getPos().z - player.chunkCoordZ);
                            int k = Math.abs(c2.getPos().x - player.chunkCoordX);
                            int l = Math.abs(c2.getPos().z - player.chunkCoordZ);
                            return (k * k + l * l) - (i * i + j * j);
                        });
                        int n = RandomHelper.getRandom().nextInt(chunks.size());

                        if (!CLOSE_SPAWN_COOLDOWN.contains(random)) { //If the player hasn't had a cell spawn near them recently, spawn it close
                            n = (int)Math.sqrt((chunks.size() * chunks.size()) - RandomHelper.getRandom().nextInt(chunks.size() * chunks.size()));
                        }
                        Chunk chunk = chunks.get(n);
                        int i = chunk.getPos().x - player.chunkCoordX;
                        int j = chunk.getPos().z - player.chunkCoordZ;
                        PixelTweaks.LOGGER.debug("Spawning cell " + i + ", " + j + " chunks away");

                        if (trySpawnInChunk(chunk, true)) { //If a cell spawned
                            CLOSE_SPAWN_COOLDOWN.add(random); //Add the player to the cooldown list
                            Scheduling.schedule(20 * 60, () -> CLOSE_SPAWN_COOLDOWN.remove(random), false); //Remove them after 1 minutes
                        }
                    }
                }

            }
        };
    }

    public static boolean trySpawnInChunk(IChunk chunk, boolean checkTiles) {
        IWorld world = chunk.getWorldForge();
        int x = RandomHelper.getRandomNumberBetween(1, 14);
        int z = RandomHelper.getRandomNumberBetween(1, 14);
        BlockPos pos = new BlockPos(x, 62, z);

        if (!checkTiles || !tileEntityExistsWithin(ZygardeCellTileEntity.class, pos, ((Chunk)chunk).getWorld(), 72.0)) {
            int y = chunk.getTopBlockY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y != 120) {
                Multimap<Block, BlockPos> map = MultimapBuilder.hashKeys().hashSetValues().build();
                pos = new BlockPos(x, y, z);
                int x1 = pos.getX() + 1;
                int y1 = pos.getY() + 5;
                int z1 = pos.getZ() + 1;
                int x2 = pos.getX() - 1;
                int y2 = pos.getY() - 4;
                int z2 = pos.getZ() - 1;

                for (int lx = x1; lx >= x2; --lx) {
                    for (int ly = y1; ly >= y2; --ly) {
                        for (int lz = z1; lz >= z2; --lz) {
                            pos = new BlockPos(lx, ly, lz);
                            Block b = chunk.getBlockState(pos).getBlock();
                            if (ZygardeListenerMixin.getSpawnableBlocks().contains(b)) {
                                map.put(b, pos);
                            }
                        }
                    }
                }

                if (!map.isEmpty()) {
                    Direction facing = null;

                    boolean hasLogs = map.keys().stream().anyMatch((b) -> LOGS.contains(b));

                    if (hasLogs) {
                        List<BlockPos> logs = map.keys().stream().filter((b) -> LOGS.contains(b)).flatMap((b) -> map.get(b).stream()).collect(Collectors.toList());
                        Collections.shuffle(logs);

                        for (BlockPos pos1 : logs) {
                            BlockState state = chunk.getBlockState(pos1);
                            if (state.hasProperty(BlockStateProperties.AXIS)) {
                                if (state.get(BlockStateProperties.AXIS) == Direction.Axis.X) {
                                    facing = hasAirPocket(chunk, pos1, Direction.DOWN, Direction.UP, Direction.WEST, Direction.EAST);
                                    if (facing != null) {
                                        spawnOn(chunk, pos1.offset(facing), facing.getOpposite());
                                        return true;
                                    }
                                } else if (state.get(BlockStateProperties.AXIS) == Direction.Axis.Z) {
                                    facing = hasAirPocket(chunk, pos1, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH);
                                    if (facing != null) {
                                        spawnOn(chunk, pos1.offset(facing), facing.getOpposite());
                                        return true;
                                    }
                                }
                            }
                            facing = hasAirPocket(chunk, pos1, Direction.byHorizontalIndex(RandomHelper.getRandom().nextInt(4)));
                            if (facing != null) {
                                spawnOn(chunk, pos1.offset(facing), facing.getOpposite());
                                return true;
                            }
                        }
                    }

                    List<BlockPos> leaves = map.keys().stream().filter((b) -> LEAVES.contains(b)).flatMap((b) -> map.get(b).stream()).collect(Collectors.toList());
                    Collections.shuffle(leaves);
                    List<BlockPos> grass =  map.keys().stream().filter((b) -> GRASS.contains(b)).flatMap((b) -> map.get(b).stream()).collect(Collectors.toList());
                    Collections.shuffle(grass);

                    Predicate<List<BlockPos>> doLeaves = (list) -> {
                        Iterator<BlockPos> it = list.iterator();
                        BlockPos pos1;
                        BlockState state;

                        exit:
                        while (true) {
                            do {
                                if (!it.hasNext()) {
                                    break exit;
                                }

                                pos1 = it.next();
                                state = chunk.getBlockState(pos1);
                            } while (state.hasProperty(BlockStateProperties.UNSTABLE) && state.get(BlockStateProperties.UNSTABLE));

                            Direction facing2 = hasAirPocket(chunk, pos1, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
                            if (facing2 != null) {
                                spawnOn(chunk, pos1.offset(facing2), facing2.getOpposite());
                                return true;
                            }
                        }
                        return false;
                    };

                    Predicate<List<BlockPos>> doGrass = (list) -> {
                        for (BlockPos pos1 : list) {
                            pos1 = pos1.add(0, 1, 0);
                            Direction facing2 = hasAirPocket(chunk, pos1, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
                            if (facing2 != null) {
                                spawnOn(chunk, pos1, Direction.DOWN);
                                return true;
                            }
                        }
                        return false;
                    };

                    if (RandomHelper.getRandom().nextFloat() >= 0.35) { //Do grass first most of the time
                        return doGrass.test(grass) || doLeaves.test(leaves);
                    } else { //Do leaves for 35% of the time first
                        return doLeaves.test(grass) || doGrass.test(leaves);
                    }
                }
            }

        }

        return false;
    }

    private static void spawnOn(IChunk chunk, BlockPos pos, Direction facing) {
        if (chunk instanceof Chunk) {
            BlockState currentState = ((Chunk)chunk).getWorld().getBlockState(pos);
            if (currentState.isAir() || (!currentState.isSolid() && currentState.getFluidState().getFluid() == Fluids.EMPTY && (!currentState.hasProperty(BlockStateProperties.WATERLOGGED) || !currentState.get(BlockStateProperties.WATERLOGGED)))) {
                Direction rotation = facing.getAxis() == Direction.Axis.Y ? Direction.byHorizontalIndex(RandomHelper.getRandom().nextInt(4)) : (RandomHelper.getRandomChance() ? Direction.UP : Direction.DOWN);

                int coreChance = 20;
                if (((Chunk) chunk).getWorld().isThundering()) coreChance = 7;

                Block block = RandomHelper.getRandom().nextInt(coreChance) == 0 ? PixelmonBlocks.zygarde_core : PixelmonBlocks.zygarde_cell;
                BlockState state = (BlockState)((BlockState)block.getDefaultState().with(ZygardeCellBlock.ORIENTATION_PROPERTY, facing)).with(ZygardeCellBlock.ROTATION_PROPERTY, rotation);
                ZygardeCellTileEntity tileEntity = new ZygardeCellTileEntity();
                tileEntity.setPos(pos);
                tileEntity.setCoreType(ZygardeCubeItem.CoreType.RANDOM);
                BlockPos realPos = chunk.getPos().asBlockPos().add(pos.getX(), pos.getY(), pos.getZ());

                ((Chunk)chunk).getWorld().setBlockState(realPos, state);
                ((Chunk)chunk).getWorld().addTileEntity(tileEntity);
                ((Chunk)chunk).getWorld().markAndNotifyBlock(realPos, (Chunk)chunk, state, state, 3, 0);
                //((Chunk)chunk).addTileEntity(tileEntity);
                //PixelTweaks.LOGGER.debug("Spawned via chunk method: " + (((Chunk)chunk).getWorld().getServer().getExecutionThread() == Thread.currentThread()));

                chunk.setModified(true);
                PixelTweaks.LOGGER.debug("Spawned Zygarde Cell at " + realPos);
            }
        }
    }

    private static Direction hasAirPocket(IChunk chunk, BlockPos pos, Direction... facings) {
        List<Direction> facingList = Lists.newArrayList();
        Direction[] var4 = facings;
        int var5 = facings.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            Direction facing = var4[var6];
            BlockPos offset = pos.offset(facing);
            BlockState state = chunk.getBlockState(offset);
            //If its air, or not solid, and also not waterlogged, then we can spawn on it
            if (state.isAir() || (!state.isSolid() && state.allowsMovement(chunk, offset, PathType.LAND) && (!state.hasProperty(BlockStateProperties.WATERLOGGED) || !state.get(BlockStateProperties.WATERLOGGED)))) {
                facingList.add(facing);
            }
        }

        return RandomHelper.getRandomElementFromList(facingList);
    }

    public static <T extends TileEntity> boolean tileEntityExistsWithin(Class<T> tileEntity, BlockPos pos, World world, double range) {
        int chunkXPos = pos.getX() >> 4;
        int chunkZPos = pos.getZ() >> 4;
        int chunkRange = Math.max((int) (range / 16.0), 1) + 1;

        for (int x = chunkXPos - chunkRange + 1; x < chunkXPos + chunkRange; ++x) {
            for (int z = chunkZPos - chunkRange + 1; z < chunkZPos + chunkRange; ++z) {
                if (world.getChunkProvider().chunkExists(x, z)) {
                    Chunk chunk = world.getChunk(x, z);
                    for (BlockPos pos2 : chunk.getTileEntityMap().keySet()) {
                        TileEntity tile = chunk.getTileEntityMap().get(pos2);
                        if (tileEntity.isAssignableFrom(tile.getClass()) && pos.distanceSq(pos2) <= range) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private static double getDistance(BlockPos pos1, BlockPos pos2) {
        return Math.sqrt(pos1.distanceSq(pos2));
    }

    public static void setTags() {
        LOGS = BlockTags.getCollection().get(LOGS_RESOURCELOCATION);
        LEAVES = BlockTags.getCollection().get(LEAVES_RESOURCELOCATION);
        GRASS = BlockTags.getCollection().get(GRASS_RESOURCELOCATION);
    }

}

/*******************************************************************************
 * HellFirePvP / Astral Sorcery 2019
 *
 * All rights reserved.
 * The source code is available on github: https://github.com/HellFirePvP/AstralSorcery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.astralsorcery.common.util.block;

import hellfirepvp.astralsorcery.common.util.MiscUtils;
import hellfirepvp.astralsorcery.common.util.data.Vector3;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: BlockDiscoverer
 * Created by HellFirePvP
 * Date: 25.08.2019 / 21:15
 */
public class BlockDiscoverer {

    public static Set<BlockPos> discoverBlocksWithSameStateAroundChain(World world, BlockPos origin, BlockState match, int length, @Nullable Direction originalBreakDirection, BlockPredicate addCheck) {
        Set<BlockPos> out = new HashSet<>();

        BlockPos offset = new BlockPos(origin);
        lbl: while (length > 0) {
            List<Direction> faces = new ArrayList<>();
            Collections.addAll(faces, Direction.values());
            if (originalBreakDirection != null && out.isEmpty()) {
                faces.remove(originalBreakDirection);
                faces.remove(originalBreakDirection.getOpposite());
            }
            Collections.shuffle(faces);
            for (Direction face : faces) {
                BlockPos at = offset.offset(face);
                if (out.contains(at)) {
                    continue;
                }
                BlockState test = world.getBlockState(at);
                if (BlockUtils.matchStateExact(match, test) && addCheck.test(world, at, test)) {
                    out.add(at);
                    length--;
                    offset = at;
                    continue lbl;
                }
            }
            break;
        }

        return out;
    }

    public static Set<BlockPos> searchForTileEntitiesAround(World world, BlockPos origin, int distance, Predicate<TileEntity> match) {
        Set<BlockPos> out = new HashSet<>();

        int minChX = (origin.getX() - distance) >> 4;
        int minChZ = (origin.getZ() - distance) >> 4;
        int maxChX = (origin.getX() + distance) >> 4;
        int maxChZ = (origin.getZ() + distance) >> 4;
        for (int chX = minChX; chX <= maxChX; chX++) {
            for (int chZ = minChZ; chZ <= maxChZ; chZ++) {
                Chunk ch = world.getChunk(chX, chZ);
                if (ch != null) {
                    out.addAll(
                            ch.getTileEntityMap()
                                    .values()
                                    .stream()
                                    .filter(tile -> tile.getPos().withinDistance(origin, distance))
                                    .filter(match)
                                    .map(TileEntity::getPos)
                                    .collect(Collectors.toList())
                    );
                }
            }
        }

        return out;
    }

    public static Set<BlockPos> searchForBlocksAround(World world, BlockPos origin, int cubeSize, BlockPredicate match) {
        Set<BlockPos> out = new HashSet<>();

        try (BlockPos.PooledMutableBlockPos offset = BlockPos.PooledMutableBlockPos.retain()) {
            for (int xx = -cubeSize; xx <= cubeSize; xx++) {
                for (int zz = -cubeSize; zz <= cubeSize; zz++) {
                    for (int yy = -cubeSize; yy <= cubeSize; yy++) {
                        offset.setPos(origin.getX() + xx, origin.getY() + yy, origin.getZ() + zz);
                        if (MiscUtils.isChunkLoaded(world, offset)) {
                            BlockState atState = world.getBlockState(offset);
                            if (match.test(world, offset, atState)) {
                                out.add(new BlockPos(offset));
                            }
                        }
                    }
                }
            }
        }
        return out;
    }

    @Nullable
    public static BlockPos searchAreaForFirst(World world, BlockPos center, int radius, @Nullable Vector3 offsetFrom, BlockPredicate acceptor) {
        for (int r = 0; r <= radius; r++) {
            Set<BlockPos> posList = new HashSet<>();
            for (int xx = -r; xx <= r; xx++) {
                for (int yy = -r; yy <= r; yy++) {
                    for (int zz = -r; zz <= r; zz++) {

                        BlockPos pos = center.add(xx, yy, zz);
                        if (MiscUtils.isChunkLoaded(world, new ChunkPos(pos))) {
                            BlockState state = world.getBlockState(pos);
                            if (acceptor.test(world, pos, state)) {
                                posList.add(pos);
                            }
                        }
                    }
                }
            }
            if (!posList.isEmpty()) {
                Vector3 offset = new Vector3(center).add(0.5, 0.5, 0.5);
                if (offsetFrom != null) {
                    offset = offsetFrom;
                }
                BlockPos closest = null;
                double prevDst = 0;
                for (BlockPos pos : posList) {
                    if (closest == null || offset.distance(pos) < prevDst) {
                        closest = pos;
                        prevDst = offset.distance(pos);
                    }
                }
                return closest;
            }
        }
        return null;
    }
}

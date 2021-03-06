package net.gegy1000.earth.server.world.cover.type;

import net.gegy1000.earth.server.world.cover.EarthCoverContext;
import net.gegy1000.earth.server.world.cover.EarthCoverType;
import net.gegy1000.earth.server.world.cover.EarthDecorationGenerator;
import net.gegy1000.earth.server.world.cover.LatitudinalZone;
import net.gegy1000.terrarium.server.world.cover.CoverSurfaceGenerator;
import net.gegy1000.terrarium.server.world.cover.CoverType;
import net.gegy1000.terrarium.server.world.feature.tree.GenerousTreeGenerator;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.Random;

public class BeachCover extends EarthCoverType {
    @Override
    public CoverSurfaceGenerator<EarthCoverContext> createSurfaceGenerator(EarthCoverContext context) {
        IBlockState state = Blocks.SAND.getDefaultState();
        return new CoverSurfaceGenerator.Static<>(context, this, state, state);
    }

    @Override
    public EarthDecorationGenerator createDecorationGenerator(EarthCoverContext context) {
        return new Decoration(context, this);
    }

    @Override
    public Biome getBiome(EarthCoverContext context, int x, int z) {
        return Biomes.BEACH;
    }

    private static class Decoration extends EarthDecorationGenerator {
        private Decoration(EarthCoverContext context, CoverType<EarthCoverContext> coverType) {
            super(context, coverType);
        }

        @Override
        public void decorate(int originX, int originZ, Random random) {
            LatitudinalZone zone = this.context.getZone(originX, originZ);
            if (zone == LatitudinalZone.TROPICS) {
                World world = this.context.getWorld();
                this.decorateScatter(random, originX, originZ, this.range(random, 0, 2), (pos, localX, localZ) -> {
                    int height = this.range(random, 6, 7);
                    new GenerousTreeGenerator(false, height, JUNGLE_LOG, JUNGLE_LEAF, true, true).generate(world, random, pos);
                });
            }
        }
    }
}

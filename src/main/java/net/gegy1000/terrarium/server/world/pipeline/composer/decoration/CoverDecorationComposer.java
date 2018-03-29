package net.gegy1000.terrarium.server.world.pipeline.composer.decoration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.gegy1000.terrarium.Terrarium;
import net.gegy1000.terrarium.server.capability.TerrariumWorldData;
import net.gegy1000.terrarium.server.world.chunk.PseudoRandomMap;
import net.gegy1000.terrarium.server.world.cover.CoverDecorationGenerator;
import net.gegy1000.terrarium.server.world.cover.CoverGenerationContext;
import net.gegy1000.terrarium.server.world.cover.CoverRegistry;
import net.gegy1000.terrarium.server.world.cover.CoverType;
import net.gegy1000.terrarium.server.world.json.InstanceJsonValueParser;
import net.gegy1000.terrarium.server.world.json.InstanceObjectParser;
import net.gegy1000.terrarium.server.world.json.TerrariumJsonUtils;
import net.gegy1000.terrarium.server.world.pipeline.component.RegionComponentType;
import net.gegy1000.terrarium.server.world.pipeline.source.tile.CoverRasterTileAccess;
import net.gegy1000.terrarium.server.world.region.GenerationRegionHandler;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class CoverDecorationComposer implements DecorationComposer {
    private static final long DECORATION_SEED = 2492037454623254033L;

    private final Random random;
    private final PseudoRandomMap coverMap;

    private final RegionComponentType<CoverRasterTileAccess> coverComponent;

    private final Map<CoverType<?>, CoverDecorationGenerator<?>> generators;
    private final Set<CoverType<?>> coverTypes = new HashSet<>();

    public CoverDecorationComposer(
            World world,
            RegionComponentType<CoverRasterTileAccess> coverComponent,
            Map<CoverType<?>, CoverDecorationGenerator<?>> generators
    ) {
        this.random = new Random(world.getSeed() ^ DECORATION_SEED);
        this.coverMap = new PseudoRandomMap(world.getSeed(), this.random.nextLong());

        this.coverComponent = coverComponent;
        this.generators = generators;
    }

    @Override
    public void decorateChunk(World world, GenerationRegionHandler regionHandler, int chunkX, int chunkZ) {
        int globalX = chunkX << 4;
        int globalZ = chunkZ << 4;

        CoverRasterTileAccess coverRaster = regionHandler.getCachedChunkRaster(this.coverComponent);

        this.coverTypes.clear();
        for (int localZ = 0; localZ < 16; localZ++) {
            for (int localX = 0; localX < 16; localX++) {
                this.coverTypes.add(coverRaster.get(localX, localZ));
            }
        }

        this.coverMap.initPosSeed(globalX, globalZ);
        long randomSeed = this.coverMap.next();

        for (CoverType<?> type : this.coverTypes) {
            CoverDecorationGenerator<?> generator = this.generators.get(type);
            if (generator != null) {
                this.random.setSeed(randomSeed);
                // TODO: Handle latitudinal zone
                generator.decorate(globalX + 8, globalZ + 8, this.random);
            }
        }
    }

    public static class Parser implements InstanceObjectParser<DecorationComposer> {
        @Override
        public DecorationComposer parse(TerrariumWorldData worldData, World world, InstanceJsonValueParser valueParser, JsonObject objectRoot) {
            RegionComponentType<CoverRasterTileAccess> coverComponent = valueParser.parseComponentType(objectRoot, "cover_component", CoverRasterTileAccess.class);

            Map<CoverType<?>, CoverDecorationGenerator<?>> generators = new HashMap<>();
            JsonObject coverTypeRoot = TerrariumJsonUtils.parseRemoteObject(objectRoot, "cover_types");

            CoverGenerationContext defaultContext = null;
            if (coverTypeRoot.has("default_context")) {
                defaultContext = valueParser.parseContext(coverTypeRoot, "default_context");
            }

            JsonArray coverTypeArray = TerrariumJsonUtils.parseRemoteArray(coverTypeRoot, "types");
            for (JsonElement element : coverTypeArray) {
                if (element.isJsonObject()) {
                    JsonObject coverObject = element.getAsJsonObject();
                    ResourceLocation coverKey = new ResourceLocation(JsonUtils.getString(coverObject, "type"));
                    CoverGenerationContext context = valueParser.parseContext(coverObject, "context");
                    this.putGenerator(generators, coverKey, context);
                } else if (TerrariumJsonUtils.isString(element)) {
                    if (defaultContext == null) {
                        throw new JsonSyntaxException("Cannot declare defaulted cover type with no default context!");
                    }
                    ResourceLocation coverKey = new ResourceLocation(element.getAsString());
                    this.putGenerator(generators, coverKey, defaultContext);
                } else {
                    Terrarium.LOGGER.warn("Ignored invalid cover type entry {}", element);
                }
            }

            return new CoverDecorationComposer(world, coverComponent, generators);
        }

        private void putGenerator(Map<CoverType<?>, CoverDecorationGenerator<?>> generators, ResourceLocation coverKey, CoverGenerationContext context) {
            CoverType<?> coverType = CoverRegistry.getCoverType(coverKey);
            if (coverType == null) {
                throw new JsonSyntaxException("Found unregistered cover type " + coverKey);
            }
            generators.put(coverType, this.createGenerator(coverType, context));
        }

        @SuppressWarnings("unchecked")
        private <T extends CoverGenerationContext> CoverDecorationGenerator<T> createGenerator(CoverType<T> coverType, CoverGenerationContext context) {
            if (coverType.getRequiredContext().isAssignableFrom(context.getClass())) {
                return coverType.createDecorationGenerator((T) context);
            } else {
                throw new JsonSyntaxException("Tried to apply context of wrong type to " + coverType);
            }
        }
    }
}

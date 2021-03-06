package net.gegy1000.terrarium.server.world.pipeline.layer;

import net.gegy1000.terrarium.server.world.pipeline.DataLayer;
import net.gegy1000.terrarium.server.world.pipeline.DataTileKey;
import net.gegy1000.terrarium.server.world.pipeline.DataView;
import net.gegy1000.terrarium.server.world.pipeline.source.tile.ShortRasterTile;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class ConstantShortProducer implements DataLayer<ShortRasterTile> {
    private final short value;

    public ConstantShortProducer(short value) {
        this.value = value;
    }

    @Override
    public ShortRasterTile apply(LayerContext context, DataView view) {
        short[] data = new short[view.getWidth() * view.getHeight()];
        Arrays.fill(data, this.value);
        return new ShortRasterTile(data, view.getWidth(), view.getHeight());
    }

    @Override
    public Collection<DataTileKey<?>> getRequiredData(LayerContext context, DataView view) {
        return Collections.emptyList();
    }
}

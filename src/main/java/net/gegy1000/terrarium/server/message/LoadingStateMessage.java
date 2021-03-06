package net.gegy1000.terrarium.server.message;

import io.netty.buffer.ByteBuf;
import net.gegy1000.terrarium.server.world.pipeline.source.LoadingState;
import net.gegy1000.terrarium.server.world.pipeline.source.LoadingStateHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class LoadingStateMessage implements IMessage {
	private LoadingState state;

    public LoadingStateMessage() {
    }

    public LoadingStateMessage(LoadingState state) {
        this.state = state;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        boolean hasState = buf.readBoolean();
        if (hasState) {
    		this.state = LoadingState.values()[buf.readInt()];
    	}
    }

    @Override
    public void toBytes(ByteBuf buf) {
        boolean hasState = this.state != null;
        buf.writeBoolean(hasState);
    	if (hasState) {
    		buf.writeInt(this.state.ordinal());
    	}
    }

    public static class Handler implements IMessageHandler<LoadingStateMessage, IMessage> {
        @Override
        public IMessage onMessage(LoadingStateMessage message, MessageContext ctx) {
            LoadingStateHandler.updateRemoteState(message.state);
            return null;
        }
    }
}

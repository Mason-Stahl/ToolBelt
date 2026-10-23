package dev.gigaherz.toolbelt.network;

import dev.gigaherz.toolbelt.ToolBelt;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client asks the server to swap the main-hand item with a belt slot.
 *
 * @param beltSlot player inventory slot holding the belt (as seen by the client)
 * @param swapWith belt slot to swap with, or {@link #INSERT} to insert into the first free/mergeable slot
 */
public record SwapItemsPayload(int beltSlot, int swapWith) implements CustomPacketPayload
{
    public static final int INSERT = -1;

    public static final Type<SwapItemsPayload> TYPE = new Type<>(ToolBelt.id("swap_items"));

    public static final StreamCodec<ByteBuf, SwapItemsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SwapItemsPayload::beltSlot,
            ByteBufCodecs.VAR_INT, SwapItemsPayload::swapWith,
            SwapItemsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}

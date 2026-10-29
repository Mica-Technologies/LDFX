package com.micatechnologies.minecraft.ldfx.mixin;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketChunkData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Rebuilds only the parts of a chunk a partial chunk packet changed.
 *
 * <p>The server sends a partial {@link SPacketChunkData} when 64 or more blocks change in one
 * chunk column during a tick (building, WorldEdit, machines), with a bitmask of the 16-block
 * sections it carries. Vanilla still marks the whole column for a render rebuild, which
 * {@code RenderGlobal} widens by a block on every side: 16 sections in each of 9 columns. Here
 * only the sections in the mask are marked (the one-block border is kept, so faces and smooth
 * lighting next to the changes stay right). Full chunks keep vanilla's whole-column marking: a
 * full chunk may have emptied sections the mask does not list.</p>
 */
@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPartialRebuild {

    @Redirect(method = "handleChunkData",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/client/multiplayer/WorldClient;markBlockRangeForRenderUpdate(IIIIII)V"))
    private void ldfx$markChangedSections(WorldClient world, int x1, int y1, int z1, int x2, int y2, int z2,
                                          SPacketChunkData packet) {
        int mask = packet.getExtractedSize();
        if (packet.isFullChunk() || mask == 0) {
            world.markBlockRangeForRenderUpdate(x1, y1, z1, x2, y2, z2);
            return;
        }
        // One call per run of consecutive sections.
        int section = 0;
        while (section < 16) {
            if ((mask & (1 << section)) == 0) {
                section++;
                continue;
            }
            int start = section;
            while (section < 16 && (mask & (1 << section)) != 0) {
                section++;
            }
            world.markBlockRangeForRenderUpdate(x1, start << 4, z1, x2, (section << 4) - 1, z2);
        }
    }
}

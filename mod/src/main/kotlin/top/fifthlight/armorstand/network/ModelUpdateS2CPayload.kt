package top.fifthlight.armorstand.network

import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import top.fifthlight.armorstand.util.ModelHash
import java.util.*
import kotlin.jvm.optionals.getOrNull

data class ModelUpdateS2CPayload(
    val modelHash: ModelHash?,
) : CustomPayload {
    companion object {
        private val PAYLOAD_ID = Identifier.of("armorstand", "model_update")
        val ID = CustomPayload.Id<ModelUpdateS2CPayload>(PAYLOAD_ID)
        val CODEC: PacketCodec<ByteBuf, ModelUpdateS2CPayload> = PacketCodecs.optional(ModelHash.CODEC).xmap(
            { ModelUpdateS2CPayload(it.getOrNull()) },
            { Optional.ofNullable(it.modelHash) },
        )
    }

    override fun getId() = ID
}
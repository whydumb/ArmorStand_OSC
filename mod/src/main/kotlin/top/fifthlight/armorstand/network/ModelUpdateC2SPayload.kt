package top.fifthlight.armorstand.network

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier

data class ModelUpdateC2SPayload(val path: String?): CustomPayload {
    companion object {
        private val PAYLOAD_ID = Identifier.of("armorstand", "model_update")
        val ID = CustomPayload.Id<ModelUpdateC2SPayload>(PAYLOAD_ID)
        val CODEC: PacketCodec<PacketByteBuf, ModelUpdateC2SPayload> = PacketCodec.of(ModelUpdateC2SPayload::write, ::ModelUpdateC2SPayload)
    }

    constructor(buf: PacketByteBuf): this(
        path = if (buf.readBoolean()) {
            buf.readString()
        } else {
            null
        },
    )

    fun write(buf: PacketByteBuf) {
        buf.writeBoolean(path != null)
        if (path != null) {
            buf.writeString(path)
        }
    }

    override fun getId() = ID
}
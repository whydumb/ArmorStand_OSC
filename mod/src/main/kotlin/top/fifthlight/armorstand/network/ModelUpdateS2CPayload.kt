package top.fifthlight.armorstand.network

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier

data class ModelUpdateS2CPayload(val path: String?): CustomPayload {
    companion object {
        private val PAYLOAD_ID = Identifier.of("armorstand", "model_update")
        val ID = CustomPayload.Id<ModelUpdateS2CPayload>(PAYLOAD_ID)
        val CODEC: PacketCodec<PacketByteBuf, ModelUpdateS2CPayload> = PacketCodec.of(ModelUpdateS2CPayload::write, ::ModelUpdateS2CPayload)
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
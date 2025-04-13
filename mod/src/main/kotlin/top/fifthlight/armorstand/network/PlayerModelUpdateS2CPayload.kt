package top.fifthlight.armorstand.network

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import java.util.*

data class PlayerModelUpdateS2CPayload(
    val uuid: UUID,
    val path: String?,
): CustomPayload {
    companion object {
        private val PAYLOAD_ID = Identifier.of("armorstand", "player_model_update")
        val ID = CustomPayload.Id<PlayerModelUpdateS2CPayload>(PAYLOAD_ID)
        val CODEC = PacketCodec.of(PlayerModelUpdateS2CPayload::write, ::PlayerModelUpdateS2CPayload)
    }

    constructor(buf: PacketByteBuf): this(
        uuid = buf.readUuid(),
        path = if (buf.readBoolean()) {
            buf.readString()
        } else {
            null
        },
    )

    fun write(buf: PacketByteBuf) {
        buf.writeUuid(uuid)
        buf.writeBoolean(path != null)
        if (path != null) {
            buf.writeString(path)
        }
    }

    override fun getId() = ID

}
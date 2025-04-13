package top.fifthlight.armorstand.server

import java.util.*

object ModelPathManager {
    private val _models = mutableMapOf<UUID, String>()
    var onUpdateListener: ((UUID, String?) -> Unit)? = null

    fun update(uuid: UUID, path: String?) {
        if (path == null) {
            _models.remove(uuid)
        } else {
            _models[uuid] = path
        }
        onUpdateListener?.let { it(uuid, path) }
    }

    fun getModels(): Map<UUID, String> = _models

    fun clear() {
        _models.clear()
    }
}
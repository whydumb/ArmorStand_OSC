package top.fifthlight.blazerod.model.resource

import org.joml.AxisAngle4f
import org.joml.Matrix4fc
import org.joml.Quaternionf
import org.joml.Vector3f
import top.fifthlight.blazerod.model.Camera

data class CameraTransform(
    val camera: Camera,
    val rotationQuaternion: Quaternionf = Quaternionf(),
    val rotationEuler: AxisAngle4f = AxisAngle4f(),
    val position: Vector3f = Vector3f(),
) {
    fun update(matrix: Matrix4fc) {
        matrix.getTranslation(position)
        matrix.getUnnormalizedRotation(rotationQuaternion)
        matrix.getRotation(rotationEuler)
    }
}

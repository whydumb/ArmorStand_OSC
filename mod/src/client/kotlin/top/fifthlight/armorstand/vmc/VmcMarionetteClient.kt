package top.fifthlight.armorstand.vmc

import com.illposed.osc.OSCBundle
import com.illposed.osc.OSCMessage
import com.illposed.osc.OSCPacket
import com.illposed.osc.transport.Transport
import com.illposed.osc.transport.udp.UDPTransport
import kotlinx.coroutines.*
import net.minecraft.client.MinecraftClient
import org.slf4j.LoggerFactory
import top.fifthlight.armorstand.util.ThreadExecutorDispatcher
import top.fifthlight.blazerod.model.Expression
import top.fifthlight.blazerod.model.HumanoidTag
import kotlin.math.sqrt

class VmcMarionetteClient(
    private val transport: Transport,
) : AutoCloseable {
    companion object {
        private val logger = LoggerFactory.getLogger(VmcMarionetteClient::class.java)
        
        // 검증 상수들
        private const val MAX_POSITION_RANGE = 100f
        private const val MIN_QUATERNION_LENGTH = 0.001f
        private const val MAX_BLEND_VALUE = 1f
        private const val MIN_BLEND_VALUE = 0f
    }

    private val scope = CoroutineScope(ThreadExecutorDispatcher(MinecraftClient.getInstance()) + SupervisorJob())

    @Volatile
    private var closed = false

    private val stateHolder: VmcMarionetteStateHolder = VmcMarionetteStateHolder()

    fun getState() = stateHolder.read()

    private fun handlePacket(message: OSCPacket) {
        when (message) {
            is OSCBundle -> {
                message.packets.forEach(::handlePacket)
            }

            is OSCMessage -> handleMessage(message)
        }
    }

    // 검증 헬퍼 함수들
    private fun isValidFloat(value: Float): Boolean {
        return value.isFinite() && !value.isNaN()
    }
    
    private fun validateAndClampPosition(pos: Float): Float? {
        if (!isValidFloat(pos)) {
            return null
        }
        return pos.coerceIn(-MAX_POSITION_RANGE, MAX_POSITION_RANGE)
    }
    
    private fun validateAndNormalizeQuaternion(
        rotX: Float, rotY: Float, rotZ: Float, rotW: Float
    ): FloatArray? {
        // 모든 회전 값이 유효한지 확인
        if (!isValidFloat(rotX) || !isValidFloat(rotY) || 
            !isValidFloat(rotZ) || !isValidFloat(rotW)) {
            return null
        }
        
        // Quaternion 크기 계산
        val qLength = sqrt(rotX*rotX + rotY*rotY + rotZ*rotZ + rotW*rotW)
        
        if (!isValidFloat(qLength) || qLength < MIN_QUATERNION_LENGTH) {
            logger.warn("Invalid quaternion length: {}", qLength)
            return null
        }
        
        // 정규화
        return floatArrayOf(
            rotX / qLength,
            rotY / qLength,
            rotZ / qLength,
            rotW / qLength
        )
    }
    
    private fun validateBlendValue(value: Float): Float? {
        if (!isValidFloat(value)) {
            return null
        }
        return value.coerceIn(MIN_BLEND_VALUE, MAX_BLEND_VALUE)
    }

    private val pendingBlendValues = mutableMapOf<Expression.Tag, Float>()
    
    private fun handleMessage(message: OSCMessage) {
        when (message.address) {
            "/VMC/Ext/Root/Pos" -> {
                val name = (message.arguments.getOrNull(0) as? String) ?: return
                if (name != "root") {
                    return
                }
                
                // 인수 추출 및 타입 확인
                val posX = (message.arguments.getOrNull(1) as? Float) ?: run {
                    logger.warn("Invalid or missing posX in root transform")
                    return
                }
                val posY = (message.arguments.getOrNull(2) as? Float) ?: run {
                    logger.warn("Invalid or missing posY in root transform")
                    return
                }
                val posZ = (message.arguments.getOrNull(3) as? Float) ?: run {
                    logger.warn("Invalid or missing posZ in root transform")
                    return
                }
                val rotX = (message.arguments.getOrNull(4) as? Float) ?: run {
                    logger.warn("Invalid or missing rotX in root transform")
                    return
                }
                val rotY = (message.arguments.getOrNull(5) as? Float) ?: run {
                    logger.warn("Invalid or missing rotY in root transform")
                    return
                }
                val rotZ = (message.arguments.getOrNull(6) as? Float) ?: run {
                    logger.warn("Invalid or missing rotZ in root transform")
                    return
                }
                val rotW = (message.arguments.getOrNull(7) as? Float) ?: run {
                    logger.warn("Invalid or missing rotW in root transform")
                    return
                }
                
                // 위치 검증 및 클램핑
                val validatedPosX = validateAndClampPosition(posX) ?: run {
                    logger.warn("Invalid root position X: {}", posX)
                    return
                }
                val validatedPosY = validateAndClampPosition(posY) ?: run {
                    logger.warn("Invalid root position Y: {}", posY)
                    return
                }
                val validatedPosZ = validateAndClampPosition(posZ) ?: run {
                    logger.warn("Invalid root position Z: {}", posZ)
                    return
                }
                
                // Quaternion 검증 및 정규화
                val normalizedRotation = validateAndNormalizeQuaternion(rotX, rotY, rotZ, rotW) ?: run {
                    logger.warn("Invalid root rotation quaternion: ({}, {}, {}, {})", rotX, rotY, rotZ, rotW)
                    return
                }
                
                stateHolder.write {
                    setRootTransform(
                        validatedPosX, validatedPosY, validatedPosZ,
                        normalizedRotation[0], normalizedRotation[1], 
                        normalizedRotation[2], normalizedRotation[3]
                    )
                }
            }

            "/VMC/Ext/Bone/Pos" -> {
                val name = (message.arguments.getOrNull(0) as? String) ?: run {
                    logger.warn("Missing bone name")
                    return
                }
                
                val tag = HumanoidTag.fromVrmName(name.replaceFirstChar { it.lowercase() }) ?: run {
                    logger.debug("Unknown bone name: {}", name)
                    return
                }
                
                // 인수 추출 및 타입 확인
                val posX = (message.arguments.getOrNull(1) as? Float) ?: run {
                    logger.warn("Invalid or missing posX for bone {}", name)
                    return
                }
                val posY = (message.arguments.getOrNull(2) as? Float) ?: run {
                    logger.warn("Invalid or missing posY for bone {}", name)
                    return
                }
                val posZ = (message.arguments.getOrNull(3) as? Float) ?: run {
                    logger.warn("Invalid or missing posZ for bone {}", name)
                    return
                }
                val rotX = (message.arguments.getOrNull(4) as? Float) ?: run {
                    logger.warn("Invalid or missing rotX for bone {}", name)
                    return
                }
                val rotY = (message.arguments.getOrNull(5) as? Float) ?: run {
                    logger.warn("Invalid or missing rotY for bone {}", name)
                    return
                }
                val rotZ = (message.arguments.getOrNull(6) as? Float) ?: run {
                    logger.warn("Invalid or missing rotZ for bone {}", name)
                    return
                }
                val rotW = (message.arguments.getOrNull(7) as? Float) ?: run {
                    logger.warn("Invalid or missing rotW for bone {}", name)
                    return
                }
                
                // 위치 검증 및 클램핑
                val validatedPosX = validateAndClampPosition(posX) ?: run {
                    logger.warn("Invalid position X for bone {}: {}", name, posX)
                    return
                }
                val validatedPosY = validateAndClampPosition(posY) ?: run {
                    logger.warn("Invalid position Y for bone {}: {}", name, posY)
                    return
                }
                val validatedPosZ = validateAndClampPosition(posZ) ?: run {
                    logger.warn("Invalid position Z for bone {}: {}", name, posZ)
                    return
                }
                
                // Quaternion 검증 및 정규화
                val normalizedRotation = validateAndNormalizeQuaternion(rotX, rotY, rotZ, rotW) ?: run {
                    logger.warn("Invalid rotation quaternion for bone {}: ({}, {}, {}, {})", name, rotX, rotY, rotZ, rotW)
                    return
                }
                
                stateHolder.write {
                    setBoneTransform(
                        tag,
                        validatedPosX, validatedPosY, validatedPosZ,
                        normalizedRotation[0], normalizedRotation[1], 
                        normalizedRotation[2], normalizedRotation[3]
                    )
                }
            }

            "/VMC/Ext/Blend/Val" -> {
                val tag = (message.arguments.getOrNull(0) as? String) ?: run {
                    logger.warn("Missing blend shape name")
                    return
                }
                
                val expressionTag = Expression.Tag.fromVrm0Name(tag) 
                    ?: Expression.Tag.fromVrm1Name(tag) ?: run {
                    logger.debug("Unknown blend shape: {}", tag)
                    return
                }
                
                val value = (message.arguments.getOrNull(1) as? Float) ?: run {
                    logger.warn("Invalid or missing value for blend shape {}", tag)
                    return
                }
                
                val validatedValue = validateBlendValue(value) ?: run {
                    logger.warn("Invalid blend value for {}: {}", tag, value)
                    return
                }
                
                pendingBlendValues[expressionTag] = validatedValue
            }

            "/VMC/Ext/Blend/Apply" -> {
                if (pendingBlendValues.isNotEmpty()) {
                    stateHolder.write {
                        pendingBlendValues.forEach { (tag, value) ->
                            setBlendShape(tag, value)
                        }
                    }
                    pendingBlendValues.clear()
                } else {
                    logger.debug("Blend/Apply received with no pending values")
                }
            }
            
            else -> {
                // 알 수 없는 메시지는 디버그 레벨로 로깅
                logger.debug("Unknown VMC message: {}", message.address)
            }
        }
    }

    init {
        if (transport !is UDPTransport) {
            transport.connect()
        }
        require(transport.isBlocking) { "Require blocking transport" }
        scope.launch(Dispatchers.IO) {
            try {
                while (true) {
                    val message = try {
                        transport.receive()
                    } catch (ex: Exception) {
                        if (!scope.isActive) {
                            // throw CancellationException
                            ensureActive()
                        }
                        logger.warn("Failed to receive OSC message", ex)
                        continue
                    }
                    handlePacket(message)
                    ensureActive()
                }
            } finally {
                if (transport !is UDPTransport) {
                    transport.disconnect()
                }
                transport.close()
            }
        }
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true
        scope.cancel()
    }
}

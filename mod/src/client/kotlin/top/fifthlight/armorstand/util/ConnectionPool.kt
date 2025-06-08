package top.fifthlight.armorstand.util

import top.fifthlight.blazerod.util.Pool
import java.sql.Connection

inline fun <T> Pool<Connection>.transaction(crossinline block: Connection.() -> T): T {
    val connection = acquire()
    try {
        val result = block(connection)
        try {
            connection.commit()
        } catch (ex: Throwable) {
            // will be caught by outer try block
            throw ex
        }
        return result
    } catch (ex: Throwable) {
        try {
            connection.rollback()
        } catch (rollbackEx: Throwable) {
            ex.addSuppressed(rollbackEx)
        }
        throw ex
    } finally {
        // Must pair acquire() and release(), even if coroutine is cancelled or exception thrown
        release(connection)
    }
}

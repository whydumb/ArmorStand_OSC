package top.fifthlight.armorstand.util

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.sql.Connection

suspend inline fun <T> Pool<Connection>.transaction(crossinline block: suspend Connection.() -> T): T {
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
        withContext(NonCancellable) {
            release(connection)
        }
    }
}

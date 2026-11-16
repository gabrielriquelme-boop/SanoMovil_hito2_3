package cl.sanomovil.backend.repository

import cl.sanomovil.backend.config.Config
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.ResultSet

/**
 * Pool de conexiones a PostgreSQL y manejo de transacciones.
 * Cada operación de negocio corre dentro de UNA transacción: si algo falla, se deshace todo.
 */
class BaseDeDatos(config: Config) : AutoCloseable {
    private val fuente = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = config.dbUrl
            username = config.dbUsuario
            password = config.dbClave
            maximumPoolSize = 8
            poolName = "sano-movil-pool"
        },
    )

    fun <T> transaccion(bloque: (Connection) -> T): T =
        fuente.connection.use { conexion ->
            conexion.autoCommit = false
            try {
                val resultado = bloque(conexion)
                conexion.commit()
                resultado
            } catch (e: Throwable) {
                conexion.rollback()
                throw e
            }
        }

    override fun close() = fuente.close()
}

// ---------- Ayudantes JDBC: SIEMPRE consultas parametrizadas (evita inyección SQL) ----------

fun <T> Connection.consultar(sql: String, vararg parametros: Any?, mapear: (ResultSet) -> T): List<T> =
    prepareStatement(sql).use { ps ->
        parametros.forEachIndexed { i, p -> ps.setObject(i + 1, p) }
        ps.executeQuery().use { rs ->
            val filas = mutableListOf<T>()
            while (rs.next()) filas.add(mapear(rs))
            filas
        }
    }

fun Connection.ejecutar(sql: String, vararg parametros: Any?): Int =
    prepareStatement(sql).use { ps ->
        parametros.forEachIndexed { i, p -> ps.setObject(i + 1, p) }
        ps.executeUpdate()
    }

/** Lee una columna entera que puede ser NULL. */
fun ResultSet.enteroONulo(columna: String): Int? {
    val valor = getInt(columna)
    return if (wasNull()) null else valor
}

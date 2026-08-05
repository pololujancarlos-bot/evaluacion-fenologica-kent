package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "evaluaciones_kent")
data class EvaluationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val fecha: String,                  // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val tecnico: String,                // Evaluator name
    val campana: String,                // Campaña e.g., "2025-2026"
    val sector: String,                 // Sector
    val turno: String,                  // Turno
    val lote: String,                   // Lote
    val valvula: String,                // Válvula
    val variedad: String = "Kent",      // Fixed Kent
    val planta: String,                 // P1, P2, P3, P4
    
    // Morphological & Maturation metrics
    val tipoHoja: String,               // Joven, Madura, Senescente
    val durezaHoja: String,             // Tierna, Intermedia, Dura
    val longitudBrote: Double,          // cm
    
    // Bud & Tinting metrics
    val tinturacionYemas: String,       // "0%", "25%", "50%", "75%", "100%" or custom
    val tinturacionPorcentaje: Double,  // Numeric 0..100
    val yemaApta: Boolean,              // Sí / No
    val yemaHinchada: Double,           // % or count
    val yemaPicoLoro: Double,           // % or count
    val yemasAbiertas: Double,          // % or count
    val yemasCerradas: Double,          // % or count
    val yemaVegetativa: Double,         // % or count
    val yemaFloral: Double,             // % or count
    
    // Inflorescence & Fruit metrics
    val emergenciaPanicula: Double,     // cm
    val paniculasPorM2: Double,         // count
    val longitudPanicula: Double,       // cm
    val florAbierta: Double,            // %
    val cuajado: Double,                // %
    val frutosPorPanicula: Double,      // count
    
    // Attachments & Notes
    val foto1Uri: String? = null,
    val foto2Uri: String? = null,
    val foto3Uri: String? = null,
    val observaciones: String = "",
    
    // Sync Metadata
    val syncStatus: String = "PENDIENTE", // "PENDIENTE" or "SINCRONIZADO"
    val remoteId: String? = null,
    val lastSyncTimestamp: Long? = null
)

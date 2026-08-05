package com.example.domain

import com.example.data.local.EvaluationEntity

enum class NivelInduccion {
    BAJA, MEDIA, AVANZADA
}

enum class NivelTinturacion {
    BAJA, MODERADA, ALTA
}

enum class EstadoMaduracion {
    EN_PROCESO, AVANZADA, COMPLETA
}

data class AgronomicSummary(
    val estadoMaduracion: EstadoMaduracion,
    val descripcionMaduracion: String,
    
    val nivelInduccion: NivelInduccion,
    val porcentajeInduccionTotal: Double,
    val descripcionInduccion: String,
    
    val nivelTinturacion: NivelTinturacion,
    val porcentajeTinturacionPromedio: Double,
    val descripcionTinturacion: String,
    
    val relacionInduccionTinturacion: String,
    val recomendacionManejo: String
)

data class FieldComparisonResult(
    val loteName: String,
    val sectorName: String,
    val totalMuestras: Int,
    val induccionPromedio: Double,
    val tinturacionPromedio: Double,
    val nivelInduccion: NivelInduccion,
    val avanzaEnInduccion: Boolean,
    val tendenciaEstado: String
)

object AgronomicAnalyzer {

    fun analyzeRecord(record: EvaluationEntity): AgronomicSummary {
        // 1. Maduración del Brote / Hoja
        val dureza = record.durezaHoja.lowercase()
        val tipoHoja = record.tipoHoja.lowercase()
        val longitudBrote = record.longitudBrote

        val estadoMaduracion = when {
            dureza.contains("dura") || tipoHoja.contains("senescente") || (dureza.contains("intermedia") && longitudBrote > 15.0) ->
                EstadoMaduracion.AVANZADA
            dureza.contains("intermedia") || tipoHoja.contains("madura") ->
                EstadoMaduracion.EN_PROCESO
            else ->
                EstadoMaduracion.EN_PROCESO
        }

        val descMaduracion = when (estadoMaduracion) {
            EstadoMaduracion.AVANZADA -> "Brote maduro con dureza de hoja consolidada (${record.durezaHoja}). Apto para inducción o respuesta floral."
            EstadoMaduracion.EN_PROCESO -> "Brote en proceso de sazonado/maduración (${record.durezaHoja}, ${record.longitudBrote} cm). Requiere seguimiento antes de estimulación."
            EstadoMaduracion.COMPLETA -> "Madurez total del tejido foliar. Estado ideal para reposo de yemas pre-floración."
        }

        // 2. Nivel de Inducción Floral
        // Calculates floral weight based on yema hinchada, pico loro, yema floral, panículas
        val pesoFloral = (record.yemaHinchada * 0.25) +
                (record.yemaPicoLoro * 0.40) +
                (record.yemaFloral * 0.35) +
                (if (record.emergenciaPanicula > 0) 20.0 else 0.0)

        val porcentajeInduccion = pesoFloral.coerceIn(0.0, 100.0)

        val nivelInduccion = when {
            porcentajeInduccion >= 50.0 || record.yemaPicoLoro >= 40.0 || record.emergenciaPanicula > 2.0 -> NivelInduccion.AVANZADA
            porcentajeInduccion >= 25.0 || record.yemaHinchada >= 30.0 -> NivelInduccion.MEDIA
            else -> NivelInduccion.BAJA
        }

        val descInduccion = when (nivelInduccion) {
            NivelInduccion.AVANZADA -> "Inducción floral avanzada (Pico loro: ${record.yemaPicoLoro}%, Emergencia panícula: ${record.emergenciaPanicula} cm). Alta receptividad a floración."
            NivelInduccion.MEDIA -> "Inducción floral media en desarrollo (Yema hinchada: ${record.yemaHinchada}%). Yemas respondiendo a estímulo inductivo."
            NivelInduccion.BAJA -> "Inducción floral baja/incipiente. Domina yema cerrada o vegetativa. Continuar monitoreo de madurez de brote."
        }

        // 3. Nivel de Tinturación de Yemas
        val tinturacionVal = record.tinturacionPorcentaje
        val nivelTinturacion = when {
            tinturacionVal >= 60.0 || record.tinturacionYemas.contains("Alta") -> NivelTinturacion.ALTA
            tinturacionVal >= 25.0 || record.tinturacionYemas.contains("Moderada") -> NivelTinturacion.MODERADA
            else -> NivelTinturacion.BAJA
        }

        val descTinturacion = when (nivelTinturacion) {
            NivelTinturacion.ALTA -> "Tinturación intensa de yemas (${record.tinturacionYemas}). Pigmentación característica indicadora de actividad hormonal previa a brotación."
            NivelTinturacion.MODERADA -> "Tinturación moderada (${record.tinturacionYemas}). Inicio de diferenciación en la base de la yema apical."
            NivelTinturacion.BAJA -> "Tinturación leve o ausente (${record.tinturacionYemas}). Yema en estado verdoso o latente."
        }

        // 4. Relación Inducción Floral vs Tinturación
        val relacion = analyzeCorrelation(porcentajeInduccion, tinturacionVal, record)

        // 5. Recomendación Agronómica
        val recomendacion = generateAgronomicRecommendation(estadoMaduracion, nivelInduccion, nivelTinturacion)

        return AgronomicSummary(
            estadoMaduracion = estadoMaduracion,
            descripcionMaduracion = descMaduracion,
            nivelInduccion = nivelInduccion,
            porcentajeInduccionTotal = porcentajeInduccion,
            descripcionInduccion = descInduccion,
            nivelTinturacion = nivelTinturacion,
            porcentajeTinturacionPromedio = tinturacionVal,
            descripcionTinturacion = descTinturacion,
            relacionInduccionTinturacion = relacion,
            recomendacionManejo = recomendacion
        )
    }

    private fun analyzeCorrelation(
        porcentajeInduccion: Double,
        porcentajeTinturacion: Double,
        record: EvaluationEntity
    ): String {
        val diff = Math.abs(porcentajeInduccion - porcentajeTinturacion)
        return when {
            porcentajeInduccion >= 40.0 && porcentajeTinturacion >= 40.0 ->
                "CORRELACIÓN ALTA POSITIVA: Se observa coincidencia clara entre la tinturación de yemas (${porcentajeTinturacion.toInt()}%) y la respuesta floral (${porcentajeInduccion.toInt()}%). La acumulación de pigmentos acompaña la evolución a yema pico loro."
            
            porcentajeTinturacion >= 50.0 && porcentajeInduccion < 30.0 ->
                "TINTURACIÓN PRECURSORA: Alta tinturación de yema (${porcentajeTinturacion.toInt()}%) previa a la emergencia visible de yema pico loro (${record.yemaPicoLoro}%). Indica que la planta está iniciando respuesta bioquímica interna antes de la diferenciación física."
            
            porcentajeInduccion >= 50.0 && porcentajeTinturacion < 30.0 ->
                "EMERGENCIA SIN TINTURACIÓN PROFERIDA: Apertura floral o brotación directa con baja tinturación previa (${porcentajeTinturacion.toInt()}%). Posible respuesta rápida por estrés hídrico o fluctuación de temperatura nocturna."
            
            else ->
                "RELACIÓN INCIPIENTE / MODERADA: Tinturación (${porcentajeTinturacion.toInt()}%) e inducción (${porcentajeInduccion.toInt()}%) en niveles iniciales de avance. Monitorear evolución semanal."
        }
    }

    private fun generateAgronomicRecommendation(
        maduracion: EstadoMaduracion,
        induccion: NivelInduccion,
        tinturacion: NivelTinturacion
    ): String {
        return when {
            induccion == NivelInduccion.AVANZADA ->
                "Mantener riego equilibrado de sostén. Evitar aplicaciones nitrogenadas para asegurar cuajado de panícula."
            maduracion == EstadoMaduracion.AVANZADA && induccion == NivelInduccion.BAJA ->
                "Lote listo para inducción floral. Considerar aplicaciones foliares de potasio/nitrato de potasio según programa fitosanitario."
            tinturacion == NivelTinturacion.ALTA ->
                "Yemas en pico de tinte. Programar evaluación rápida a los 5 días para detectar emergencia de panícula."
            else ->
                "Continuar monitoreo rutinario cada 7 días en plantas marcadas (P1-P4)."
        }
    }

    fun compareFields(records: List<EvaluationEntity>): List<FieldComparisonResult> {
        if (records.isEmpty()) return emptyList()

        val grouped = records.groupBy { "${it.sector} - ${it.lote}" }

        return grouped.map { (loteKey, list) ->
            val totalMuestras = list.size
            val sectorName = list.firstOrNull()?.sector ?: "Sector"
            val loteName = list.firstOrNull()?.lote ?: "Lote"

            val avgInduccion = list.map { record ->
                (record.yemaHinchada * 0.25) + (record.yemaPicoLoro * 0.40) + (record.yemaFloral * 0.35)
            }.average()

            val avgTinturacion = list.map { it.tinturacionPorcentaje }.average()

            val nivelInduccion = when {
                avgInduccion >= 45.0 -> NivelInduccion.AVANZADA
                avgInduccion >= 20.0 -> NivelInduccion.MEDIA
                else -> NivelInduccion.BAJA
            }

            // Check progression over time
            val sortedByTime = list.sortedBy { it.timestamp }
            val avanza = if (sortedByTime.size >= 2) {
                val oldest = sortedByTime.first()
                val newest = sortedByTime.last()
                (newest.yemaPicoLoro + newest.yemaFloral) >= (oldest.yemaPicoLoro + oldest.yemaFloral)
            } else {
                avgInduccion > 15.0
            }

            val tendencia = if (avanza) "Avanzando activamente" else "Estacionario / En reposo"

            FieldComparisonResult(
                loteName = loteName,
                sectorName = sectorName,
                totalMuestras = totalMuestras,
                induccionPromedio = avgInduccion,
                tinturacionPromedio = avgTinturacion,
                nivelInduccion = nivelInduccion,
                avanzaEnInduccion = avanza,
                tendenciaEstado = tendencia
            )
        }.sortedByDescending { it.induccionPromedio }
    }
}

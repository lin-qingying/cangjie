package org.cangnova.cangjie.lsp.analysis

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.cangnova.cangjie.lsp.state.LspTextDocument

/**
 * Stable identity carried by a published Cangjie diagnostic for the later codeAction request.
 */
internal data class CangjieLspDiagnosticData(
    val documentUri: String,
    val documentVersion: Int,
    val diagnosticClass: String,
    val factoryName: String,
    val severityName: String,
    val psiStartOffset: Int,
    val psiEndOffset: Int,
) {
    fun matches(document: LspTextDocument, diagnostic: CaDiagnosticWithPsi<*>): Boolean {
        val psiRange = diagnostic.psi.textRange
        return documentUri == document.uri &&
            documentVersion == document.version &&
            diagnosticClass == diagnostic.javaClass.name &&
            factoryName == diagnostic.factoryName &&
            severityName == diagnostic.severity.name &&
            psiStartOffset == (psiRange?.startOffset ?: NO_PSI_RANGE) &&
            psiEndOffset == (psiRange?.endOffset ?: NO_PSI_RANGE)
    }

    fun toJson(): JsonObject = JsonObject().apply {
        addProperty(KEY_DOCUMENT_URI, documentUri)
        addProperty(KEY_DOCUMENT_VERSION, documentVersion)
        addProperty(KEY_DIAGNOSTIC_CLASS, diagnosticClass)
        addProperty(KEY_FACTORY_NAME, factoryName)
        addProperty(KEY_SEVERITY, severityName)
        addProperty(KEY_PSI_START, psiStartOffset)
        addProperty(KEY_PSI_END, psiEndOffset)
    }

    companion object {
        private const val KEY_DOCUMENT_URI = "documentUri"
        private const val KEY_DOCUMENT_VERSION = "documentVersion"
        private const val KEY_DIAGNOSTIC_CLASS = "diagnosticClass"
        private const val KEY_FACTORY_NAME = "factoryName"
        private const val KEY_SEVERITY = "severity"
        private const val KEY_PSI_START = "psiStartOffset"
        private const val KEY_PSI_END = "psiEndOffset"

        private const val NO_PSI_RANGE = -1

        fun create(document: LspTextDocument, diagnostic: CaDiagnosticWithPsi<*>): CangjieLspDiagnosticData {
            val psiRange = diagnostic.psi.textRange
            return CangjieLspDiagnosticData(
                documentUri = document.uri,
                documentVersion = document.version,
                diagnosticClass = diagnostic.javaClass.name,
                factoryName = diagnostic.factoryName,
                severityName = diagnostic.severity.name,
                psiStartOffset = psiRange?.startOffset ?: NO_PSI_RANGE,
                psiEndOffset = psiRange?.endOffset ?: NO_PSI_RANGE,
            )
        }

        fun parse(value: Any?): CangjieLspDiagnosticData? {
            val element = when (value) {
                is JsonElement -> value
                is Map<*, *> -> Gson().toJsonTree(value)
                else -> null
            } ?: return null
            if (!element.isJsonObject) return null
            val obj = element.asJsonObject
            return CangjieLspDiagnosticData(
                documentUri = obj.string(KEY_DOCUMENT_URI) ?: return null,
                documentVersion = obj.int(KEY_DOCUMENT_VERSION) ?: return null,
                diagnosticClass = obj.string(KEY_DIAGNOSTIC_CLASS) ?: return null,
                factoryName = obj.string(KEY_FACTORY_NAME) ?: return null,
                severityName = obj.string(KEY_SEVERITY) ?: return null,
                psiStartOffset = obj.int(KEY_PSI_START) ?: return null,
                psiEndOffset = obj.int(KEY_PSI_END) ?: return null,
            )
        }

        private fun JsonObject.string(key: String): String? =
            get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

        private fun JsonObject.int(key: String): Int? =
            get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
                ?.let { element -> runCatching { element.asInt }.getOrNull() }
    }
}

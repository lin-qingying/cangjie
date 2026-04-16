package org.cangnova.cangjie.analysis.api.types

interface CaErrorType : CaType {
    val errorMessage: String

    val presentableText: String?

    override fun createPointer(): CaTypePointer<CaErrorType>
}

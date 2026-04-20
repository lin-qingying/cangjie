package org.cangnova.cangjie.analysis.api.types

abstract class CaUsualClassType : CaClassLikeType() {
    abstract override fun createPointer(): CaTypePointer<CaUsualClassType>
}

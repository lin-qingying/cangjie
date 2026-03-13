package org.cangnova.cangjie.cfir.declarations.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirPureAbstractElement
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility

open class CfirDeclarationStatusImpl(
    override val visibility: Visibility = Visibilities.Public,
    override val modality: Modality? = null,
) : CfirPureAbstractElement(), CfirDeclarationStatus {
    override val source: CjSourceElement? get() = null

    protected var flags: Int = 0

    @CfirImplementationDetail
    internal val rawFlags: Int get() = flags

    private operator fun get(modifier: Modifier): Boolean = (flags and modifier.mask) != 0

    private operator fun set(modifier: Modifier, value: Boolean) {
        flags = if (value) {
            flags or modifier.mask
        } else {
            flags and modifier.mask.inv()
        }
    }

    override var isOverride: Boolean
        get() = this[Modifier.OVERRIDE]
        set(value) {
            this[Modifier.OVERRIDE] = value
        }

    override var isOperator: Boolean
        get() = this[Modifier.OPERATOR]
        set(value) {
            this[Modifier.OPERATOR] = value
        }

    override var isStatic: Boolean
        get() = this[Modifier.STATIC]
        set(value) {
            this[Modifier.STATIC] = value
        }

    override var isConst: Boolean
        get() = this[Modifier.CONST]
        set(value) {
            this[Modifier.CONST] = value
        }

    override var isMut: Boolean
        get() = this[Modifier.MUT]
        set(value) {
            this[Modifier.MUT] = value
        }

    override var isUnsafe: Boolean
        get() = this[Modifier.UNSAFE]
        set(value) {
            this[Modifier.UNSAFE] = value
        }

    override var isForeign: Boolean
        get() = this[Modifier.FOREIGN]
        set(value) {
            this[Modifier.FOREIGN] = value
        }

    override var isCommon: Boolean
        get() = this[Modifier.COMMON]
        set(value) {
            this[Modifier.COMMON] = value
        }

    override var isSpecific: Boolean
        get() = this[Modifier.SPECIFIC]
        set(value) {
            this[Modifier.SPECIFIC] = value
        }

    override var isRedef: Boolean
        get() = this[Modifier.REDEF]
        set(value) {
            this[Modifier.REDEF] = value
        }

    override var isAbstract: Boolean
        get() = this[Modifier.ABSTRACT]
        set(value) {
            this[Modifier.ABSTRACT] = value
        }

    override var isOpen: Boolean
        get() = this[Modifier.OPEN]
        set(value) {
            this[Modifier.OPEN] = value
        }

    override var isSealed: Boolean
        get() = this[Modifier.SEALED]
        set(value) {
            this[Modifier.SEALED] = value
        }

    enum class Modifier(val mask: Int) {
        OVERRIDE(0x1),
        OPERATOR(0x2),
        STATIC(0x4),
        CONST(0x8),
        MUT(0x10),
        UNSAFE(0x20),
        FOREIGN(0x40),
        COMMON(0x80),
        SPECIFIC(0x100),
        REDEF(0x200),
        ABSTRACT(0x400),
        OPEN(0x800),
        SEALED(0x1000),
    }

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirElement {
        return this
    }

    companion object {
        val DEFAULT = CfirDeclarationStatusImpl()
    }
}

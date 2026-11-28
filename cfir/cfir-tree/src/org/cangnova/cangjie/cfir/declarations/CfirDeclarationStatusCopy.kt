package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl

/**
 * 复制声明状态的接口事实，返回独立可变 raw 状态。
 * 供字段描述及模式绑定使用；不复制 resolved 状态的阶段身份，也不共享实现中的可变 flags。
 */
fun cloneDeclarationStatus(status: CfirDeclarationStatus): CfirDeclarationStatusImpl =
    CfirDeclarationStatusImpl(status.visibility, status.modality).also { copied ->
        copied.visibilityExplicitness = status.visibilityExplicitness
        copied.modalityExplicitness = status.modalityExplicitness
        copied.bodyPresence = status.bodyPresence
        copied.isDeclarationFile = status.isDeclarationFile
        copied.isAbstractExplicit = status.isAbstractExplicit
        copied.isOverride = status.isOverride
        copied.isOperator = status.isOperator
        copied.isStatic = status.isStatic
        copied.isConst = status.isConst
        copied.isMut = status.isMut
        copied.isUnsafe = status.isUnsafe
        copied.isForeign = status.isForeign
        copied.isC = status.isC
        copied.isCommon = status.isCommon
        copied.isFromCommonPart = status.isFromCommonPart
        copied.isSpecific = status.isSpecific
        copied.isCommonWithDefault = status.isCommonWithDefault
        copied.isRedef = status.isRedef
        copied.isDefault = status.isDefault
        copied.isAbstract = status.isAbstract
        copied.isOpen = status.isOpen
        copied.isSealed = status.isSealed
    }

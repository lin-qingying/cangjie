package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportBinding
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportTarget
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 返回当前限定名的父限定名；顶层短名的父节点统一折叠为根包。
 *
 * 导入解析会用该结果定位包前缀，不能让空字符串继续作为普通包名参与索引查找。
 */
internal fun FqName.parentOrRoot(): FqName {
    val fqNameString = asString()
    val parentString = fqNameString.substringBeforeLast('.', missingDelimiterValue = "")
    return if (parentString.isEmpty()) FqName.ROOT else FqName(parentString)
}

/**
 * 将限定名最后一段转换为普通标识符名称。
 *
 * 该函数只服务于已确认不是特殊名称的导入短名，调用侧负责保证输入来自语法合法的导入路径。
 */
internal fun FqName.shortNameAsIdentifier(): Name {
    val fqNameString = asString()
    val shortNameString = fqNameString.substringAfterLast('.')
    return Name.identifier(shortNameString)
}

/**
 * 为已解析导入绑定生成稳定签名。
 *
 * 签名包含原始导入路径、是否星号导入以及解析目标集合，
 * 用于冲突检测和重复导入判定时消除目标枚举顺序带来的不稳定性。
 */
internal fun CfirResolvedImportBinding.stableTargetSignature(): String {
    val targetSignatures = targets.map { target ->
        when (target) {
            is CfirResolvedImportTarget.Package -> "pkg:${target.fqName.asString()}"
            is CfirResolvedImportTarget.ClassLike -> "class:${target.classId.asString()}"
            is CfirResolvedImportTarget.Callable -> {
                val callableOwner = "${target.packageFqName.asString()}.${target.name.asString()}"
                "callable:$callableOwner#${target.symbols.size}"
            }
        }
    }.sorted()
    return buildString {
        append(importDirective.importedFqName?.asString() ?: "")
        append('|')
        append(importDirective.isAllUnder)
        append('|')
        append(targetSignatures.joinToString(";"))
    }
}

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportBinding
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportTarget
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

internal fun FqName.parentOrRoot(): FqName {
    val fqNameString = asString()
    val parentString = fqNameString.substringBeforeLast('.', missingDelimiterValue = "")
    return if (parentString.isEmpty()) FqName.ROOT else FqName(parentString)
}

internal fun FqName.shortNameAsIdentifier(): Name {
    val fqNameString = asString()
    val shortNameString = fqNameString.substringAfterLast('.')
    return Name.identifier(shortNameString)
}

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


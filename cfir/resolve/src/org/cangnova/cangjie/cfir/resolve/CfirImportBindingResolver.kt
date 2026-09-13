package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.declarations.CfirImport
import org.cangnova.cangjie.cfir.declarations.builder.buildImport
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOrigin
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportBinding
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportTarget
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/** 将 raw import 指令解析为结构化 import binding 的解析器。 */
internal class CfirImportBindingResolver(
    /** 当前解析 session，用于查询包、class-like 与顶层 callable。 */
    private val session: CfirSession,
) {
    /**
     * 将一层语言默认 import 解析为与源码 import 相同的结构 binding。
     *
     * 排除列表在 binding 建立时一次性应用；类型、tower、extend 可达性等后续入口
     * 只消费记录结果，不再各自读取 [org.cangnova.cangjie.resolve.DefaultImportsProvider]。
     */
    fun resolveDefaultImportBindings(
        imports: List<ImportPath>,
        excludedImports: List<FqName>,
    ): List<CfirResolvedImportBinding> = imports
        .asSequence()
        .filter { it.fqName !in excludedImports }
        .map { importPath ->
            val importDirective = buildImport {
                source = null
                importedFqName = importPath.fqName
                isAllUnder = importPath.isAllUnder
                aliasName = importPath.alias
                aliasSource = null
            }
            resolveImportBinding(importDirective, CfirLookupOrigin.DEFAULT_IMPORT)
        }
        .toList()

    /**
     * 解析单条 [CfirImport] 的有效导入名和候选目标集合。
     *
     * 目标可能同时包含 package、class-like 与 callable；all-under import 不在这里展开具体成员。
     */
    fun resolveImportBinding(
        importDirective: CfirImport,
        lookupOrigin: CfirLookupOrigin,
    ): CfirResolvedImportBinding {
        require(
            lookupOrigin == CfirLookupOrigin.EXPLICIT_IMPORT ||
                lookupOrigin == CfirLookupOrigin.DEFAULT_IMPORT,
        ) {
            "Import binding requires EXPLICIT_IMPORT or DEFAULT_IMPORT origin, got $lookupOrigin"
        }
        val importedFqName = importDirective.importedFqName
        val aliasName = importDirective.aliasName
        val effectiveName = when {
            aliasName != null -> aliasName
            importedFqName != null -> importedFqName.shortNameAsIdentifier()
            else -> org.cangnova.cangjie.name.Name.identifier("")
        }
        val targets = mutableListOf<CfirResolvedImportTarget>()
        val packageCandidates = importedFqName?.let { importedName ->
            val packageNames = if (importDirective.isAllUnder) {
                listOf(importedName)
            } else {
                listOf(importedName, importedName.parentOrRoot()).distinct()
            }
            packageNames.flatMap { packageName -> packageFqNameCandidates(importDirective, packageName) }.distinct()
        }.orEmpty()
        val memberPackageCandidates = importedFqName
            ?.parentOrRoot()
            ?.let { packageFqNameCandidates(importDirective, it) }
            .orEmpty()

        if (importedFqName != null) {
            packageCandidates.firstOrNull(session.symbolProvider::hasPackage)?.let { packageFqName ->
                targets += CfirResolvedImportTarget.Package(packageFqName)
            }
        }

        if (importedFqName != null) {
            val memberName = importedFqName.shortNameAsIdentifier()

            if (!importDirective.isAllUnder) {
                for (packageFqName in memberPackageCandidates) {
                    val classId = ClassId(packageFqName, memberName)
                    session.symbolProvider.getClassLikeSymbolByClassId(classId)?.let { symbol ->
                        if (targets.none { it is CfirResolvedImportTarget.ClassLike && it.symbol == symbol }) {
                            targets += CfirResolvedImportTarget.ClassLike(
                                // import 路径可以命中其它包 public import 出来的声明；binding 必须保存
                                // 最终声明身份，effectiveName 已单独保留使用点名称，不能把两者混为一体。
                                classId = symbol.classId,
                                symbol = symbol,
                            )
                        }
                    }

                    val callableSymbols = session.symbolProvider.getTopLevelCallableSymbols(packageFqName, memberName)
                    if (callableSymbols.isNotEmpty()) {
                        targets += CfirResolvedImportTarget.Callable(
                            packageFqName = packageFqName,
                            name = memberName,
                            symbols = callableSymbols,
                        )
                    }
                }
            }
        }

        return CfirResolvedImportBinding(
            importDirective = importDirective,
            effectiveName = effectiveName,
            targets = targets,
            lookupOrigin = lookupOrigin,
        )
    }

    /**
     * 组织名不是普通包层级。官方 CJO 查找将 `org::pkg` 规范化为 `pkg@org`，源码包
     * 则仍以普通 `pkg` 作为 CFIR package identity；两种候选按库优先、源码兼容顺序查询。
     */
    private fun packageFqNameCandidates(importDirective: CfirImport, packageFqName: FqName): List<FqName> {
        val organizationName = importDirective.organizationName ?: return listOf(packageFqName)
        val canonicalName = "${packageFqName.asString()}@${organizationName.asString()}"
        return listOf(FqName(canonicalName), packageFqName).distinct()
    }
}

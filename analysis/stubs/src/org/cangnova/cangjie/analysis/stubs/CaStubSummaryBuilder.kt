package org.cangnova.cangjie.analysis.stubs

import com.intellij.psi.stubs.NamedStub
import com.intellij.psi.stubs.StubElement
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.stubs.CangJieBindingPatternStub
import org.cangnova.cangjie.psi.stubs.CangJieCallableStubBase
import org.cangnova.cangjie.psi.stubs.CangJieClassStub
import org.cangnova.cangjie.psi.stubs.CangJieEnumPatternStub
import org.cangnova.cangjie.psi.stubs.CangJieEnumStub
import org.cangnova.cangjie.psi.stubs.CangJieExtendStub
import org.cangnova.cangjie.psi.stubs.CangJieFileStub
import org.cangnova.cangjie.psi.stubs.CangJieFileStubKind
import org.cangnova.cangjie.psi.stubs.CangJieInterfaceStub
import org.cangnova.cangjie.psi.stubs.CangJiePlaceHolderStub
import org.cangnova.cangjie.psi.stubs.CangJieStructStub
import org.cangnova.cangjie.psi.stubs.CangJieStubWithFqName
import org.cangnova.cangjie.psi.stubs.CangJieTuplePatternStub
import org.cangnova.cangjie.psi.stubs.CangJieTypeAliasStub
import org.cangnova.cangjie.psi.stubs.CangJieTypePatternStub
import org.cangnova.cangjie.psi.stubs.CangJieTypeStatementStub
import org.cangnova.cangjie.psi.stubs.CangJieVarOrEnumPatternStub
import org.cangnova.cangjie.psi.stubs.CangJieVariableStub
import org.cangnova.cangjie.psi.stubs.CangJieWildcardPatternStub
import org.cangnova.cangjie.psi.stubs.elements.CjFileStubBuilder

/**
 * 负责把 `CjFile` 解析成 analysis 层可消费的 stub 摘要。
 *
 * 这里故意把 “文件 -> stub tree” 与 “stub tree -> 摘要” 两步拆开，
 * 这样 source / compiled 都走同一套摘要提取规则，而测试也可以直接喂手工 stub 树。
 */
internal class CaStubSummaryBuilder(
    /**
     * 当 PSI 文件没有现成 stub 时用于构建文件 stub 树的 builder。
     */
    private val fileStubBuilder: CjFileStubBuilder = CjFileStubBuilder(),

    /**
     * 从文件 stub 树提取 analysis 摘要的共享提取器。
     */
    private val extractor: CaStubTreeSummaryExtractor = CaStubTreeSummaryExtractor(),
) {
    /**
     * 为单个仓颉 PSI 文件构建 analysis stub 摘要。
     */
    fun build(file: CjFile): CaStubFileSummary {
        val fileStub = resolveFileStub(file)
        val fileKey = file.virtualFile?.url ?: file.name
        return extractor.extract(
            fileKey = fileKey,
            fallbackPackageFqName = resolveFallbackPackageFqName(file, fileStub),
            fileStub = fileStub,
        )
    }

    /**
     * 解析文件可用的仓颉文件 stub。
     *
     * 优先复用 PSI 已持有的 stub；没有时才通过 [fileStubBuilder] 构建，确保 source 与 compiled 路径统一。
     */
    private fun resolveFileStub(file: CjFile): CangJieFileStub {
        return (file.stub as? CangJieFileStub)
            ?: (fileStubBuilder.buildStubTree(file) as? CangJieFileStub)
            ?: error("Cannot build CangJieFileStub for ${file.name}")
    }

    /**
     * compiled file 已经由 decompiled 链路提供稳定 file stub，
     * 这里必须优先复用 stub 中的包信息，避免回退到 `file.packageFqName`
     * 触发整份 decompiled 文本的 PSI 解析。
     */
    private fun resolveFallbackPackageFqName(
        file: CjFile,
        fileStub: CangJieFileStub,
    ): FqName {
        return (fileStub.kind as? CangJieFileStubKind.WithPackage)?.packageFqName
            ?: file.packageFqName
    }
}

/**
 * 从稳定的 `CangJieFileStub` 树提取 analysis 摘要。
 *
 * 这层是整个模块的语义核心，必须保证 source 和 compiled 看到的是同一套规则。
 */
internal class CaStubTreeSummaryExtractor {
    /**
     * 从文件 stub 树中提取单文件摘要。
     */
    fun extract(
        fileKey: String,
        fallbackPackageFqName: FqName,
        fileStub: CangJieFileStub,
    ): CaStubFileSummary {
        val packageFqName = (fileStub.kind as? CangJieFileStubKind.WithPackage)?.packageFqName ?: fallbackPackageFqName
        val topLevelClassifiers = linkedSetOf<Name>()
        val topLevelCallables = linkedSetOf<Name>()
        val classMemberNames = linkedMapOf<ClassId, MutableSet<Name>>()

        fileStub.childrenStubs.forEach { child ->
            when (child) {
                is CangJieTypeStatementStub<*> -> {
                    if (child is CangJieExtendStub) return@forEach
                    child.name?.let { topLevelClassifiers += Name.identifier(it) }
                    collectClassMemberNames(child, classMemberNames)
                }

                is CangJieTypeAliasStub -> {
                    child.name?.let { topLevelClassifiers += Name.identifier(it) }
                }

                is CangJieCallableStubBase<*> -> {
                    if (child.isTopLevel() || child.parentStub is CangJieFileStub) {
                        child.name?.let { topLevelCallables += Name.identifier(it) }
                    }
                }

                is CangJieVariableStub -> {
                    if (child.isTopLevel()) {
                        topLevelCallables += collectPatternDeclarationNames(child)
                    }
                }
            }
        }

        return CaStubFileSummary(
            fileKey = fileKey,
            stubKind = fileStub.kind,
            packageFqName = packageFqName,
            topLevelClassifierNames = topLevelClassifiers,
            topLevelCallableNames = topLevelCallables,
            classMemberNames = classMemberNames.mapValues { (_, names) -> names.toSet() },
        )
    }

    /**
     * class-like 成员名提取规则。
     *
     * 这里只消费 PSI stub 层已经确认过的声明节点，不再在 analysis 层自行推导语法。
     */
    private fun collectClassMemberNames(
        classStub: CangJieTypeStatementStub<*>,
        destination: MutableMap<ClassId, MutableSet<Name>>,
    ) {
        val classId = classStub.getClassId() ?: return
        val memberNames = destination.getOrPut(classId, ::linkedSetOf)
        collectClassMemberNamesFromChildren(classStub.childrenStubs, destination, memberNames)
    }

    /**
     * 递归遍历 class-like body 子 stub，收集成员名称并为嵌套 class-like 建立独立成员索引。
     */
    private fun collectClassMemberNamesFromChildren(
        children: List<StubElement<*>>,
        destination: MutableMap<ClassId, MutableSet<Name>>,
        memberNames: MutableSet<Name>,
    ) {
        children.forEach { child ->
            when (child) {
                is CangJiePlaceHolderStub<*> -> {
                    collectClassMemberNamesFromChildren(child.childrenStubs, destination, memberNames)
                }

                is CangJieCallableStubBase<*>,
                is CangJieTypeAliasStub -> {
                    (child as? CangJieStubWithFqName<*>)?.name?.let { memberNames += Name.identifier(it) }
                }

                is CangJieClassStub,
                is CangJieStructStub,
                is CangJieInterfaceStub,
                is CangJieEnumStub -> {
                    (child as? CangJieStubWithFqName<*>)?.name?.let { memberNames += Name.identifier(it) }
                    collectClassMemberNames(child as CangJieTypeStatementStub<*>, destination)
                }
            }
        }
    }

    /**
     * 顶层模式变量在 Analysis API 中对齐为 package-level callable。
     *
     * 因此这里必须从 pattern stub 子树中抽出真正声明出来的名字，而不是把整个变量节点当成匿名占位。
     */
    private fun collectPatternDeclarationNames(
        stub: StubElement<*>,
    ): Set<Name> {
        val names = linkedSetOf<Name>()
        stub.childrenStubs.forEach { child ->
            when (child) {
                is CangJieBindingPatternStub,
                is CangJieTypePatternStub,
                is CangJieVarOrEnumPatternStub -> {
                    (child as? NamedStub<*>)?.name?.let { names += Name.identifier(it) }
                }

                is CangJieTuplePatternStub,
                is CangJieEnumPatternStub,
                is CangJieWildcardPatternStub,
                is CangJieVariableStub -> {
                    names += collectPatternDeclarationNames(child)
                }
            }
        }
        return names
    }
}

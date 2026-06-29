package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.symbolRelationProvider

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.containingDeclaration
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiComponentTestDirectives
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiReferenceTestUtils.isExtendMemberDeclaration
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiSymbolOverrideTestDirectives
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedAllOverridden
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedDirectOverridden
import org.cangnova.cangjie.analysis.api.impl.base.test.overrideTargetKind
import org.cangnova.cangjie.analysis.api.impl.base.test.targetNameText
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * 覆写关系 generated 测试。
 *
 * 这组测试直接对齐 Kotlin Analysis 的 overrides 主链，但只保留仓颉真实存在的能力：
 * 1. `directlyOverriddenSymbols`
 * 2. `allOverriddenSymbols`
 * 3. class member / property / extend member 的统一覆写视图
 *
 * 输出采用稳定签名，而不是直接泄漏后端 symbol 细节，
 * 这样后续导航、文档、renderer 侧都可以复用同一条关系语义。
 */
abstract class AbstractOverriddenDeclarationProviderTest : AbstractAnalysisApiComponentTest() {
    /**
     * 当前 override relation 测试额外注册的目标种类与期望覆写链指令。
     */
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + AnalysisApiSymbolOverrideTestDirectives

    /**
     * 执行覆写关系测试。
     *
     * 方法定位目标 callable，恢复公开 symbol，并比较直接覆写链与完整覆写链的稳定签名。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val targetOwnerName = directives[AnalysisApiComponentTestDirectives.TARGET_CLASS].singleOrNull()
        val targetDeclaration = findTargetCallable(
            mainFile = mainFile,
            targetKind = directives.overrideTargetKind,
            targetName = directives.targetNameText,
            targetOwnerName = targetOwnerName,
        )

        analyzeForTest(mainFile) {
            val symbol = when (targetDeclaration) {
                is CjNamedFunction -> targetDeclaration.symbol as? CaCallableSymbol
                is CjProperty -> targetDeclaration.symbol as? CaCallableSymbol
                else -> error("Unsupported override target PSI: ${targetDeclaration::class.simpleName}")
            } ?: error("Override target `${directives.targetNameText}` cannot be restored to a callable symbol.")

            val actualAll = symbol.allOverriddenSymbols.map { overridden -> renderCallableSignature(overridden) }.toList()
            val actualDirect = symbol.directlyOverriddenSymbols.map { overridden -> renderCallableSignature(overridden) }.toList()

            assertEquals(directives.expectedAllOverridden, actualAll, "allOverriddenSymbols 输出不符合预期。")
            assertEquals(directives.expectedDirectOverridden, actualDirect, "directlyOverriddenSymbols 输出不符合预期。")
        }
    }

    /**
     * 按目标种类、名称和可选 owner 名称定位待检查的 callable PSI。
     *
     * 该定位区分成员函数、成员属性和 extend 函数，避免同名 callable 混淆。
     */
    private fun findTargetCallable(
        mainFile: CjFile,
        targetKind: String,
        targetName: String,
        targetOwnerName: String?,
    ) = when (targetKind) {
        "MEMBER_FUNCTION" -> PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java)
            .single { function ->
                function.name == targetName &&
                    !function.isExtendMemberDeclaration() &&
                    function.getStrictParentOfType<CjTypeStatement>()?.name == targetOwnerName
            }

        "MEMBER_PROPERTY" -> PsiTreeUtil.findChildrenOfType(mainFile, CjProperty::class.java)
            .single { property ->
                property.name == targetName &&
                    property.getStrictParentOfType<CjTypeStatement>()?.name == targetOwnerName
            }

        "EXTEND_FUNCTION" -> PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java)
            .single { function ->
                function.name == targetName && function.isExtendMemberDeclaration()
            }

        else -> error("Unsupported override target kind: $targetKind")
    }

    /**
     * 把 callable 关系渲染成稳定签名，作为 generated 测试的真相输出。
     *
     * 这里不直接依赖后端 debug string，而是只消费公开 Analysis API：
     * - 语义归属关系
     * - 参数列表
     * - 返回类型
     */
    private fun CaSession.renderCallableSignature(symbol: CaCallableSymbol): String = buildString {
        append(renderDeclarationQualifiedName(symbol))
        if (symbol is CaNamedFunctionSymbol) {
            append("(")
            val parameters = (symbol as CaValueParameterOwnerSymbol).valueParameters
            parameters.forEachIndexed { index, parameter ->
                append(parameter.name.asString())
                append(":")
                append(normalizeTypeRendering(parameter.returnType.render(CaTypeRendererForSource.WITH_SHORT_NAMES)))
                if (index != parameters.lastIndex) {
                    append(",")
                }
            }
            append(")")
        }
        append(":")
        append(normalizeTypeRendering(symbol.returnType.render(CaTypeRendererForSource.WITH_SHORT_NAMES)))
    }

    /**
     * 渲染 callable symbol 的稳定限定名称。
     *
     * 优先使用 `CallableId`；对于 extend 或局部 callable，则沿 containing declaration 链拼接可读名称。
     */
    private fun CaSession.renderDeclarationQualifiedName(symbol: CaCallableSymbol): String {
        symbol.callableId?.let { callableId ->
            return callableId.toString().replace('/', '.')
        }

        val extendContainer = symbol.containingDeclaration as? CaExtendSymbol
        if (extendContainer != null) {
            val packageName = extendContainer.extendId.substringBefore(':')
            val receiverType = extendContainer.extendId.substringAfter(':').substringBefore("<:")
            return "$packageName.$receiverType.${symbol.name?.asString() ?: "<anonymous>"}"
        }

        val parentsWithSelf = generateSequence<CaDeclarationSymbol>(symbol as CaDeclarationSymbol) { current ->
            current.containingDeclaration as? CaDeclarationSymbol
        }.toList().asReversed()

        return parentsWithSelf.joinToString(".") { declaration ->
            declaration.name?.asString() ?: "<anonymous>"
        }
    }
}

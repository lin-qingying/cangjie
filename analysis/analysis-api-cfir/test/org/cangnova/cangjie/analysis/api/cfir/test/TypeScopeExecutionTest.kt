package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForDebug
import org.cangnova.cangjie.analysis.api.signatures.CaFunctionSignature
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 具体类型的类型作用域执行回归（任务 4.1/4.5）。
 *
 * 验的是**替换**：`Box<Int64>` 的成员签名必须是 `Int64`，`Box<Shaped>` 的必须是 `Shaped`，
 * 而声明仍是同一个 `Box.unwrap` 符号；`declarationScope` 给出未替换的对照视图。
 * 只比名字的断言在这里没有意义——名字在两种视图里都一样，正是「看起来对但其实没替换」的失效形态。
 */
@OptIn(CaExperimentalApi::class)
class TypeScopeExecutionTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/typeScope",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 泛型实例、泛型继承、typealias、受约束 extend 四种形态的替换与约束过滤。
     */
    @Test
    fun typeScopeSubstitution(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            fun typeOfParameter(functionName: String): CaType =
                mainFile.function(functionName).symbol.valueParameters.single().returnType

            fun render(type: CaType): String = type.render(CaTypeRendererForDebug.WITH_SHORT_NAMES)

            val intBoxType = typeOfParameter("takesIntBox")
            val shapedBoxType = typeOfParameter("takesShapedBox")
            val intBoxChildType = typeOfParameter("takesIntBoxChild")
            val intBoxAliasType = typeOfParameter("takesIntBoxAlias")

            // ① 泛型实例：成员签名按实际类型实参替换，且保留原声明身份。
            val intBoxScope = requireNotNull(intBoxType.typeScope) { "Box<Int64> 必须有类型作用域" }
            val unwrap = intBoxScope.callableSignatures(Name.identifier("unwrap")).single()
            assertEquals(
                "Int64",
                render((unwrap as CaFunctionSignature<*>).valueParameters.single().returnType),
                "签名侧值参数必须是替换后的 Int64，而不是声明侧的 T",
            )
            assertEquals("Int64", render(unwrap.returnType), "签名侧返回类型必须是替换后的 Int64")
            assertEquals(
                "unwrap",
                (unwrap.symbol as CaNamedSymbol).name?.asString(),
                "替换不得换掉声明身份：签名仍指向 Box.unwrap 这个符号",
            )

            // ② 对照：声明视图仍是未替换的（符号侧参数类型是 T）。
            val declarationUnwrap = intBoxScope.declarationScope
                .callables(Name.identifier("unwrap"))
                .single()
            assertEquals(
                "T",
                render((declarationUnwrap as CaFunctionSymbol).valueParameters.single().returnType),
                "声明视图必须保持未替换：这里正是它与类型作用域的分工处",
            )

            // ③ 泛型继承：IntBox 从 Box<Int64> 继承的成员同样替换。
            val intBoxChildScope = requireNotNull(intBoxChildType.typeScope) { "IntBox 必须有类型作用域" }
            assertEquals(
                "Int64",
                render(intBoxChildScope.callableSignatures(Name.identifier("unwrap")).single().returnType),
                "泛型继承链上的成员签名也必须替换",
            )


            // ④ typealias：别名不改变替换语义。
            val intBoxAliasScope = requireNotNull(intBoxAliasType.typeScope) { "IntBoxAlias 必须有类型作用域" }
            assertEquals(
                "Int64",
                render(intBoxAliasScope.callableSignatures(Name.identifier("unwrap")).single().returnType),
                "typealias 指向的实例类型同样替换",
            )

            // ⑤ 受约束 extend：约束满足才出现，且出现时签名已替换。
            val shapedScope = requireNotNull(shapedBoxType.typeScope) { "Box<Shaped> 必须有类型作用域" }
            assertEquals(
                "Shaped",
                render(shapedScope.callableSignatures(Name.identifier("shapedTag")).single().returnType),
                "受约束 extend 成员的签名按实参替换",
            )
            assertTrue(
                intBoxScope.callableSignatures(Name.identifier("shapedTag")).toList().isEmpty(),
                "Box<Int64> 不满足 T <: Shaped，受约束 extend 成员不得出现",
            )

            // ⑥ 属性：接口属性的签名同样按实参替换，声明视图仍是 T。
            val containerType = typeOfParameter("takesContainer")
            val containerScope = requireNotNull(containerType.typeScope) { "Container<Int64> 必须有类型作用域" }
            assertEquals(
                "Int64",
                render(containerScope.callableSignatures(Name.identifier("stored")).single().returnType),
                "属性签名必须按实参替换",
            )
            assertEquals(
                "T",
                render((containerScope.declarationScope.callables(Name.identifier("stored")).single() as CaPropertySymbol).returnType),
                "属性在声明视图里仍是 T",
            )

            // ⑦ 非 class-like receiver：有上界的类型参数按其上界取成员。
            val boundedType = typeOfParameter("takesBounded")
            val boundedScope = requireNotNull(boundedType.typeScope) { "有上界的类型参数必须有类型作用域（取上界）" }
            assertTrue(
                boundedScope.callableSignatures(Name.identifier("area")).toList().isNotEmpty(),
                "类型参数的成员来自其上界 Shaped", 
            )

            // 名字视图与符号视图一致：替换不改变可查到的名字集合。
            assertTrue(intBoxScope.mayContainName(Name.identifier("unwrap")), "名字视图必须报告 unwrap")
            assertNotNull(
                intBoxScope.declarationScope.callables(Name.identifier("unwrap")).single().callableId,
                "声明视图的成员必须保留 callableId（导入/缩短依赖它）",
            )
        }
    }

    /**
     * 按名字取夹具里的唯一命名函数。
     */
    private fun CjFile.function(name: String): CjNamedFunction =
        PsiTreeUtil.findChildrenOfType(this, CjNamedFunction::class.java).single { it.name == name }
}

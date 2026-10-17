package org.cangnova.cangjie.analysis.low.level.api.cfir

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.collectDiagnosticsForFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolvableSessionForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.utils.hasSerializedInitializer
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjParameter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 验证 SDK std.ast 的 compiled PSI stub 成员能完整进入 IDE stub-based CFIR。 */
class DecompiledStdlibMacroApiTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/decompiledLibraries",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    /** 宏 API 的枚举项、字段必须可解析，且不能在 stub→CFIR 时丢失或重复。 */
    @Test
    fun decompiledStdlibMacroApi(mainFile: CjFile) {
        val diagnostics = mainFile.collectDiagnosticsForFile(
            mainFile.getResolutionFacadeForTest(),
            DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS,
        )
        val diagnosticDetails = diagnostics.joinToString("\n") {
            "${it.factoryName} at ${it.textRanges} on `${it.psiElement.text}`"
        }
        assertEquals(emptyList<String>(), diagnostics.map { it.factoryName }.sorted(), diagnosticDetails)

        val symbolProvider = mainFile.getResolvableSessionForTest().symbolProvider
        val reportLevels = classLike(symbolProvider, "std.ast", "DiagReportLevel") as CfirEnum
        val reportEntries = reportLevels.declarations.filterIsInstance<CfirEnumConstructor>()
        assertEquals(setOf("ERROR", "WARNING"), reportEntries.map { it.name.asString() }.toSet())
        assertEquals(2, reportEntries.size, "each enum entry must be materialized exactly once")
        assertTrue(reportEntries.all { (it.psi as CjEnumConstructor).containingCjFile.isCompiled })

        val tokenKinds = classLike(symbolProvider, "std.ast", "TokenKind") as CfirEnum
        val tokenKindEntries = tokenKinds.declarations.filterIsInstance<CfirEnumConstructor>()
        assertTrue(tokenKindEntries.any { it.name.asString() == "IDENTIFIER" })
        assertEquals(
            tokenKindEntries.size,
            tokenKindEntries.map { it.name }.toSet().size,
            "compiled enum entries must not be duplicated",
        )
        val identifierEntry = tokenKindEntries.single { it.name.asString() == "IDENTIFIER" }
        val packageIdentifierSymbols = symbolProvider.getTopLevelCallableSymbols(
            FqName("std.ast"),
            Name.identifier("IDENTIFIER"),
        ).filterIsInstance<CfirEnumConstructorSymbol>()
        assertEquals(1, packageIdentifierSymbols.size, "an enum entry must be indexed once for unqualified lookup")
        assertSame(identifierEntry.symbol, packageIdentifierSymbols.single(), "package lookup must reuse the enum-owned symbol")

        val token = classLike(symbolProvider, "std.ast", "Token") as CfirStruct
        val kindField = token.declarations.filterIsInstance<CfirFieldVariable>().single { it.name.asString() == "kind" }
        assertTrue((kindField.psi as CjFieldVariable).containingCjFile.isCompiled)

        val delimiterField = token.declarations.filterIsInstance<CfirFieldVariable>().single { it.name.asString() == "delimiterNum" }
        val delimiterPsi = delimiterField.psi as CjFieldVariable
        assertEquals(
            delimiterPsi.hasInitializer(),
            delimiterField.hasSerializedInitializer,
            "CFIR must preserve whether the compiled stub exposes an initializer expression",
        )
        assertEquals(delimiterPsi.isStatic, delimiterField.status.isStatic)
        assertEquals(delimiterPsi.isConst, delimiterField.status.isConst)
        assertEquals(delimiterPsi.hasModifier(org.cangnova.cangjie.lexer.CjTokens.MUT_KEYWORD), delimiterField.status.isMut)

        val path = classLike(symbolProvider, "std.fs", "Path") as CfirStruct
        val separator = path.declarations.filterIsInstance<CfirFieldVariable>().single { it.name.asString() == "Separator" }
        assertTrue(separator.status.isStatic)
        assertTrue(separator.status.isConst)
        assertEquals(null, separator.dispatchReceiverType, "static fields must not acquire an instance receiver")
        assertEquals(
            (separator.psi as CjFieldVariable).hasInitializer(),
            separator.hasSerializedInitializer,
            "CFIR must preserve the compiled stub's initializer availability without inferring source syntax",
        )

        val option = classLike(symbolProvider, "std.core", "Option") as CfirEnum
        val optionTypeParameter = option.typeParameters.single()
        val some = option.declarations.filterIsInstance<CfirEnumConstructor>().single { it.name.asString() == "Some" }
        val payloadType = some.valueParameters.single().returnTypeRef.coneTypeOrNull as ConeTypeParameterType
        assertEquals(optionTypeParameter.symbol, payloadType.lookupTag.typeParameterSymbol)
        val someReturnType = some.returnTypeRef.coneTypeOrNull as ConeEnumType
        assertEquals(option.symbol.classId, someReturnType.classId)
        assertEquals(optionTypeParameter.symbol, (someReturnType.typeArguments.single().type as ConeTypeParameterType).lookupTag.typeParameterSymbol)

        val printFunctions = symbolProvider.getTopLevelFunctionSymbols(
            FqName("std.core"),
            Name.identifier("print"),
        ).map { it.cfir as? CfirNamedFunction ?: error("std.core.print must deserialize as a named function") }
            .filter { function -> function.valueParameters.any { it.name.asString() == "flush" } }
        assertEquals(15, printFunctions.size, "all standard print overloads with `flush` must be projected exactly once")
        printFunctions.forEach { printFunction ->
            val flush = printFunction.valueParameters.single { it.name.asString() == "flush" }
            assertTrue(flush.isNamed, "CJO named-parameter metadata must survive the library PSI stub")
            assertTrue(flush.defaultValue != null, "CJO default-value metadata must survive the library PSI stub")
            val flushPsi = flush.psi as CjParameter
            assertTrue(flushPsi.containingCjFile.isCompiled)
            assertTrue(flushPsi.isNamed)
            assertTrue(flushPsi.hasDefaultValue())
        }
    }

    private fun classLike(
        symbolProvider: org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider,
        packageName: String,
        name: String,
    ) = requireNotNull(
        symbolProvider.getClassLikeSymbolByClassId(
            ClassId(FqName(packageName), Name.identifier(name)),
        ),
    ) { "SDK declaration $packageName.$name must be available through the library symbol provider" }.cfir
}

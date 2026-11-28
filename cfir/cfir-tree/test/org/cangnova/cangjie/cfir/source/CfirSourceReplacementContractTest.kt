package org.cangnova.cangjie.cfir.source

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.declarations.builder.buildImport
import org.cangnova.cangjie.cfir.declarations.builder.buildResolvedImport
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** 源代次迁移须覆盖统一IR源槽位；不得通过重建节点来改变既有节点身份。 */
class CfirSourceReplacementContractTest {
    @Test
    fun replacesRootSourceWithoutReplacingElement() {
        val before = BinarySource("before")
        val after = BinarySource("after")
        val element = buildLiteralExpression {
            source = before
            kind = CfirLiteralKind.INT
            value = "42"
        }
        val replace = CfirElement::class.java.getMethod("replaceSource", CjSourceElement::class.java)
        replace.invoke(element, after)
        assertSame(after, element.source)
        assertEquals("42", element.value)
        assertEquals(CfirLiteralKind.INT, element.kind)
    }

    @Test
    fun fileSourceAndLineMappingBelongToTheSameMigrationContract() {
        CfirFile::class.java.getMethod("replaceSourceFile", org.cangnova.cangjie.CjSourceFile::class.java)
        CfirFile::class.java.getMethod("replaceSourceFileLinesMapping", org.cangnova.cangjie.source.CjSourceFileLinesMapping::class.java)
    }

    @Test
    @org.cangnova.cangjie.cfir.CfirImplementationDetail
    fun delegatedImportSourcesMoveTheActualDelegate() {
        val before = BinarySource("before-import")
        val after = BinarySource("after-import")
        val beforeAlias = BinarySource("before-alias")
        val afterAlias = BinarySource("after-alias")
        val import = buildImport {
            source = before
            aliasSource = beforeAlias
            importedFqName = FqName("sample.item")
            isAllUnder = false
        }
        val resolved = buildResolvedImport {
            delegate = import
            packageFqName = FqName("sample")
        }
        resolved.replaceSource(after)
        resolved.replaceAliasSource(afterAlias)
        assertSame(import, resolved.delegate)
        assertSame(after, import.source)
        assertSame(after, resolved.source)
        assertSame(afterAlias, import.aliasSource)
        assertSame(afterAlias, resolved.aliasSource)
    }

    @Test
    fun sourceLessSingletonCannotAcquireAFileGeneration() {
        val after = BinarySource("after")
        val replace = CfirElement::class.java.getMethod("replaceSource", CjSourceElement::class.java)
        val failure = assertThrows(java.lang.reflect.InvocationTargetException::class.java) {
            replace.invoke(org.cangnova.cangjie.cfir.expressions.CfirEmptyArgumentList, after)
        }
        assertEquals(IllegalArgumentException::class.java, failure.cause?.javaClass)
    }

    private class BinarySource(identity: String) : CjBinarySourceElement(
        debugText = identity,
        binaryFilePath = null,
        stableIdentity = identity,
    )
}

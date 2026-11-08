package org.cangnova.cangjie.analysis.decompiler.stub

import org.cangnova.cangjie.cfir.declarations.CfirClassKind
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.psi.stubs.impl.CangJieStubClassifierKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * 守住 stub 层 kind 与 CFIR 声明 kind 的词汇对应关系。
 *
 * `CangJieStubClassifierKind` 把 `ordinal` 直接写进 USER_TYPE stub（顺序即格式），
 * 这里防的是**词汇漂移**（新增/改名/重排）；生产端 `Cone* -> kind` 的语义映射由
 * `TypeCjoStubBuilder` 负责，另有后续批次补生产端断言。
 */
class CangJieStubClassifierKindConsistencyTest {
    @Test
    fun stubKindOrderMatchesCfirClassKind() {
        assertEquals(
            CfirClassKind.entries.map { kind -> kind.name },
            CangJieStubClassifierKind.entries.map { kind -> kind.name },
        )
    }

    @Test
    fun coneTypesMapToExpectedStubKind() {
        val tag = StdlibClassIds.Any.toLookupTag()
        assertEquals(CangJieStubClassifierKind.CLASS, stubClassifierKindOf(ConeClassLikeType(tag)))
        assertEquals(
            CangJieStubClassifierKind.INTERFACE,
            stubClassifierKindOf(ConeClassLikeType(tag, isInterface = true)),
        )
        assertEquals(CangJieStubClassifierKind.STRUCT, stubClassifierKindOf(ConeStructType(tag)))
        assertEquals(CangJieStubClassifierKind.ENUM, stubClassifierKindOf(ConeEnumType(tag)))
        assertNull(stubClassifierKindOf(ConePrimitiveType.INT64))
        // 别名由 alias 分支单独处理，不能被 kind 分派吃掉
        assertNull(stubClassifierKindOf(ConeTypeAliasType(ClassId.fromString("std/core/Byte"))))
    }
}

package org.cangnova.cangjie.analysis.low.level.api.cfir.api

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import kotlin.test.Test
import kotlin.test.assertNull

class CfirDesignationBuiltinFallbackTest {
    @Test
    fun `primitive class ids are excluded from containing file fallback`() {
        assertNull(findInContainingFileIfApplicable(PrimitiveTypeKind.INT64.classId, target = null))
        assertNull(findInContainingFileIfApplicable(PrimitiveTypeKind.BOOLEAN.classId, target = null))
    }

    @Test
    fun `non primitive class ids require containing file to search`() {
        val stdStringClassId = ClassId(FqName("std.core"), Name.identifier("String"))
        val customClassId = ClassId(FqName("demo.pkg"), Name.identifier("Sample"))

        assertNull(findInContainingFileIfApplicable(stdStringClassId, target = null))
        assertNull(findInContainingFileIfApplicable(customClassId, target = null))
        assertNull(findInContainingFileIfApplicable(ClassId.topLevel(StandardNames.FqNames.anyFqName), target = null))
    }
}

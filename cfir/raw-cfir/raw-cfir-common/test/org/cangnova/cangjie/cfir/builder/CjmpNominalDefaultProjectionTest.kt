package org.cangnova.cangjie.cfir.builder

import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.parsing.CjmpDirectMemberSyntaxFacts
import org.cangnova.cangjie.parsing.CjmpNominalSyntaxFacts
import org.cangnova.cangjie.psi.CjNodeTypes
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CjmpNominalDefaultProjectionTest {
    private val settings = LanguageVersionSettingsImpl.DEFAULT

    private fun member(type: IElementType, common: Boolean = true, specific: Boolean = false,
                       platform: Boolean = false, body: Boolean = false, accessor: Boolean = false,
                       initializer: Boolean = false) = CjmpDirectMemberSyntaxFacts(
        type, common, specific, platform, body, accessor, initializer,
    )

    private fun projects(vararg members: CjmpDirectMemberSyntaxFacts) =
        CjmpNominalSyntaxFacts(members.toList()).hasCjmpCommonDefault(settings)

    @Test
    fun evaluatesDirectCommonMembers() {
        assertTrue(projects())
        assertTrue(projects(member(CjNodeTypes.FUNC, common = false)))
        assertTrue(projects(member(CjNodeTypes.ENUM_CONSTRUCTOR)))
        for (type in listOf(CjNodeTypes.FUNC, CjNodeTypes.SECONDARY_CONSTRUCTOR, CjNodeTypes.FINALIZER)) {
            assertFalse(projects(member(type)))
            assertTrue(projects(member(type, body = true)))
        }
        assertFalse(projects(member(CjNodeTypes.PROPERTY)))
        assertTrue(projects(member(CjNodeTypes.PROPERTY, accessor = true)))
        assertFalse(projects(member(CjNodeTypes.FIELD)))
        assertTrue(projects(member(CjNodeTypes.FIELD, initializer = true)))
        assertFalse(projects(member(CjNodeTypes.VARIABLE, initializer = true)))
        assertFalse(projects(member(CjNodeTypes.FUNC, body = true), member(CjNodeTypes.FIELD)))
        assertTrue(projects(member(CjNodeTypes.VARIABLE, specific = true)))
    }

    @Test
    fun respectsVersionedRolesAndPrimaryConstructorFeature() {
        val alpha66 = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_ALPHA_66)
        val alpha68 = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_ALPHA_68)
        val constructor = CjmpNominalSyntaxFacts(listOf(member(CjNodeTypes.PRIMARY_CONSTRUCTOR, body = true)))
        assertTrue(constructor.hasCjmpCommonDefault(alpha66))
        assertFalse(constructor.hasCjmpCommonDefault(alpha68))
        assertFalse(CjmpNominalSyntaxFacts(listOf(member(CjNodeTypes.PRIMARY_CONSTRUCTOR)))
            .hasCjmpCommonDefault(alpha66))
        val platform = CjmpNominalSyntaxFacts(listOf(member(CjNodeTypes.VARIABLE, platform = true)))
        assertTrue(platform.hasCjmpCommonDefault(alpha66))
        assertFalse(platform.hasCjmpCommonDefault(settings))
    }
}

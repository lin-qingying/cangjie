package org.cangnova.cangjie.cfir.checkers.generator.diagnostics

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model.DiagnosticList
import org.cangnova.cangjie.util.PrivateForInline

@Suppress("UNUSED_VARIABLE", "LocalVariableName", "ClassName", "unused")
@OptIn(PrivateForInline::class)
object DIAGNOSTICS_LIST : DiagnosticList("CfirErrors") {
    val RESOLVE by object : DiagnosticGroup("Resolve") {
        val INVALID_DECLARATION by error<PsiElement> {
            parameter<String>("reason")
        }

        val TYPES_ERROR_RECOVERY by error<PsiElement> {
            parameter<String>("ruleId")
            parameter<String>("detail")
        }

        val IMPORT_TARGET_NOT_FOUND by error<PsiElement> {
            parameter<String>("ruleId")
            parameter<String>("detail")
        }
        val IMPORT_CONFLICT by error<PsiElement> {
            parameter<String>("ruleId")
            parameter<String>("detail")
        }
        val IMPORT_ALIAS_CONFLICT by error<PsiElement> {
            parameter<String>("ruleId")
            parameter<String>("detail")
        }

        val SUPER_TYPES_SELF_REFERENCE by error<PsiElement> {
            parameter<String>("ruleId")
            parameter<String>("detail")
        }
        val SUPER_TYPES_DUPLICATE by error<PsiElement> {
            parameter<String>("ruleId")
            parameter<String>("detail")
        }

        val ILLEGAL_EXTENDED_TYPE by error<PsiElement> {
            parameter<String>("ruleId")
            parameter<String>("detail")
        }
        val EXTEND_DUPLICATE_INTERFACE by error<PsiElement> {
            parameter<String>("ruleId")
            parameter<String>("detail")
        }
        val EXTEND_NOT_INTERFACE by error<PsiElement> {
            parameter<String>("ruleId")
            parameter<String>("detail")
        }

        val INTERFACE_CANNOT_INHERIT_CLASS by error<PsiElement> {
            parameter<String>("ruleId")
            parameter<String>("detail")
        }
        val MULTIPLE_CLASS_SUPER_TYPES by error<PsiElement> {
            parameter<String>("ruleId")
            parameter<String>("detail")
        }

        val STATUS_MODIFIER_LEGALITY by error<PsiElement> {
            parameter<String>("ruleId")
            parameter<String>("detail")
        }
    }
}

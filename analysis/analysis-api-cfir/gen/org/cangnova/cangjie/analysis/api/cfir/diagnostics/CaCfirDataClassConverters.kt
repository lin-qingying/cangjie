

package org.cangnova.cangjie.analysis.api.cfir.diagnostics

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.psi.CjBlockExpression
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjImportItem
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjResumeExpression
import org.cangnova.cangjie.psi.CjTypeReference

/*
 * 本文件由生成器自动生成
 * 请勿手动修改
 */

internal val CJ_DIAGNOSTIC_CONVERTER: CaDiagnosticConverter = CaDiagnosticConverterBuilder.buildConverter {
    addConversions1()
    addConversions3()
    addConversions4()
    addConversions5()
    addConversions6()
    addConversions7()
    addConversions8()
    addConversions9()
    addConversions10()
    addConversions11()
    addConversions14()
    addConversions16()
    addConversions17()
    addConversions18()
    addConversions19()
    addConversions20()
    addConversions21()
    addConversions22()
    addConversions23()
    addConversions24()
    addConversions25()
    addConversions26()
    addConversions27()
    addConversions28()
    addConversions30()
    addConversions31()
    addConversions32()
    addConversions33()
    addConversions34()
    addConversions35()
    addConversions36()
    addConversions37()
    addConversions38()
    addConversions39()
    addConversions41()
    addConversions42()
    addConversions43()
    addConversions44()
    addConversions45()
    addConversions46()
    addConversions47()
    addConversions49()
    addConversions50()
    addConversions51()
    addConversions52()
    addConversions53()
    addConversions54()
    addConversions55()
    addConversions56()
    addConversions57()
    addConversions58()
    addConversions59()
    addConversions60()
    addConversions61()
    addConversions62()
    addConversions65()
    addConversions66()
    addConversions67()
    addConversions68()
    addConversions69()
    addConversions71()
    addConversions73()
    addConversions74()
    addConversions75()
    addConversions76()
    addConversions77()
    addConversions78()
    addConversions79()
    addConversions80()
    addConversions81()
    addConversions83()
    addConversions85()
    addConversions86()
    addConversions87()
    addConversions88()
    addConversions89()
    addConversions90()
    addConversions91()
    addConversions92()
    addConversions93()
    addConversions94()
    addConversions96()
    addConversions97()
    addConversions98()
    addConversions99()
    addConversions101()
    addConversions102()
    addConversions103()
    addConversions104()
    addConversions105()
    addConversions106()
    addConversions107()
    addConversions108()
    addConversions109()
    addConversions111()
    addConversions112()
    addConversions113()
    addConversions114()
    addConversions115()
    addConversions116()
    addConversions117()
    addConversions118()
    addConversions119()
    addConversions120()
    addConversions122()
    addConversions123()
    addConversions125()
    addConversions126()
    addConversions127()
    addConversions128()
    addConversions129()
    addConversions130()
    addConversions131()
    addConversions132()
    addConversions134()
    addConversions135()
    addConversions136()
    addConversions137()
    addConversions138()
    addConversions140()
    addConversions141()
    addConversions142()
    addConversions143()
    addConversions144()
    addConversions145()
    addConversions146()
    addConversions147()
    addConversions148()
    addConversions149()
    addConversions150()
    addConversions151()
    addConversions152()
    addConversions153()
    addConversions155()
    addConversions156()
    addConversions157()
    addConversions159()
    addConversions161()
    addConversions162()
    addConversions163()
    addConversions164()
    addConversions165()
    addConversions166()
    addConversions167()
    addConversions168()
    addConversions169()
    addConversions170()
    addConversions171()
    addConversions172()
    addConversions173()
    addConversions174()
    addConversions175()
    addConversions176()
    addConversions177()
    addConversions178()
    addConversions180()
    addConversions181()
    addConversions182()
    addConversions183()
    addConversions184()
    addConversions185()
    addConversions186()
    addConversions188()
    addConversions189()
    addConversions190()
    addConversions191()
    addConversions192()
    addConversions193()
    addConversions194()
    addConversions195()
    addConversions196()
    addConversions198()
    addConversions199()
}

private fun CaDiagnosticConverterBuilder.addConversions1() {
    add(CfirErrors.REDECLARATION) { cfirDiagnostic ->
        RedeclarationImpl(
            cfirDiagnostic.a.map { string ->
                string
            },
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_IMMUT) { cfirDiagnostic ->
        PropertyHaveSameDeclarationInInheritImmutImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.COMMON_GENERIC_FROZEN_NOT_SUPPORTED) { cfirDiagnostic ->
        CommonGenericFrozenNotSupportedImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.JAVA_MIRROR_METHOD_ARG_MUST_BE_JAVA_MIRROR) { cfirDiagnostic ->
        JavaMirrorMethodArgMustBeJavaMirrorImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions3() {
    add(CfirErrors.DEPRECATION_WEAKENING) { cfirDiagnostic ->
        DeprecationWeakeningImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.APILEVEL_MULTI_ANNO) { cfirDiagnostic ->
        ApilevelMultiAnnoImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_UNRESOLVED) { cfirDiagnostic ->
        MacroUnresolvedImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions4() {
    add(CfirErrors.STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE) { cfirDiagnostic ->
        StaticCannotBeOpenAbstractOverrideImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.CJMAPPING_METHOD_RET_UNSUPPORTED) { cfirDiagnostic ->
        CjmappingMethodRetUnsupportedImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions5() {
    add(CfirErrors.MOCK_FROZEN_UNSUPPORTED) { cfirDiagnostic ->
        MockFrozenUnsupportedImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_CANNOT_FIND_DEPENDENCY_BCHIR) { cfirDiagnostic ->
        MacroCannotFindDependencyBchirImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_EXPAND_ATEXCL) { cfirDiagnostic ->
        MacroExpandAtexclImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions6() {
    add(CfirErrors.VARRAY_SIZE_MISMATCH) { cfirDiagnostic ->
        VarraySizeMismatchImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.c),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.CLASS_NOT_OPEN_FOR_INHERITANCE) { cfirDiagnostic ->
        ClassNotOpenForInheritanceImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL) { cfirDiagnostic ->
        InterfaceCallWithUnimplementedCallImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.INOUT_MUST_BE_VAR_VARIABLE) { cfirDiagnostic ->
        InoutMustBeVarVariableImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.EXPLICITLY_ABSTRACT_ONLY_FOR_CJMP_ABSTRACT_CLASS) { cfirDiagnostic ->
        ExplicitlyAbstractOnlyForCjmpAbstractClassImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.IFAVAILABLE_ARG_NOT_LITERAL) { cfirDiagnostic ->
        IfavailableArgNotLiteralImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.UNUSED_EXPRESSION) { cfirDiagnostic ->
        UnusedExpressionImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_EVALUATE_FAILED) { cfirDiagnostic ->
        MacroEvaluateFailedImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions7() {
    add(CfirErrors.STATIC_FUNCTION_OVERLOAD_CONFLICTS) { cfirDiagnostic ->
        StaticFunctionOverloadConflictsImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions8() {
    add(CfirErrors.DEPRECATED_MODIFIER_FOR_TARGET) { cfirDiagnostic ->
        DeprecatedModifierForTargetImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.AMBIGUOUS_MATCH_PRIMITIVE_EXTEND) { cfirDiagnostic ->
        AmbiguousMatchPrimitiveExtendImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b.map { name ->
                name
            },
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.SPECIFIC_HAS_DIFFERENT_PARAMETER) { cfirDiagnostic ->
        SpecificHasDifferentParameterImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions9() {
    add(CfirErrors.DEPRECATED_WARNING) { cfirDiagnostic ->
        DeprecatedWarningImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic.c,
            cfirDiagnostic.d,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.OBJC_INTEROP_METHOD_PARAM_MUST_BE_OBJC_COMPATIBLE) { cfirDiagnostic ->
        ObjcInteropMethodParamMustBeObjcCompatibleImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.OBJC_MIRROR_SUBTYPE_CANNOT_MULTIPLE_INHERIT) { cfirDiagnostic ->
        ObjcMirrorSubtypeCannotMultipleInheritImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions10() {
    add(CfirErrors.EXTEND_SUPER_NOT_ALLOWED) { cfirDiagnostic ->
        ExtendSuperNotAllowedImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.EXTEND_CHECK_SEQUENCE_CANNOT_DECIDE) { cfirDiagnostic ->
        ExtendCheckSequenceCannotDecideImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.OBJC_MIRROR_SUBTYPE_MUST_INHERIT_MIRROR) { cfirDiagnostic ->
        ObjcMirrorSubtypeMustInheritMirrorImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions11() {
    add(CfirErrors.AMBIGUOUS_USE) { cfirDiagnostic ->
        AmbiguousUseImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.CAPTURE_BEFORE_INITIALIZATION) { cfirDiagnostic ->
        CaptureBeforeInitializationImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.FOREIGN_NAME_CONFLICTING_DERIVED_ANNOTATION) { cfirDiagnostic ->
        ForeignNameConflictingDerivedAnnotationImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic.c,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions14() {
    add(CfirErrors.MACRO_EXECUTOR_TIMEOUT) { cfirDiagnostic ->
        MacroExecutorTimeoutImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions16() {
    add(CfirErrors.INHERIT_MEMBER_KIND_INCONSISTENT) { cfirDiagnostic ->
        InheritMemberKindInconsistentImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic.c,
            cfirDiagnostic.d,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions17() {
    add(CfirErrors.NO_CONSTRUCTOR) { cfirDiagnostic ->
        NoConstructorImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.STATIC_VARIABLE_USE_GENERIC_PARAMETER) { cfirDiagnostic ->
        StaticVariableUseGenericParameterImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions18() {
    add(CfirErrors.HIDE_MULTI_ANNOTATION) { cfirDiagnostic ->
        HideMultiAnnotationImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions19() {
    add(CfirErrors.INVALID_NODE_AFTER_CHECK) { cfirDiagnostic ->
        InvalidNodeAfterCheckImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions20() {
    add(CfirErrors.DIFFERENT_OR_PATTERN) { cfirDiagnostic ->
        DifferentOrPatternImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.HIDE_AT_FUNC_PARAM) { cfirDiagnostic ->
        HideAtFuncParamImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions21() {
    add(CfirErrors.EXTEND_IMMUTABLE_MUT_INTERFACE) { cfirDiagnostic ->
        ExtendImmutableMutInterfaceImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION) { cfirDiagnostic ->
        InferredTypeVariableIntoEmptyIntersectionImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b.map { coneCangJieType ->
                cfirSymbolBuilder.typeBuilder.buildType(coneCangJieType)
            },
            cfirDiagnostic.c,
            cfirDiagnostic.d,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions22() {
    add(CfirErrors.REDUNDANT_MODIFIER_FOR_TARGET) { cfirDiagnostic ->
        RedundantModifierForTargetImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.GENERIC_PARAM_DIRECTLY_RECURSIVE) { cfirDiagnostic ->
        GenericParamDirectlyRecursiveImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.EXTEND_ILLEGAL_MEMBER) { cfirDiagnostic ->
        ExtendIllegalMemberImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.CLASS_CONST_INIT_WITH_VAR) { cfirDiagnostic ->
        ClassConstInitWithVarImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions23() {
    add(CfirErrors.CONFLICTING_OVERLOADS) { cfirDiagnostic ->
        ConflictingOverloadsImpl(
            cfirDiagnostic.a.map { string ->
                string
            },
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.ONLY_CFUNC_CAN_USE_ANNOTATION) { cfirDiagnostic ->
        OnlyCfuncCanUseAnnotationImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_EXECUTOR_PROTOCOL_ERROR) { cfirDiagnostic ->
        MacroExecutorProtocolErrorImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions24() {
    add(CfirErrors.EXTEND_NOT_INTERFACE) { cfirDiagnostic ->
        ExtendNotInterfaceImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.RETURN_TYPE_MISMATCH) { cfirDiagnostic ->
        ReturnTypeMismatchImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.b),
            cfirDiagnostic.c,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.PARAM_COUNT_MISMATCH) { cfirDiagnostic ->
        ParamCountMismatchImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.INVALID_MEMBER_VISIBILITY_IN_CLASS) { cfirDiagnostic ->
        InvalidMemberVisibilityInClassImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions25() {
    add(CfirErrors.SUPER_TYPES_SELF_REFERENCE) { cfirDiagnostic ->
        SuperTypesSelfReferenceImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.ILLEGAL_SCOPE_USE_OF_ANNOTATION) { cfirDiagnostic ->
        IllegalScopeUseOfAnnotationImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions26() {
    add(CfirErrors.PROPERTY_MUST_IMPLEMENT_BOTH) { cfirDiagnostic ->
        PropertyMustImplementBothImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.UNSUPPORTED_TYPE_ARGUMENT_IN_JAVA_INTEROP) { cfirDiagnostic ->
        UnsupportedTypeArgumentInJavaInteropImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions27() {
    add(CfirErrors.APILEVEL_SYSCAP_WARNING) { cfirDiagnostic ->
        ApilevelSyscapWarningImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions28() {
    add(CfirErrors.INVALID_UNARY_EXPR_WITH_TARGET) { cfirDiagnostic ->
        InvalidUnaryExprWithTargetImpl(
            cfirDiagnostic.a,
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.b),
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.c),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.CJMAPPING_DECL_NOT_SUPPORTED) { cfirDiagnostic ->
        CjmappingDeclNotSupportedImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions30() {
    add(CfirErrors.TUPLE_PATTERN_NOT_MATCH) { cfirDiagnostic ->
        TuplePatternNotMatchImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MOCK_DOESNT_SUPPORT_MOCKING) { cfirDiagnostic ->
        MockDoesntSupportMockingImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic.c,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions31() {
    add(CfirErrors.NEW_INFERENCE_ERROR) { cfirDiagnostic ->
        NewInferenceErrorImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.COMMON_NON_EXHAUSTIVE_PLATFORM_EXHAUSTIVE_MISMATCH) { cfirDiagnostic ->
        CommonNonExhaustivePlatformExhaustiveMismatchImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions32() {
    add(CfirErrors.GENERIC_ARGUMENT_NO_MATCH) { cfirDiagnostic ->
        GenericArgumentNoMatchImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.APILEVEL_REF_HIGHER) { cfirDiagnostic ->
        ApilevelRefHigherImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic.c,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_EXPANSION_FAILED) { cfirDiagnostic ->
        MacroExpansionFailedImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_UNDECLARED_IDENTIFIER) { cfirDiagnostic ->
        MacroUndeclaredIdentifierImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions33() {
    add(CfirErrors.NOT_MATCHED) { cfirDiagnostic ->
        NotMatchedImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic.c,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions34() {
    add(CfirErrors.CLASSIFIER_REDECLARATION) { cfirDiagnostic ->
        ClassifierRedeclarationImpl(
            cfirDiagnostic.a.map { string ->
                string
            },
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions35() {
    add(CfirErrors.ARGUMENT_TYPE_MISMATCH) { cfirDiagnostic ->
        ArgumentTypeMismatchImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.b),
            cfirDiagnostic.c,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.ASSIGNMENT_TYPE_MISMATCH) { cfirDiagnostic ->
        AssignmentTypeMismatchImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.b),
            cfirDiagnostic.c,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.INVALID_UNARY_EXPR) { cfirDiagnostic ->
        InvalidUnaryExprImpl(
            cfirDiagnostic.a,
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.b),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.JAVA_IMPL_CANNOT_INHERIT_PURE_CANGJIE_TYPE) { cfirDiagnostic ->
        JavaImplCannotInheritPureCangjieTypeImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions36() {
    add(CfirErrors.INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION) { cfirDiagnostic ->
        InferredTypeVariableIntoPossibleEmptyIntersectionImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b.map { coneCangJieType ->
                cfirSymbolBuilder.typeBuilder.buildType(coneCangJieType)
            },
            cfirDiagnostic.c,
            cfirDiagnostic.d,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.OPTIONAL_CHAIN_NON_OPTIONAL) { cfirDiagnostic ->
        OptionalChainNonOptionalImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MISSING_JAVA_INTEROP_ANNOTATION) { cfirDiagnostic ->
        MissingJavaInteropAnnotationImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions37() {
    add(CfirErrors.INHERIT_THREAD_CONTEXT_NOT_OPEN) { cfirDiagnostic ->
        InheritThreadContextNotOpenImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.CSTRUCT_CANNOT_IMPL_INTERFACES) { cfirDiagnostic ->
        CstructCannotImplInterfacesImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.IFAVAILABLE_LEVEL_LIMIT) { cfirDiagnostic ->
        IfavailableLevelLimitImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions38() {
    add(CfirErrors.CONST_EVAL_SHIFT_COUNT_OVERFLOW) { cfirDiagnostic ->
        ConstEvalShiftCountOverflowImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.INHERIT_NOT_RETURN_THIS) { cfirDiagnostic ->
        InheritNotReturnThisImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions39() {
    add(CfirErrors.JAVA_NON_JTYPE) { cfirDiagnostic ->
        JavaNonJtypeImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic.c,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_DIAG_REPORT_WARNING) { cfirDiagnostic ->
        MacroDiagReportWarningImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions41() {
    add(CfirErrors.REDEF_INSTANCE_ERROR) { cfirDiagnostic ->
        RedefInstanceErrorImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.INVISIBLE_MEMBER) { cfirDiagnostic ->
        InvisibleMemberImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.VAR_IN_OR_PATTERN) { cfirDiagnostic ->
        VarInOrPatternImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.RETURN_IN_TRY_HANDLE_BLOCK) { cfirDiagnostic ->
        ReturnInTryHandleBlockImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions42() {
    add(CfirErrors.UNABLE_TO_INFER_DECL) { cfirDiagnostic ->
        UnableToInferDeclImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.OBJC_POINTER_ARGUMENT_MUST_BE_OBJC_COMPATIBLE) { cfirDiagnostic ->
        ObjcPointerArgumentMustBeObjcCompatibleImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_ALIAS_CONFLICT) { cfirDiagnostic ->
        MacroAliasConflictImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b.map { fqName ->
                fqName
            },
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions43() {
    add(CfirErrors.GENERIC_PARAM_EXIST_IN_CLASS_IRRELEVANT_UPPERBOUND_RECURSIVELY) { cfirDiagnostic ->
        GenericParamExistInClassIrrelevantUpperboundRecursivelyImpl(
            cfirDiagnostic.a,
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.b),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.COMMON_ASSIGN_TO_COMMON_IMMUTABLE_IN_CTOR) { cfirDiagnostic ->
        CommonAssignToCommonImmutableInCtorImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions44() {
    add(CfirErrors.COMMON_OPEN_CLASS_NO_INIT) { cfirDiagnostic ->
        CommonOpenClassNoInitImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_INVALID_ESCAPE) { cfirDiagnostic ->
        MacroInvalidEscapeImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions45() {
    add(CfirErrors.IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION) { cfirDiagnostic ->
        ImmutableFunctionCannotAccessMutableFunctionImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions46() {
    add(CfirErrors.PATTERN_NOT_MATCH) { cfirDiagnostic ->
        PatternNotMatchImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.NO_CONST_INIT) { cfirDiagnostic ->
        NoConstInitImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions47() {
    add(CfirErrors.IMPLICIT_RESUME_OUTSIDE_HANDLER) { cfirDiagnostic ->
        ImplicitResumeOutsideHandlerImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions49() {
    add(CfirErrors.CLASS_UNINITIALIZED_FIELD) { cfirDiagnostic ->
        ClassUninitializedFieldImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.CONST_EVAL_DIVIDE_BY_ZERO) { cfirDiagnostic ->
        ConstEvalDivideByZeroImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions50() {
    add(CfirErrors.UNABLE_TO_INFER_RETURN_TYPE) { cfirDiagnostic ->
        UnableToInferReturnTypeImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.PROPERTY_MUST_HAVE_ACCESSORS) { cfirDiagnostic ->
        PropertyMustHaveAccessorsImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.ANNOTATION_ARG_TARGET_ARRAY_LIT) { cfirDiagnostic ->
        AnnotationArgTargetArrayLitImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.ANNOTATION_NOT_APPLICABLE_JFFI) { cfirDiagnostic ->
        AnnotationNotApplicableJffiImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_EXPECT_MACRO_DEFINITION) { cfirDiagnostic ->
        MacroExpectMacroDefinitionImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_CYCLE) { cfirDiagnostic ->
        MacroCycleImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b.map { string ->
                string
            },
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions51() {
    add(CfirErrors.INOUT_MODIFY_HEAP_VARIABLE) { cfirDiagnostic ->
        InoutModifyHeapVariableImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.SPECIFIC_HAS_DIFFERENT_TYPE) { cfirDiagnostic ->
        SpecificHasDifferentTypeImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.IFAVAILABLE_UNKNOWN_ARG_NAME) { cfirDiagnostic ->
        IfavailableUnknownArgNameImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions52() {
    add(CfirErrors.UNREACHABLE_PATTERN) { cfirDiagnostic ->
        UnreachablePatternImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.ONLY_LITERAL_SUPPORT) { cfirDiagnostic ->
        OnlyLiteralSupportImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions53() {
    add(CfirErrors.CANNOT_ASSIGN_TO_IMMUTABLE) { cfirDiagnostic ->
        CannotAssignToImmutableImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.INSTANCE_FUNC_CANNOT_BE_USED_IN_FINALIZER) { cfirDiagnostic ->
        InstanceFuncCannotBeUsedInFinalizerImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.USELESS_COMMAND_TYPE) { cfirDiagnostic ->
        UselessCommandTypeImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MOCK_WRONG_STATIC_DECL) { cfirDiagnostic ->
        MockWrongStaticDeclImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions54() {
    add(CfirErrors.COMMAND_INCOMPATIBLE_TYPE) { cfirDiagnostic ->
        CommandIncompatibleTypeImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.CANNOT_ASSIGN_TO_SUBSCRIPT) { cfirDiagnostic ->
        CannotAssignToSubscriptImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.DEPRECATION_REDEF_ERROR) { cfirDiagnostic ->
        DeprecationRedefErrorImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS) { cfirDiagnostic ->
        CommonDirectExtensionHasDuplicatePrivateMembersImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic.c,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_UNDEFINED_PACKAGE) { cfirDiagnostic ->
        MacroUndefinedPackageImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions55() {
    add(CfirErrors.MISMATCHING_HANDLE_BLOCK) { cfirDiagnostic ->
        MismatchingHandleBlockImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.b),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions56() {
    add(CfirErrors.COMMON_DIRECT_EXTENSION_HAS_COMMON_PRIVATE_MEMBERS) { cfirDiagnostic ->
        CommonDirectExtensionHasCommonPrivateMembersImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_NOT_EXPANDED) { cfirDiagnostic ->
        MacroNotExpandedImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions57() {
    add(CfirErrors.JAVA_MIRROR_PROP_MUST_BE_JAVA_MIRROR) { cfirDiagnostic ->
        JavaMirrorPropMustBeJavaMirrorImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions58() {
    add(CfirErrors.INVALID_SUBSCRIPT_ASSIGN_PARAMETER) { cfirDiagnostic ->
        InvalidSubscriptAssignParameterImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions59() {
    add(CfirErrors.MACRO_EXECUTOR_SERVER_DISCONNECTED) { cfirDiagnostic ->
        MacroExecutorServerDisconnectedImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions60() {
    add(CfirErrors.SPECIFIC_HAS_DIFFERENT_SUPER_TYPE) { cfirDiagnostic ->
        SpecificHasDifferentSuperTypeImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions61() {
    add(CfirErrors.INHERIT_ABSTRACT_CLASS_STATIC_UNIMPLEMENT_FUNC) { cfirDiagnostic ->
        InheritAbstractClassStaticUnimplementFuncImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic.c,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions62() {
    add(CfirErrors.INVALID_CFUNC_RETURN_TYPE) { cfirDiagnostic ->
        InvalidCfuncReturnTypeImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions65() {
    add(CfirErrors.PARAM_NAMED_MISMATCHED) { cfirDiagnostic ->
        ParamNamedMismatchedImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.RANGE_STEP_CANNOT_BE_ZERO) { cfirDiagnostic ->
        RangeStepCannotBeZeroImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.GENERIC_STATIC_ACCESS) { cfirDiagnostic ->
        GenericStaticAccessImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.EXTEND_MEMBER_CANNOT_SHADOW) { cfirDiagnostic ->
        ExtendMemberCannotShadowImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.JAVA_HAS_DEFAULT_CONFLICT_WITH_STATIC) { cfirDiagnostic ->
        JavaHasDefaultConflictWithStaticImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.APILEVEL_MULTI_DIFF_SYSCAP) { cfirDiagnostic ->
        ApilevelMultiDiffSyscapImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions66() {
    add(CfirErrors.CONFLICT_WITH_SUB_PACKAGE) { cfirDiagnostic ->
        ConflictWithSubPackageImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions67() {
    add(CfirErrors.MEMBER_NOT_IMPORTED) { cfirDiagnostic ->
        MemberNotImportedImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.DEPRECATED_ERROR) { cfirDiagnostic ->
        DeprecatedErrorImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic.c,
            cfirDiagnostic.d,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.JAVA_MIRROR_CANNOT_BE_EXTENDED_WITH_INTERFACE) { cfirDiagnostic ->
        JavaMirrorCannotBeExtendedWithInterfaceImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.OBJC_IMPL_MUST_HAVE_OBJC_MIRROR_SUPER_CLASS) { cfirDiagnostic ->
        ObjcImplMustHaveObjcMirrorSuperClassImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions68() {
    add(CfirErrors.EXPLICIT_SUPER_CALL_REQUIRED) { cfirDiagnostic ->
        ExplicitSuperCallRequiredImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions69() {
    add(CfirErrors.WRONG_MODIFIER_TARGET) { cfirDiagnostic ->
        WrongModifierTargetImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.OBJC_INTEROP_TOPLEVEL_RET_MUST_BE_OBJC_COMPATIBLE) { cfirDiagnostic ->
        ObjcInteropToplevelRetMustBeObjcCompatibleImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MOCK_UNSUPPORTED_TYPE) { cfirDiagnostic ->
        MockUnsupportedTypeImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions71() {
    add(CfirErrors.ANNOTATION_CUSTOM_PLACE) { cfirDiagnostic ->
        AnnotationCustomPlaceImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.INOUT_MISMATCH) { cfirDiagnostic ->
        InoutMismatchImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.JAVA_INVALID_UNIT) { cfirDiagnostic ->
        JavaInvalidUnitImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic.c,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.APILEVEL_SYSCAP_ERROR) { cfirDiagnostic ->
        ApilevelSyscapErrorImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions73() {
    add(CfirErrors.TRAILING_LAMBDA_CANNOT_USED_FOR_NON_FUNCTION) { cfirDiagnostic ->
        TrailingLambdaCannotUsedForNonFunctionImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.EXPORT_EXTEND_DEPEND_NON_EXPORT_EXTEND) { cfirDiagnostic ->
        ExportExtendDependNonExportExtendImpl(
            cfirDiagnostic.a.map { name ->
                name
            },
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.ANNOTATION_NON_PUBLIC) { cfirDiagnostic ->
        AnnotationNonPublicImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_EXECUTOR_UNAVAILABLE) { cfirDiagnostic ->
        MacroExecutorUnavailableImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions74() {
    add(CfirErrors.DEPRECATION_REDEF_WARNING) { cfirDiagnostic ->
        DeprecationRedefWarningImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.SPECIFIC_PRIMARY_UNMATCHED_VAR_DECL) { cfirDiagnostic ->
        SpecificPrimaryUnmatchedVarDeclImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.OBJC_MIRROR_MUST_INHERIT_MIRROR) { cfirDiagnostic ->
        ObjcMirrorMustInheritMirrorImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions75() {
    add(CfirErrors.COMMAND_HANDLE_TYPE_ERROR) { cfirDiagnostic ->
        CommandHandleTypeErrorImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.CANNOT_DEFINE_VAR_IN_CONST_FUNCTION) { cfirDiagnostic ->
        CannotDefineVarInConstFunctionImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.OBJC_MIRROR_DECL_CANNOT_INHERIT) { cfirDiagnostic ->
        ObjcMirrorDeclCannotInheritImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions76() {
    add(CfirErrors.CANNOT_REF_TO_PKG_NAME) { cfirDiagnostic ->
        CannotRefToPkgNameImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.DEFINE_JAVA_ANNOTATION) { cfirDiagnostic ->
        DefineJavaAnnotationImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.OBJC_INTEROP_NOT_SUPPORTED) { cfirDiagnostic ->
        ObjcInteropNotSupportedImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions77() {
    add(CfirErrors.PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_MUT) { cfirDiagnostic ->
        PropertyHaveSameDeclarationInInheritMutImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions78() {
    add(CfirErrors.FLOAT_LITERAL_TOO_LARGE) { cfirDiagnostic ->
        FloatLiteralTooLargeImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.INTERPOLATION_IN_CONST_PATTERN) { cfirDiagnostic ->
        InterpolationInConstPatternImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions79() {
    add(CfirErrors.MACRO_EXPECT_ATTRIBUTED_MACRO) { cfirDiagnostic ->
        MacroExpectAttributedMacroImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions80() {
    add(CfirErrors.ANNOTATION_ERROR_OBJECT) { cfirDiagnostic ->
        AnnotationErrorObjectImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions81() {
    add(CfirErrors.USE_FUNC_CAPTURE_VAR_ALONE) { cfirDiagnostic ->
        UseFuncCaptureVarAloneImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions83() {
    add(CfirErrors.GENERIC_UPPER_BOUNDS_MUST_BE_JAVA_IN_JAVA) { cfirDiagnostic ->
        GenericUpperBoundsMustBeJavaInJavaImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.RESUMPTION_INCORRECT_RETURN_TYPE) { cfirDiagnostic ->
        ResumptionIncorrectReturnTypeImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.b),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions85() {
    add(CfirErrors.SUPER_TYPES_DUPLICATE) { cfirDiagnostic ->
        SuperTypesDuplicateImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.NOT_OVERLOAD_IN_MATCH) { cfirDiagnostic ->
        NotOverloadInMatchImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.UNUSED_IMPORT) { cfirDiagnostic ->
        UnusedImportImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions86() {
    add(CfirErrors.INVISIBLE_REFERENCE) { cfirDiagnostic ->
        InvisibleReferenceImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.ACCESSIBILITY_ERROR) { cfirDiagnostic ->
        AccessibilityErrorImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.RESUME_WRONG_RESUMPTION_TYPE) { cfirDiagnostic ->
        ResumeWrongResumptionTypeImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions87() {
    add(CfirErrors.SPECIFIC_HAS_DUPLICATE_EXTENSIONS) { cfirDiagnostic ->
        SpecificHasDuplicateExtensionsImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions88() {
    add(CfirErrors.CJMP_ABSTRACT_CLASS_MEMBER_HAS_NO_EXPLICIT_MODIFIER) { cfirDiagnostic ->
        CjmpAbstractClassMemberHasNoExplicitModifierImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic.c,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.CJMP_NON_SPECIFIC_ABSTRACT_MEMBER_IN_SPECIFIC_CLASS) { cfirDiagnostic ->
        CjmpNonSpecificAbstractMemberInSpecificClassImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_REEVALUATION_FAILED) { cfirDiagnostic ->
        MacroReevaluationFailedImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions89() {
    add(CfirErrors.RESUME_THROWING_MISMATCH_TYPE) { cfirDiagnostic ->
        ResumeThrowingMismatchTypeImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.PRIMITIVE_TYPE_AS_GENERICS_ARG) { cfirDiagnostic ->
        PrimitiveTypeAsGenericsArgImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions90() {
    add(CfirErrors.DEPRECATED_MODIFIER_CONTAINING_DECLARATION) { cfirDiagnostic ->
        DeprecatedModifierContainingDeclarationImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.CANNOT_USE_ANNOTATION_JFFI) { cfirDiagnostic ->
        CannotUseAnnotationJffiImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions91() {
    add(CfirErrors.IMPORT_ALIAS_CONFLICT) { cfirDiagnostic ->
        ImportAliasConflictImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.CORE_OBJECT_NOT_FOUND_WHEN_NO_PRELUDE) { cfirDiagnostic ->
        CoreObjectNotFoundWhenNoPreludeImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions92() {
    add(CfirErrors.INVALID_BINARY_OPERATOR) { cfirDiagnostic ->
        InvalidBinaryOperatorImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic.c,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.SPECIFIC_VAR_NOT_MATCH_LET) { cfirDiagnostic ->
        SpecificVarNotMatchLetImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions93() {
    add(CfirErrors.SPECIFIC_HAS_DIFFERENT_KIND) { cfirDiagnostic ->
        SpecificHasDifferentKindImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_EXPAND_CODE_SHOULD_NOT_HAVE_MACROCALL) { cfirDiagnostic ->
        MacroExpandCodeShouldNotHaveMacrocallImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions94() {
    add(CfirErrors.NO_MATCHING_OPERATOR_INVOKE) { cfirDiagnostic ->
        NoMatchingOperatorInvokeImpl(
            cfirDiagnostic.a,
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.b),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions96() {
    add(CfirErrors.EXTEND_INTERFACE_NOT_EXTENDABLE) { cfirDiagnostic ->
        ExtendInterfaceNotExtendableImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.TYPE_MISMATCH) { cfirDiagnostic ->
        TypeMismatchImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.b),
            cfirDiagnostic.c,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.INVALID_SUBSCRIPT_ASSIGN_PARAMETER_NUM) { cfirDiagnostic ->
        InvalidSubscriptAssignParameterNumImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.EXPLICITLY_ABSTRACT_CAN_NOT_HAVE_BODY) { cfirDiagnostic ->
        ExplicitlyAbstractCanNotHaveBodyImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions97() {
    add(CfirErrors.ENUM_SUPER_NOT_ALLOWED) { cfirDiagnostic ->
        EnumSuperNotAllowedImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.CONFLICTING_UPPER_BOUNDS) { cfirDiagnostic ->
        ConflictingUpperBoundsImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MOCK_FROZEN_REQUIRED) { cfirDiagnostic ->
        MockFrozenRequiredImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions98() {
    add(CfirErrors.UNRESOLVED_IMPORT) { cfirDiagnostic ->
        UnresolvedImportImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.OBJC_CTOR_MUST_HAVE_FOREIGN_NAME) { cfirDiagnostic ->
        ObjcCtorMustHaveForeignNameImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions99() {
    add(CfirErrors.JAVA_HAS_DEFAULT_ANNOTATION_ARGS) { cfirDiagnostic ->
        JavaHasDefaultAnnotationArgsImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions101() {
    add(CfirErrors.UNABLE_TO_INFER_EXPR) { cfirDiagnostic ->
        UnableToInferExprImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions102() {
    add(CfirErrors.UNRESOLVED_REFERENCE) { cfirDiagnostic ->
        UnresolvedReferenceImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions103() {
    add(CfirErrors.VARIABLE_OF_JAVA_TYPE) { cfirDiagnostic ->
        VariableOfJavaTypeImpl(
            cfirDiagnostic.a,
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.b),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions104() {
    add(CfirErrors.EXTEND_DUPLICATE_INTERFACE) { cfirDiagnostic ->
        ExtendDuplicateInterfaceImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.EXTEND_C_TYPE_NOT_ALLOWED) { cfirDiagnostic ->
        ExtendCTypeNotAllowedImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.USE_MUTABLE_FUNC_ALONE) { cfirDiagnostic ->
        UseMutableFuncAloneImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.SPECIFIC_HAS_DIFFERENT_ANNOTATION) { cfirDiagnostic ->
        SpecificHasDifferentAnnotationImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.OBJC_SETTER_NAME_ON_IMMUTABLE_PROP) { cfirDiagnostic ->
        ObjcSetterNameOnImmutablePropImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions105() {
    add(CfirErrors.ILLEGAL_EXTENDED_TYPE) { cfirDiagnostic ->
        IllegalExtendedTypeImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.INVALID_RETURN) { cfirDiagnostic ->
        InvalidReturnImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.USE_EXPR_WITHOUT_IMPORT) { cfirDiagnostic ->
        UseExprWithoutImportImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions106() {
    add(CfirErrors.INHERIT_MEMBER_TYPE_INCONSISTENT) { cfirDiagnostic ->
        InheritMemberTypeInconsistentImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic.c,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions107() {
    add(CfirErrors.DUPLICATE_INOUT_ARGUMENT) { cfirDiagnostic ->
        DuplicateInoutArgumentImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.JAVA_INCORRECT_USE_BETWEEN_TYPES) { cfirDiagnostic ->
        JavaIncorrectUseBetweenTypesImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions108() {
    add(CfirErrors.REPEATED_BOUND) { cfirDiagnostic ->
        RepeatedBoundImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions109() {
    add(CfirErrors.JAVA_UNSUPPORTED_DECL) { cfirDiagnostic ->
        JavaUnsupportedDeclImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic.c,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.OBJC_CJMAPPING_INHERITANCE_INTERFACE_NOT_SUPPORTED) { cfirDiagnostic ->
        ObjcCjmappingInheritanceInterfaceNotSupportedImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions111() {
    add(CfirErrors.INOUT_MODIFY_CSTRING_OR_ZEROSIZED) { cfirDiagnostic ->
        InoutModifyCstringOrZerosizedImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions112() {
    add(CfirErrors.JAVA_IMPL_REDEFINITION) { cfirDiagnostic ->
        JavaImplRedefinitionImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.OBJC_INTEROP_PROP_MUST_BE_OBJC_COMPATIBLE) { cfirDiagnostic ->
        ObjcInteropPropMustBeObjcCompatibleImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.FOREIGN_NAME_CONFLICTING_ANNOTATION) { cfirDiagnostic ->
        ForeignNameConflictingAnnotationImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions113() {
    add(CfirErrors.ANNOTATION_ERROR_ARG_NUM) { cfirDiagnostic ->
        AnnotationErrorArgNumImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.STATIC_MEMBER_IN_INTERFACE_MUST_HAS_BODY) { cfirDiagnostic ->
        StaticMemberInInterfaceMustHasBodyImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions114() {
    add(CfirErrors.EXTEND_ORPHAN_RULE) { cfirDiagnostic ->
        ExtendOrphanRuleImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.OVERRIDE_STATIC_ERROR) { cfirDiagnostic ->
        OverrideStaticErrorImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.NAMED_ARGUMENTS_NOT_ALLOWED) { cfirDiagnostic ->
        NamedArgumentsNotAllowedImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.FLOAT_LITERAL_TOO_SMALL) { cfirDiagnostic ->
        FloatLiteralTooSmallImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.SPECIFIC_HAS_DEPRECATED_ANNOTATION) { cfirDiagnostic ->
        SpecificHasDeprecatedAnnotationImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic.c,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions115() {
    add(CfirErrors.INVALID_SUBSCRIPT_ASSIGN_RETURN) { cfirDiagnostic ->
        InvalidSubscriptAssignReturnImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.NON_ABSTRACT_CLASS_CANNOT_BE_SEALED) { cfirDiagnostic ->
        NonAbstractClassCannotBeSealedImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.OBJC_MIRROR_INTEROPLIB_MUST_BE_IMPORTED) { cfirDiagnostic ->
        ObjcMirrorInteroplibMustBeImportedImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions116() {
    add(CfirErrors.VAR_IN_OR_CONDITION) { cfirDiagnostic ->
        VarInOrConditionImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions117() {
    add(CfirErrors.TOO_MANY_ARGUMENTS) { cfirDiagnostic ->
        TooManyArgumentsImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.OBJC_INTEROP_FIELD_MUST_BE_OBJC_COMPATIBLE) { cfirDiagnostic ->
        ObjcInteropFieldMustBeObjcCompatibleImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions118() {
    add(CfirErrors.ILLEGAL_THIS_OR_SUPER_CALL) { cfirDiagnostic ->
        IllegalThisOrSuperCallImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MULTIPLE_COMMON_IMPLEMENTATIONS) { cfirDiagnostic ->
        MultipleCommonImplementationsImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions119() {
    add(CfirErrors.NAMED_PARAMETER_NOT_FOUND) { cfirDiagnostic ->
        NamedParameterNotFoundImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT) { cfirDiagnostic ->
        GenericTypeShouldBeUsedWithTypeArgumentImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions120() {
    add(CfirErrors.CANNOT_INFER_PARAMETER_TYPE) { cfirDiagnostic ->
        CannotInferParameterTypeImpl(
            cfirSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(cfirDiagnostic.a),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.INVALID_USE_OF_JAVA_ANNOTATION) { cfirDiagnostic ->
        InvalidUseOfJavaAnnotationImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions122() {
    add(CfirErrors.PATTERN_INITIALIZER_TYPE_MISMATCH) { cfirDiagnostic ->
        PatternInitializerTypeMismatchImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.b),
            cfirDiagnostic.c,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.UNSAFE_FUNC_CAN_ONLY_BE_CALLED) { cfirDiagnostic ->
        UnsafeFuncCanOnlyBeCalledImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.VARRAY_ARG_TYPE_WITH_REFTYPE) { cfirDiagnostic ->
        VarrayArgTypeWithReftypeImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.RESUMPTION_HANDLE_TYPE_ERROR) { cfirDiagnostic ->
        ResumptionHandleTypeErrorImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions123() {
    add(CfirErrors.EXTEND_DEFAULT_IMPLEMENTATION_CONFLICT) { cfirDiagnostic ->
        ExtendDefaultImplementationConflictImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.COMMAND_RESUMPTION_MISMATCH) { cfirDiagnostic ->
        CommandResumptionMismatchImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.b),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions125() {
    add(CfirErrors.ARGUMENT_PASSED_TWICE) { cfirDiagnostic ->
        ArgumentPassedTwiceImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.ABSTRACT_MEMBER_NOT_IMPLEMENTED) { cfirDiagnostic ->
        AbstractMemberNotImplementedImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.SPECIFIC_INIT_COMMON_PRIMARY_CONSTRUCTOR) { cfirDiagnostic ->
        SpecificInitCommonPrimaryConstructorImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.OBJC_MIRROR_SUBTYPE_MUST_BE_ANNOTATED) { cfirDiagnostic ->
        ObjcMirrorSubtypeMustBeAnnotatedImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions126() {
    add(CfirErrors.JAVA_MIRROR_METHOD_RET_UNSUPPORTED) { cfirDiagnostic ->
        JavaMirrorMethodRetUnsupportedImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MOCK_DISABLED) { cfirDiagnostic ->
        MockDisabledImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions127() {
    add(CfirErrors.GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS) { cfirDiagnostic ->
        GenericInstantiationCausesAmbiguousFunctionsImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions128() {
    add(CfirErrors.NO_VALUE_FOR_PARAMETER) { cfirDiagnostic ->
        NoValueForParameterImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.CANNOT_OVERRIDE_INVISIBLE_MEMBER) { cfirDiagnostic ->
        CannotOverrideInvisibleMemberImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions129() {
    add(CfirErrors.IMPORT_CONFLICT) { cfirDiagnostic ->
        ImportConflictImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.SPECIFIC_MEMBER_MUST_HAVE_IMPLEMENTATION) { cfirDiagnostic ->
        SpecificMemberMustHaveImplementationImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions130() {
    add(CfirErrors.UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE) { cfirDiagnostic ->
        UpperBoundMustBeClassOrInterfaceImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.HIDE_DIFF_PARAM) { cfirDiagnostic ->
        HideDiffParamImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions131() {
    add(CfirErrors.INCOMPATIBLE_MODIFIERS) { cfirDiagnostic ->
        IncompatibleModifiersImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.JAVA_MIRROR_SUBTYPE_MUST_BE_ANNOTATED) { cfirDiagnostic ->
        JavaMirrorSubtypeMustBeAnnotatedImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions132() {
    add(CfirErrors.DEPRECATED_MODIFIER_PAIR) { cfirDiagnostic ->
        DeprecatedModifierPairImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions134() {
    add(CfirErrors.INVALID_RETURN_IN_STATIC_INIT) { cfirDiagnostic ->
        InvalidReturnInStaticInitImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.EXPECT_CONST) { cfirDiagnostic ->
        ExpectConstImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions135() {
    add(CfirErrors.MUT_ONLY_ON_FUNCTION) { cfirDiagnostic ->
        MutOnlyOnFunctionImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_INVALID_INPUT_TOKENS) { cfirDiagnostic ->
        MacroInvalidInputTokensImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_SAME_PACKAGE_DEF_CALL) { cfirDiagnostic ->
        MacroSamePackageDefCallImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions136() {
    add(CfirErrors.ANNOTATION_ERROR_ARG_RANGE) { cfirDiagnostic ->
        AnnotationErrorArgRangeImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions137() {
    add(CfirErrors.OVERRIDING_RETURN_TYPE_MISMATCH) { cfirDiagnostic ->
        OverridingReturnTypeMismatchImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.b),
            cfirDiagnostic.c,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_EXPECT_PLAIN_MACRO) { cfirDiagnostic ->
        MacroExpectPlainMacroImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions138() {
    add(CfirErrors.AMBIGUOUS_FUNCTION_CALL) { cfirDiagnostic ->
        AmbiguousFunctionCallImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.GENERIC_NO_METHOD_MATCH_IN_UPPER_BOUNDS) { cfirDiagnostic ->
        GenericNoMethodMatchInUpperBoundsImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.CANNOT_MODIFY_VAR) { cfirDiagnostic ->
        CannotModifyVarImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.INOUT_CAN_ONLY_USED_IN_CFUNC_CALLING) { cfirDiagnostic ->
        InoutCanOnlyUsedInCfuncCallingImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.JAVA_MIRROR_CTOR_ARG_MUST_BE_JAVA_MIRROR) { cfirDiagnostic ->
        JavaMirrorCtorArgMustBeJavaMirrorImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.HIDE_MISSING_HIDE) { cfirDiagnostic ->
        HideMissingHideImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions140() {
    add(CfirErrors.MACRO_CANNOT_OPEN_LIB) { cfirDiagnostic ->
        MacroCannotOpenLibImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions141() {
    add(CfirErrors.UNABLE_TO_INFER_GENERIC_FUNC) { cfirDiagnostic ->
        UnableToInferGenericFuncImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions142() {
    add(CfirErrors.BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION) { cfirDiagnostic ->
        BuilderInferenceMultiLambdaRestrictionImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions143() {
    add(CfirErrors.INTERFACE_CANNOT_INHERIT_CLASS) { cfirDiagnostic ->
        InterfaceCannotInheritClassImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.INTERFACE_SUPER_NOT_ALLOWED) { cfirDiagnostic ->
        InterfaceSuperNotAllowedImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MEET_CONSTRAINT_INDIRECTLY) { cfirDiagnostic ->
        MeetConstraintIndirectlyImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.JAVA_MIRROR_INTEROPLIB_MUST_BE_IMPORTED) { cfirDiagnostic ->
        JavaMirrorInteroplibMustBeImportedImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions144() {
    add(CfirErrors.EFFECTS_FEATURE_DISABLED) { cfirDiagnostic ->
        EffectsFeatureDisabledImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.COMMON_PACKAGE_HAS_MAIN) { cfirDiagnostic ->
        CommonPackageHasMainImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions145() {
    add(CfirErrors.ENUM_PATTERN_PARAM_SIZE_ERROR) { cfirDiagnostic ->
        EnumPatternParamSizeErrorImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.parse_this_type_not_allow) { cfirDiagnostic ->
        ParseThisTypeNotAllowImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions146() {
    add(CfirErrors.JAVA_IMPL_CANNOT_BE_EXTENDED_WITH_INTERFACE) { cfirDiagnostic ->
        JavaImplCannotBeExtendedWithInterfaceImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions147() {
    add(CfirErrors.ANNOTATION_NO_CONST_INIT) { cfirDiagnostic ->
        AnnotationNoConstInitImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.GENERIC_CONSTRAINT_NOT_LOOSER) { cfirDiagnostic ->
        GenericConstraintNotLooserImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.INHERIT_THREAD_CONTEXT_INVALID) { cfirDiagnostic ->
        InheritThreadContextInvalidImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.COMMON_GENERIC_RENAME_NOT_SUPPORTED) { cfirDiagnostic ->
        CommonGenericRenameNotSupportedImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_EXPAND_FAILED) { cfirDiagnostic ->
        MacroExpandFailedImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions148() {
    add(CfirErrors.CANNOT_INHERIT_SEALED) { cfirDiagnostic ->
        CannotInheritSealedImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic.c,
            cfirDiagnostic.d,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.CJMP_PARAMETER_DEFAULT_VALUE_BOTH_SIDES) { cfirDiagnostic ->
        CjmpParameterDefaultValueBothSidesImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions149() {
    add(CfirErrors.INVALID_CFUNC_PARAMETER_TYPE) { cfirDiagnostic ->
        InvalidCfuncParameterTypeImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions150() {
    add(CfirErrors.MISMATCHED_TYPES_BECAUSE) { cfirDiagnostic ->
        MismatchedTypesBecauseImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.b),
            cfirDiagnostic.c,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.VARRAY_SUBSCRIPT_NUM) { cfirDiagnostic ->
        VarraySubscriptNumImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions151() {
    add(CfirErrors.CATCH_TYPE_MUST_EXTEND_EXCEPTION) { cfirDiagnostic ->
        CatchTypeMustExtendExceptionImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.INVALID_INOUT_ARGUMENT) { cfirDiagnostic ->
        InvalidInoutArgumentImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions152() {
    add(CfirErrors.NEED_NAMED_ARGUMENT) { cfirDiagnostic ->
        NeedNamedArgumentImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MATCH_CASE_HAS_NO_TYPE) { cfirDiagnostic ->
        MatchCaseHasNoTypeImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.OPEN_ABSTRACT_SPECIFIC_CAN_NOT_REPLACE_OPEN_COMMON) { cfirDiagnostic ->
        OpenAbstractSpecificCanNotReplaceOpenCommonImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions153() {
    add(CfirErrors.JAVA_HAS_DEFAULT_ANNOTATION_IS_IN_WRONG_PLACE) { cfirDiagnostic ->
        JavaHasDefaultAnnotationIsInWrongPlaceImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_CANNOT_FIND_METHOD) { cfirDiagnostic ->
        MacroCannotFindMethodImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions155() {
    add(CfirErrors.UNQUALIFIED_LEFT_VALUE_ASSIGNED) { cfirDiagnostic ->
        UnqualifiedLeftValueAssignedImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.EXTEND_A_JAVA_TYPE) { cfirDiagnostic ->
        ExtendAJavaTypeImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.INVALID_USE_OF_ANNOTATION_JFFI) { cfirDiagnostic ->
        InvalidUseOfAnnotationJffiImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.CJ_MAPPING_GENERIC_METHOD_NOT_GET_INSTANCE_CONFIG) { cfirDiagnostic ->
        CjMappingGenericMethodNotGetInstanceConfigImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_DIAG_REPORT_ERROR) { cfirDiagnostic ->
        MacroDiagReportErrorImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_AMBIGUOUS_MATCH) { cfirDiagnostic ->
        MacroAmbiguousMatchImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b.map { fqName ->
                fqName
            },
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions156() {
    add(CfirErrors.IMMUTABLE_PROPERTY_WITH_SETTER) { cfirDiagnostic ->
        ImmutablePropertyWithSetterImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.DEPRECATION_OVERRIDE_WARNING) { cfirDiagnostic ->
        DeprecationOverrideWarningImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions157() {
    add(CfirErrors.INVALID_CALLED_OBJECT) { cfirDiagnostic ->
        InvalidCalledObjectImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions159() {
    add(CfirErrors.VARRAY_ARGS_NUMBER_MISMATCH) { cfirDiagnostic ->
        VarrayArgsNumberMismatchImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.OBJC_METHOD_MUST_HAVE_FOREIGN_NAME) { cfirDiagnostic ->
        ObjcMethodMustHaveForeignNameImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions161() {
    add(CfirErrors.OBJC_FUNC_ARGUMENT_MUST_BE_OBJC_COMPATIBLE) { cfirDiagnostic ->
        ObjcFuncArgumentMustBeObjcCompatibleImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions162() {
    add(CfirErrors.EXCEED_FLOAT_LITERAL_RANGE) { cfirDiagnostic ->
        ExceedFloatLiteralRangeImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.JAVA_MIRROR_SUBTYPE_ANNO_MUST_INHERIT_MIRROR) { cfirDiagnostic ->
        JavaMirrorSubtypeAnnoMustInheritMirrorImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_INVALID_ATTR_TOKENS) { cfirDiagnostic ->
        MacroInvalidAttrTokensImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions163() {
    add(CfirErrors.SPAWN_ARG_NO_EFFECT) { cfirDiagnostic ->
        SpawnArgNoEffectImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.INOUT_MODIFY_NON_CTYPE) { cfirDiagnostic ->
        InoutModifyNonCtypeImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions164() {
    add(CfirErrors.RESUME_NO_WITH) { cfirDiagnostic ->
        ResumeNoWithImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.ACCESSIBILITY_WITH_MAIN_HINT) { cfirDiagnostic ->
        AccessibilityWithMainHintImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic.c,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.FOREIGN_NAME_APPEARED_IN_CHILD) { cfirDiagnostic ->
        ForeignNameAppearedInChildImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions165() {
    add(CfirErrors.STRUCT_SUPER_NOT_ALLOWED) { cfirDiagnostic ->
        StructSuperNotAllowedImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.CONST_EVAL_ARITHMETIC_OVERFLOW) { cfirDiagnostic ->
        ConstEvalArithmeticOverflowImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.TYPE_UNINITIALIZED_STATIC_FIELD) { cfirDiagnostic ->
        TypeUninitializedStaticFieldImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.HIDE_MUST_AT_END) { cfirDiagnostic ->
        HideMustAtEndImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions166() {
    add(CfirErrors.RECURSIVE_CONSTRUCTOR_CALL) { cfirDiagnostic ->
        RecursiveConstructorCallImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.JAVA_INTEROP_NOT_SUPPORTED) { cfirDiagnostic ->
        JavaInteropNotSupportedImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions167() {
    add(CfirErrors.GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS) { cfirDiagnostic ->
        GenericNoMemberMatchInUpperBoundsImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions168() {
    add(CfirErrors.LITERAL_NUMERIC_OVERFLOW) { cfirDiagnostic ->
        LiteralNumericOverflowImpl(
            cfirDiagnostic.a,
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.b),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.GENERIC_TYPE_INCONSISTENT) { cfirDiagnostic ->
        GenericTypeInconsistentImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.ANNOTATION_ARG_TARGET) { cfirDiagnostic ->
        AnnotationArgTargetImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.DEPRECATION_OVERRIDE_ERROR) { cfirDiagnostic ->
        DeprecationOverrideErrorImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions169() {
    add(CfirErrors.MACRO_DEPENDENCY_COMPILE_FAILED) { cfirDiagnostic ->
        MacroDependencyCompileFailedImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic.c,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions170() {
    add(CfirErrors.NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER) { cfirDiagnostic ->
        NameInConstraintIsNotATypeParameterImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions171() {
    add(CfirErrors.EXPORT_SAME_PRIVATE_DECL) { cfirDiagnostic ->
        ExportSamePrivateDeclImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions172() {
    add(CfirErrors.NOTHING_TO_OVERRIDE) { cfirDiagnostic ->
        NothingToOverrideImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.OBJC_INTEROP_CTOR_PARAM_MUST_BE_OBJC_COMPATIBLE) { cfirDiagnostic ->
        ObjcInteropCtorParamMustBeObjcCompatibleImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions173() {
    add(CfirErrors.MISMATCHED_TYPES_MULTIPLE_ASSIGN) { cfirDiagnostic ->
        MismatchedTypesMultipleAssignImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.GENERIC_PARAMETER_OF_JAVA_TYPE) { cfirDiagnostic ->
        GenericParameterOfJavaTypeImpl(
            cfirDiagnostic.a,
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.b),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_EXECUTOR_SERVER_CRASH) { cfirDiagnostic ->
        MacroExecutorServerCrashImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions174() {
    add(CfirErrors.CJMAPPING_STRUCT_GENERIC_NOT_SUPPORTED) { cfirDiagnostic ->
        CjmappingStructGenericNotSupportedImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions175() {
    add(CfirErrors.EXTEND_IMMUTABLE_MUT_PROPERTY) { cfirDiagnostic ->
        ExtendImmutableMutPropertyImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.CANNOT_WEAKEN_ACCESS_PRIVILEGE) { cfirDiagnostic ->
        CannotWeakenAccessPrivilegeImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.USELESS_EXCEPTION_TYPE) { cfirDiagnostic ->
        UselessExceptionTypeImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.COMMON_SPECIFIC_ANNOTATION_NOT_ALLOWED) { cfirDiagnostic ->
        CommonSpecificAnnotationNotAllowedImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions176() {
    add(CfirErrors.WRONG_MODIFIER_CONTAINING_DECLARATION) { cfirDiagnostic ->
        WrongModifierContainingDeclarationImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions177() {
    add(CfirErrors.REPEATED_MODIFIER) { cfirDiagnostic ->
        RepeatedModifierImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.OBJC_CJMAPPING_GENERIC_NOT_SUPPORTED) { cfirDiagnostic ->
        ObjcCjmappingGenericNotSupportedImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions178() {
    add(CfirErrors.TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR) { cfirDiagnostic ->
        TypeInferenceOnlyInputTypesErrorImpl(
            cfirSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(cfirDiagnostic.a),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.COMMON_STATIC_LET_CANT_BE_INITIALIZED_IN_STATIC_INIT) { cfirDiagnostic ->
        CommonStaticLetCantBeInitializedInStaticInitImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.JAVA_MIRROR_CANNOT_INHERIT_PURE_CANGJIE_TYPE) { cfirDiagnostic ->
        JavaMirrorCannotInheritPureCangjieTypeImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.CJMAPPING_STRUCT_INHERITANCE_INTERFACE_NOT_SUPPORTED) { cfirDiagnostic ->
        CjmappingStructInheritanceInterfaceNotSupportedImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions180() {
    add(CfirErrors.ENUM_CONSTRUCTOR_WITH_PARAM_MUST_HAVE_ARGS) { cfirDiagnostic ->
        EnumConstructorWithParamMustHaveArgsImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions181() {
    add(CfirErrors.MOCK_NOT_IN_TEST_MODE) { cfirDiagnostic ->
        MockNotInTestModeImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions182() {
    add(CfirErrors.MULTIPLE_CLASS_SUPER_TYPES) { cfirDiagnostic ->
        MultipleClassSuperTypesImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b.map { name ->
                name
            },
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions183() {
    add(CfirErrors.CONST_EVAL_NEGATIVE_SHIFT_COUNT) { cfirDiagnostic ->
        ConstEvalNegativeShiftCountImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.EXTEND_FUNCTION_CANNOT_OVERRIDDEN) { cfirDiagnostic ->
        ExtendFunctionCannotOverriddenImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.OBJC_FUNC_CALL_PROPERTY_CAN_ONLY_BE_CALLED) { cfirDiagnostic ->
        ObjcFuncCallPropertyCanOnlyBeCalledImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions184() {
    add(CfirErrors.SPECIFIC_HAS_DIFFERENT_MODIFIER) { cfirDiagnostic ->
        SpecificHasDifferentModifierImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.MACRO_CALL_SAVE_FILE_FAILED) { cfirDiagnostic ->
        MacroCallSaveFileFailedImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions185() {
    add(CfirErrors.EXTEND_IMMUTABLE_INDEX_ASSIGNMENT) { cfirDiagnostic ->
        ExtendImmutableIndexAssignmentImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.INVALID_SUBSCRIPT_EXPR) { cfirDiagnostic ->
        InvalidSubscriptExprImpl(
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.a),
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.CJMAPPING_METHOD_ARG_NOT_SUPPORTED) { cfirDiagnostic ->
        CjmappingMethodArgNotSupportedImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.OBJC_INTEROP_TOPLEVEL_PARAM_MUST_BE_OBJC_COMPATIBLE) { cfirDiagnostic ->
        ObjcInteropToplevelParamMustBeObjcCompatibleImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions186() {
    add(CfirErrors.ENUM_TYPE_CANNOT_BE_USED_AS_CONSTRUCTOR) { cfirDiagnostic ->
        EnumTypeCannotBeUsedAsConstructorImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.INVALID_LOOP_CONTROL) { cfirDiagnostic ->
        InvalidLoopControlImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.NON_EXHAUSTIVE_MATCH) { cfirDiagnostic ->
        NonExhaustiveMatchImpl(
            cfirDiagnostic.a.map { string ->
                string
            },
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.IFAVAILABLE_ARG_NO_NAME) { cfirDiagnostic ->
        IfavailableArgNoNameImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions188() {
    add(CfirErrors.EXTEND_GENERIC_USAGE) { cfirDiagnostic ->
        ExtendGenericUsageImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.REDUNDANT_MODIFIER) { cfirDiagnostic ->
        RedundantModifierImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.AMBIGUOUS_CONSTRUCTOR_CALL) { cfirDiagnostic ->
        AmbiguousConstructorCallImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.USED_BEFORE_INITIALIZATION) { cfirDiagnostic ->
        UsedBeforeInitializationImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.NOT_MEMBER_OF) { cfirDiagnostic ->
        NotMemberOfImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic.c,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions189() {
    add(CfirErrors.JAVA_APP_INHERIT_EXT) { cfirDiagnostic ->
        JavaAppInheritExtImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions190() {
    add(CfirErrors.MIXING_NAMED_AND_POSITIONAL_ARGUMENTS) { cfirDiagnostic ->
        MixingNamedAndPositionalArgumentsImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.APILEVEL_MISSING_ARG) { cfirDiagnostic ->
        ApilevelMissingArgImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions191() {
    add(CfirErrors.EXTEND_SPECIALIZATION_CONFLICT) { cfirDiagnostic ->
        ExtendSpecializationConflictImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.CANNOT_HAVE_DEFAULT_PARAM) { cfirDiagnostic ->
        CannotHaveDefaultParamImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.SHADOW_CANNOT_IN_TYPE_ARGS) { cfirDiagnostic ->
        ShadowCannotInTypeArgsImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirSymbolBuilder.typeBuilder.buildType(cfirDiagnostic.c),
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions192() {
    add(CfirErrors.ONLY_ONE_CLASS_BOUND_ALLOWED) { cfirDiagnostic ->
        OnlyOneClassBoundAllowedImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions193() {
    add(CfirErrors.INVALID_OPERATOR_PARAMETER_COUNT) { cfirDiagnostic ->
        InvalidOperatorParameterCountImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic.c,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions194() {
    add(CfirErrors.THROW_EXPR_WITH_WRONG_TYPE) { cfirDiagnostic ->
        ThrowExprWithWrongTypeImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.EXTEND_REF_TARGET_CANNOT_BE_JAVA_IMPL) { cfirDiagnostic ->
        ExtendRefTargetCannotBeJavaImplImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions195() {
    add(CfirErrors.INHERIT_SUPER_MEMBER_KIND_INCONSISTENT) { cfirDiagnostic ->
        InheritSuperMemberKindInconsistentImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.OBJC_INTEROP_METHOD_RET_MUST_BE_OBJC_COMPATIBLE) { cfirDiagnostic ->
        ObjcInteropMethodRetMustBeObjcCompatibleImpl(
            cfirDiagnostic.a,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions196() {
    add(CfirErrors.SPAWN_ARG_INVALID) { cfirDiagnostic ->
        SpawnArgInvalidImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions198() {
    add(CfirErrors.TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE) { cfirDiagnostic ->
        TypeCannotExtendImportedInterfaceImpl(
            cfirDiagnostic.a,
            cfirDiagnostic.b,
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

private fun CaDiagnosticConverterBuilder.addConversions199() {
    add(CfirErrors.LAMBDA_MUST_HAVE_TYPE_ANNOTATION) { cfirDiagnostic ->
        LambdaMustHaveTypeAnnotationImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.VARRAY_IN_CFUNC) { cfirDiagnostic ->
        VarrayInCfuncImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
    add(CfirErrors.HIDE_COMPILE_TIME_INVISIBLE) { cfirDiagnostic ->
        HideCompileTimeInvisibleImpl(
            cfirDiagnostic as CjPsiDiagnostic,
            token,
        )
    }
}

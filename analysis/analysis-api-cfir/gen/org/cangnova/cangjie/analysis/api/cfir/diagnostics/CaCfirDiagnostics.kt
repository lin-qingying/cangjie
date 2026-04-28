

package org.cangnova.cangjie.analysis.api.cfir.diagnostics

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCommandTypePattern
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjHandleClause
import org.cangnova.cangjie.psi.CjImportItem
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjPerformExpression
import org.cangnova.cangjie.psi.CjResumeExpression
import org.cangnova.cangjie.psi.CjTypeReference

/*
 * 本文件由生成器自动生成
 * 请勿手动修改
 */

sealed interface CaCfirDiagnostic<PSI : PsiElement> : CaDiagnosticWithPsi<PSI> {
    interface NoConstructor : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NoConstructor::class
    }

    interface EnumTypeCannotBeUsedAsConstructor : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = EnumTypeCannotBeUsedAsConstructor::class
        val enumName: Name
    }

    interface ConflictingOverloads : CaCfirDiagnostic<CjNamedDeclaration> {
        override val diagnosticClass get() = ConflictingOverloads::class
        val conflictingSymbols: List<String>
    }

    interface Redeclaration : CaCfirDiagnostic<CjNamedDeclaration> {
        override val diagnosticClass get() = Redeclaration::class
        val conflictingSymbols: List<String>
    }

    interface ClassifierRedeclaration : CaCfirDiagnostic<CjNamedDeclaration> {
        override val diagnosticClass get() = ClassifierRedeclaration::class
        val conflictingSymbols: List<String>
    }

    interface UnresolvedImport : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnresolvedImport::class
        val reference: String
    }

    interface ImportConflict : CaCfirDiagnostic<CjImportItem> {
        override val diagnosticClass get() = ImportConflict::class
        val name: Name
    }

    interface ImportAliasConflict : CaCfirDiagnostic<CjImportItem> {
        override val diagnosticClass get() = ImportAliasConflict::class
        val alias: Name
    }

    interface SuperTypesSelfReference : CaCfirDiagnostic<CjTypeReference> {
        override val diagnosticClass get() = SuperTypesSelfReference::class
        val className: Name
    }

    interface SuperTypesDuplicate : CaCfirDiagnostic<CjTypeReference> {
        override val diagnosticClass get() = SuperTypesDuplicate::class
        val typeName: Name
    }

    interface InterfaceCannotInheritClass : CaCfirDiagnostic<CjTypeReference> {
        override val diagnosticClass get() = InterfaceCannotInheritClass::class
        val interfaceName: Name
        val superTypeName: Name
    }

    interface MultipleClassSuperTypes : CaCfirDiagnostic<CjTypeReference> {
        override val diagnosticClass get() = MultipleClassSuperTypes::class
        val className: Name
        val superTypes: List<Name>
    }

    interface IllegalExtendedType : CaCfirDiagnostic<CjTypeReference> {
        override val diagnosticClass get() = IllegalExtendedType::class
        val typeName: Name
    }

    interface ExtendDuplicateInterface : CaCfirDiagnostic<CjTypeReference> {
        override val diagnosticClass get() = ExtendDuplicateInterface::class
        val interfaceName: Name
    }

    interface ExtendNotInterface : CaCfirDiagnostic<CjTypeReference> {
        override val diagnosticClass get() = ExtendNotInterface::class
        val typeName: Name
    }

    interface ExtendOrphanRule : CaCfirDiagnostic<CjTypeReference> {
        override val diagnosticClass get() = ExtendOrphanRule::class
        val targetTypeName: Name
    }

    interface ExtendGenericUsage : CaCfirDiagnostic<CjDeclaration> {
        override val diagnosticClass get() = ExtendGenericUsage::class
        val typeParameterName: Name
    }

    interface ExtendSpecializationConflict : CaCfirDiagnostic<CjTypeReference> {
        override val diagnosticClass get() = ExtendSpecializationConflict::class
        val interfaceName: Name
    }

    interface ExtendDefaultImplementationConflict : CaCfirDiagnostic<CjTypeReference> {
        override val diagnosticClass get() = ExtendDefaultImplementationConflict::class
        val memberName: Name
        val interfaceName: Name
    }

    interface ExtendImmutableMutInterface : CaCfirDiagnostic<CjTypeReference> {
        override val diagnosticClass get() = ExtendImmutableMutInterface::class
        val interfaceName: Name
        val mutMemberName: Name
    }

    interface ExtendImmutableMutProperty : CaCfirDiagnostic<CjDeclaration> {
        override val diagnosticClass get() = ExtendImmutableMutProperty::class
        val propertyName: Name
    }

    interface ExtendImmutableIndexAssignment : CaCfirDiagnostic<CjDeclaration> {
        override val diagnosticClass get() = ExtendImmutableIndexAssignment::class
        val operatorName: Name
    }

    interface ExtendInterfaceNotExtendable : CaCfirDiagnostic<CjTypeReference> {
        override val diagnosticClass get() = ExtendInterfaceNotExtendable::class
        val interfaceName: Name
    }

    interface ExtendCTypeNotAllowed : CaCfirDiagnostic<CjTypeReference> {
        override val diagnosticClass get() = ExtendCTypeNotAllowed::class
        val typeName: Name
    }

    interface ExtendSuperNotAllowed : CaCfirDiagnostic<CjExpression> {
        override val diagnosticClass get() = ExtendSuperNotAllowed::class
    }

    interface StructSuperNotAllowed : CaCfirDiagnostic<CjExpression> {
        override val diagnosticClass get() = StructSuperNotAllowed::class
    }

    interface EnumSuperNotAllowed : CaCfirDiagnostic<CjExpression> {
        override val diagnosticClass get() = EnumSuperNotAllowed::class
    }

    interface InterfaceSuperNotAllowed : CaCfirDiagnostic<CjExpression> {
        override val diagnosticClass get() = InterfaceSuperNotAllowed::class
    }

    interface StaticCannotBeOpenAbstractOverride : CaCfirDiagnostic<CjDeclaration> {
        override val diagnosticClass get() = StaticCannotBeOpenAbstractOverride::class
        val declarationName: Name?
    }

    interface MutOnlyOnFunction : CaCfirDiagnostic<CjDeclaration> {
        override val diagnosticClass get() = MutOnlyOnFunction::class
        val declarationName: Name?
    }

    interface NothingToOverride : CaCfirDiagnostic<CjNamedDeclaration> {
        override val diagnosticClass get() = NothingToOverride::class
    }

    interface OverrideStaticError : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OverrideStaticError::class
        val declarationKind: String
    }

    interface RedefInstanceError : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RedefInstanceError::class
        val declarationKind: String
    }

    interface InvalidOperatorParameterCount : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InvalidOperatorParameterCount::class
        val operator: String
        val expectedCount: String
        val actualCount: String
    }

    interface RepeatedModifier : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RepeatedModifier::class
        val modifier: CjKeywordToken
    }

    interface RedundantModifier : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RedundantModifier::class
        val modifier: CjKeywordToken
        val redundantBecauseOf: CjKeywordToken
    }

    interface IncompatibleModifiers : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IncompatibleModifiers::class
        val modifier1: CjKeywordToken
        val modifier2: CjKeywordToken
    }

    interface WrongModifierTarget : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = WrongModifierTarget::class
        val modifier: CjKeywordToken
        val target: String
    }

    interface WrongModifierContainingDeclaration : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = WrongModifierContainingDeclaration::class
        val modifier: CjKeywordToken
        val container: String
    }

    interface RedundantModifierForTarget : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RedundantModifierForTarget::class
        val modifier: CjKeywordToken
        val target: String
    }

    interface DeprecatedModifierForTarget : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedModifierForTarget::class
        val modifier: CjKeywordToken
        val target: String
    }

    interface DeprecatedModifierContainingDeclaration : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedModifierContainingDeclaration::class
        val modifier: CjKeywordToken
        val container: String
    }

    interface DeprecatedModifierPair : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedModifierPair::class
        val modifier: CjKeywordToken
        val conflictingModifier: CjKeywordToken
    }

    interface CannotWeakenAccessPrivilege : CaCfirDiagnostic<CjNamedDeclaration> {
        override val diagnosticClass get() = CannotWeakenAccessPrivilege::class
        val baseMemberName: Name
        val baseVisibility: Visibility
    }

    interface ParamNamedMismatched : CaCfirDiagnostic<CjNamedDeclaration> {
        override val diagnosticClass get() = ParamNamedMismatched::class
        val baseMemberName: Name
    }

    interface NoValueForParameter : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NoValueForParameter::class
        val parameterName: Name
    }

    interface TooManyArguments : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TooManyArguments::class
        val targetName: Name
    }

    interface NamedParameterNotFound : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NamedParameterNotFound::class
        val parameterName: Name
    }

    interface ArgumentPassedTwice : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ArgumentPassedTwice::class
    }

    interface NamedArgumentsNotAllowed : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NamedArgumentsNotAllowed::class
        val targetDescription: String
    }

    interface MixingNamedAndPositionalArguments : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MixingNamedAndPositionalArguments::class
    }

    interface NeedNamedArgument : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NeedNamedArgument::class
        val parameterName: Name
    }

    interface AmbiguousConstructorCall : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AmbiguousConstructorCall::class
        val className: Name
    }

    interface AmbiguousFunctionCall : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AmbiguousFunctionCall::class
        val functionName: Name
    }

    interface RecursiveConstructorCall : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = RecursiveConstructorCall::class
    }

    interface IllegalThisOrSuperCall : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IllegalThisOrSuperCall::class
        val calleeName: String
    }

    interface ExplicitSuperCallRequired : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExplicitSuperCallRequired::class
    }

    interface InvalidLoopControl : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InvalidLoopControl::class
    }

    interface UsedBeforeInitialization : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UsedBeforeInitialization::class
        val variableName: Name
    }

    interface ClassUninitializedField : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ClassUninitializedField::class
        val fieldName: Name
    }

    interface GenericNoMemberMatchInUpperBounds : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = GenericNoMemberMatchInUpperBounds::class
        val memberName: Name
        val typeParameterName: Name
    }

    interface GenericNoMethodMatchInUpperBounds : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = GenericNoMethodMatchInUpperBounds::class
        val methodName: Name
        val typeParameterName: Name
    }

    interface CannotModifyVar : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CannotModifyVar::class
        val variableName: Name
    }

    interface ImmutableFunctionCannotAccessMutableFunction : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ImmutableFunctionCannotAccessMutableFunction::class
        val currentFunctionName: Name
        val targetFunctionName: Name
    }

    interface AnnotationNoConstInit : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AnnotationNoConstInit::class
    }

    interface InvalidCfuncReturnType : CaCfirDiagnostic<CjTypeReference> {
        override val diagnosticClass get() = InvalidCfuncReturnType::class
        val actualType: CaType
    }

    interface EffectsFeatureDisabled : CaCfirDiagnostic<CjElement> {
        override val diagnosticClass get() = EffectsFeatureDisabled::class
        val constructName: String
    }

    interface CommandIncompatibleType : CaCfirDiagnostic<CjPerformExpression> {
        override val diagnosticClass get() = CommandIncompatibleType::class
        val actualType: CaType
    }

    interface CommandHandleTypeError : CaCfirDiagnostic<CjCommandTypePattern> {
        override val diagnosticClass get() = CommandHandleTypeError::class
        val actualType: CaType
    }

    interface ImplicitResumeOutsideHandler : CaCfirDiagnostic<CjResumeExpression> {
        override val diagnosticClass get() = ImplicitResumeOutsideHandler::class
    }

    interface ResumeNoWith : CaCfirDiagnostic<CjResumeExpression> {
        override val diagnosticClass get() = ResumeNoWith::class
        val resumptionType: CaType
    }

    interface ResumeThrowingMismatchType : CaCfirDiagnostic<CjResumeExpression> {
        override val diagnosticClass get() = ResumeThrowingMismatchType::class
        val actualType: CaType
    }

    interface MismatchingHandleBlock : CaCfirDiagnostic<CjHandleClause> {
        override val diagnosticClass get() = MismatchingHandleBlock::class
        val actualType: CaType
        val expectedType: CaType
    }

    interface NonExhaustiveMatch : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NonExhaustiveMatch::class
        val missingCases: List<String>
    }

    interface TuplePatternNotMatch : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TuplePatternNotMatch::class
        val actualTypeText: String
    }

    interface PatternNotMatch : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = PatternNotMatch::class
        val patternText: String
    }

    interface EnumPatternParamSizeError : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = EnumPatternParamSizeError::class
    }

    interface NotOverloadInMatch : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NotOverloadInMatch::class
    }

    interface MatchCaseHasNoType : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MatchCaseHasNoType::class
    }

    interface NameInConstraintIsNotATypeParameter : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NameInConstraintIsNotATypeParameter::class
        val name: Name
    }

    interface OnlyOneClassBoundAllowed : CaCfirDiagnostic<CjElement> {
        override val diagnosticClass get() = OnlyOneClassBoundAllowed::class
    }

    interface RepeatedBound : CaCfirDiagnostic<CjElement> {
        override val diagnosticClass get() = RepeatedBound::class
    }

    interface ConflictingUpperBounds : CaCfirDiagnostic<CjNamedDeclaration> {
        override val diagnosticClass get() = ConflictingUpperBounds::class
    }

    interface CannotInferParameterType : CaCfirDiagnostic<CjElement> {
        override val diagnosticClass get() = CannotInferParameterType::class
        val parameter: CaTypeParameterSymbol
    }

    interface NewInferenceError : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NewInferenceError::class
        val message: String
    }

    interface TypeInferenceOnlyInputTypesError : CaCfirDiagnostic<CjElement> {
        override val diagnosticClass get() = TypeInferenceOnlyInputTypesError::class
        val parameter: CaTypeParameterSymbol
    }

    interface BuilderInferenceMultiLambdaRestriction : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = BuilderInferenceMultiLambdaRestriction::class
        val typeParameterName: Name
        val declarationName: Name
    }

    interface InferredTypeVariableIntoEmptyIntersection : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InferredTypeVariableIntoEmptyIntersection::class
        val typeVariable: String
        val incompatibleTypes: List<CaType>
        val kindDescription: String
        val causingTypesText: String
    }

    interface InferredTypeVariableIntoPossibleEmptyIntersection : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InferredTypeVariableIntoPossibleEmptyIntersection::class
        val typeVariable: String
        val incompatibleTypes: List<CaType>
        val kindDescription: String
        val causingTypesText: String
    }

    interface TypeMismatch : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeMismatch::class
        val expectedType: CaType
        val actualType: CaType
        val isMismatchDueToNullability: Boolean
    }

    interface PatternInitializerTypeMismatch : CaCfirDiagnostic<CjNamedDeclaration> {
        override val diagnosticClass get() = PatternInitializerTypeMismatch::class
        val expectedType: CaType
        val actualType: CaType
        val isMismatchDueToNullability: Boolean
    }

    interface ReturnTypeMismatch : CaCfirDiagnostic<CjExpression> {
        override val diagnosticClass get() = ReturnTypeMismatch::class
        val expectedType: CaType
        val actualType: CaType
        val isMismatchDueToNullability: Boolean
    }

    interface ArgumentTypeMismatch : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ArgumentTypeMismatch::class
        val expectedType: CaType
        val actualType: CaType
        val isMismatchDueToNullability: Boolean
    }

    interface AssignmentTypeMismatch : CaCfirDiagnostic<CjExpression> {
        override val diagnosticClass get() = AssignmentTypeMismatch::class
        val expectedType: CaType
        val actualType: CaType
        val isMismatchDueToNullability: Boolean
    }

    interface VarraySizeMismatch : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = VarraySizeMismatch::class
        val expectedSize: Long
        val actualSize: Long
        val elementType: CaType
    }

    interface GenericTypeShouldBeUsedWithTypeArgument : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = GenericTypeShouldBeUsedWithTypeArgument::class
        val typeName: Name
    }

    interface InvisibleMember : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InvisibleMember::class
        val member: String
        val visibility: String
    }

    interface InvisibleReference : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InvisibleReference::class
        val reference: String
        val visibility: String
    }

    interface OverridingReturnTypeMismatch : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OverridingReturnTypeMismatch::class
        val actualType: CaType
        val expectedType: CaType
        val overriddenName: Name
    }

    interface CannotOverrideInvisibleMember : CaCfirDiagnostic<CjNamedDeclaration> {
        override val diagnosticClass get() = CannotOverrideInvisibleMember::class
        val memberName: Name
    }

    interface ClassNotOpenForInheritance : CaCfirDiagnostic<CjTypeReference> {
        override val diagnosticClass get() = ClassNotOpenForInheritance::class
        val className: Name
    }

    interface AbstractMemberNotImplemented : CaCfirDiagnostic<CjNamedDeclaration> {
        override val diagnosticClass get() = AbstractMemberNotImplemented::class
        val className: Name
    }

    interface LiteralNumericOverflow : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = LiteralNumericOverflow::class
        val literalText: String
        val targetType: CaType
    }

    interface ConstEvalDivideByZero : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ConstEvalDivideByZero::class
        val operatorName: String
    }

    interface ConstEvalArithmeticOverflow : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ConstEvalArithmeticOverflow::class
        val operatorName: String
    }

    interface ConstEvalNegativeShiftCount : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ConstEvalNegativeShiftCount::class
    }

    interface ConstEvalShiftCountOverflow : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ConstEvalShiftCountOverflow::class
    }

    interface UnresolvedReference : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnresolvedReference::class
        val reference: String
        val operator: String?
    }

    interface InvalidBinaryOperator : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InvalidBinaryOperator::class
        val operator: String
        val leftType: String
        val rightType: String
    }

    interface NoMatchingOperatorInvoke : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NoMatchingOperatorInvoke::class
        val name: String
        val type: CaType
    }

    interface InvalidNodeAfterCheck : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InvalidNodeAfterCheck::class
    }

    interface UnableToInferDecl : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnableToInferDecl::class
    }

    interface MismatchedTypesMultipleAssign : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MismatchedTypesMultipleAssign::class
        val actualType: CaType
    }

    interface MismatchedTypesBecause : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MismatchedTypesBecause::class
        val expectedType: CaType
        val actualType: CaType
        val reason: String
    }

    interface AmbiguousUse : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AmbiguousUse::class
        val name: Name
    }

    interface ConflictWithSubPackage : CaCfirDiagnostic<CjNamedDeclaration> {
        override val diagnosticClass get() = ConflictWithSubPackage::class
        val declarationName: Name
        val subPackageName: Name
    }

    interface CoreObjectNotFoundWhenNoPrelude : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CoreObjectNotFoundWhenNoPrelude::class
    }

    interface AccessibilityWithMainHint : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AccessibilityWithMainHint::class
        val declarationKind: String
        val memberName: Name
        val visibility: Visibility
    }

    interface AccessibilityError : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AccessibilityError::class
        val declarationKind: String
        val visibility: Visibility
    }

    interface ParamCountMismatch : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ParamCountMismatch::class
        val expected: Int
        val actual: Int
    }

    interface UnableToInferReturnType : CaCfirDiagnostic<CjDeclaration> {
        override val diagnosticClass get() = UnableToInferReturnType::class
    }

    interface UnableToInferGenericFunc : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnableToInferGenericFunc::class
    }

    interface InvalidCalledObject : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InvalidCalledObject::class
    }

    interface InvalidReturn : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InvalidReturn::class
    }

    interface InvalidReturnInStaticInit : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InvalidReturnInStaticInit::class
    }

    interface InvalidSubscriptAssignParameter : CaCfirDiagnostic<CjDeclaration> {
        override val diagnosticClass get() = InvalidSubscriptAssignParameter::class
    }

    interface InvalidSubscriptAssignParameterNum : CaCfirDiagnostic<CjDeclaration> {
        override val diagnosticClass get() = InvalidSubscriptAssignParameterNum::class
    }

    interface InvalidSubscriptAssignReturn : CaCfirDiagnostic<CjDeclaration> {
        override val diagnosticClass get() = InvalidSubscriptAssignReturn::class
    }

    interface StaticFunctionOverloadConflicts : CaCfirDiagnostic<CjNamedDeclaration> {
        override val diagnosticClass get() = StaticFunctionOverloadConflicts::class
        val functionName: Name
    }

    interface UseMutableFuncAlone : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UseMutableFuncAlone::class
        val functionName: Name
    }

    interface UnsafeFuncCanOnlyBeCalled : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnsafeFuncCanOnlyBeCalled::class
    }

    interface AmbiguousMatchPrimitiveExtend : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AmbiguousMatchPrimitiveExtend::class
        val functionName: Name
        val extendedTypes: List<Name>
    }

    interface CannotHaveDefaultParam : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CannotHaveDefaultParam::class
        val functionKind: String
    }

    interface TrailingLambdaCannotUsedForNonFunction : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TrailingLambdaCannotUsedForNonFunction::class
        val paramType: CaType
    }

    interface LambdaMustHaveTypeAnnotation : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = LambdaMustHaveTypeAnnotation::class
    }

    interface UseFuncCaptureVarAlone : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UseFuncCaptureVarAlone::class
        val description: String
    }

    interface UnableToInferExpr : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnableToInferExpr::class
    }

    interface ExceedFloatLiteralRange : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExceedFloatLiteralRange::class
        val literalText: String
    }

    interface FloatLiteralTooLarge : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = FloatLiteralTooLarge::class
        val type: CaType
        val maximum: String
    }

    interface FloatLiteralTooSmall : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = FloatLiteralTooSmall::class
        val type: CaType
        val minimum: String
    }

    interface InvalidUnaryExpr : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InvalidUnaryExpr::class
        val operator: String
        val type: CaType
    }

    interface InvalidUnaryExprWithTarget : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InvalidUnaryExprWithTarget::class
        val operator: String
        val type: CaType
        val returnType: CaType
    }

    interface InvalidSubscriptExpr : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InvalidSubscriptExpr::class
        val receiverType: CaType
        val indexDescription: String
    }

    interface CannotAssignToSubscript : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CannotAssignToSubscript::class
    }

    interface NotMemberOf : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NotMemberOf::class
        val memberName: Name
        val kind: String
        val typeName: Name
    }

    interface MemberNotImported : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MemberNotImported::class
        val memberName: Name
    }

    interface CannotAssignToImmutable : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CannotAssignToImmutable::class
    }

    interface UnqualifiedLeftValueAssigned : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnqualifiedLeftValueAssigned::class
        val name: Name
    }

    interface DifferentOrPattern : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DifferentOrPattern::class
        val description: String
    }

    interface VarInOrPattern : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = VarInOrPattern::class
    }

    interface VarInOrCondition : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = VarInOrCondition::class
    }

    interface UnreachablePattern : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnreachablePattern::class
    }

    interface EnumConstructorWithParamMustHaveArgs : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = EnumConstructorWithParamMustHaveArgs::class
        val constructorName: Name
    }

    interface OptionalChainNonOptional : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OptionalChainNonOptional::class
        val type: CaType
    }

    interface CaptureBeforeInitialization : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CaptureBeforeInitialization::class
        val variableName: Name
    }

    interface InterpolationInConstPattern : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InterpolationInConstPattern::class
    }

    interface CannotRefToPkgName : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CannotRefToPkgName::class
    }

    interface UseExprWithoutImport : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UseExprWithoutImport::class
        val importPath: FqName
        val exprKind: String
    }

    interface GenericTypeInconsistent : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = GenericTypeInconsistent::class
        val typeParameterName: Name
    }

    interface GenericArgumentNoMatch : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = GenericArgumentNoMatch::class
    }

    interface GenericConstraintNotLooser : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = GenericConstraintNotLooser::class
    }

    interface GenericInstantiationCausesAmbiguousFunctions : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = GenericInstantiationCausesAmbiguousFunctions::class
        val instantiation: Name
        val functionName: Name
    }

    interface GenericParamExistInClassIrrelevantUpperboundRecursively : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = GenericParamExistInClassIrrelevantUpperboundRecursively::class
        val typeParameterName: Name
        val upperBound: CaType
    }

    interface GenericParamDirectlyRecursive : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = GenericParamDirectlyRecursive::class
        val typeParameterName: Name
        val boundName: Name
    }

    interface UpperBoundMustBeClassOrInterface : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UpperBoundMustBeClassOrInterface::class
        val upperBound: CaType
        val typeParameterName: Name
    }

    interface GenericStaticAccess : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = GenericStaticAccess::class
    }

    interface PrimitiveTypeAsGenericsArg : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = PrimitiveTypeAsGenericsArg::class
    }

    interface MeetConstraintIndirectly : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MeetConstraintIndirectly::class
    }

    interface GenericUpperBoundsMustBeJavaInJava : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = GenericUpperBoundsMustBeJavaInJava::class
    }

    interface InheritMemberKindInconsistent : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InheritMemberKindInconsistent::class
        val memberKind: String
        val memberName: Name
        val superMemberKind: String
        val containerName: Name
    }

    interface InheritSuperMemberKindInconsistent : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InheritSuperMemberKindInconsistent::class
        val memberName: Name
    }

    interface InheritMemberTypeInconsistent : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InheritMemberTypeInconsistent::class
        val aspect: String
        val memberKind: String
        val memberName: Name
    }

    interface InheritAbstractClassStaticUnimplementFunc : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InheritAbstractClassStaticUnimplementFunc::class
        val className: Name
        val memberKind: String
        val memberName: Name
    }

    interface InvalidMemberVisibilityInClass : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InvalidMemberVisibilityInClass::class
        val modifier: String
        val memberKind: String
    }

    interface CannotInheritSealed : CaCfirDiagnostic<CjTypeReference> {
        override val diagnosticClass get() = CannotInheritSealed::class
        val verb: String
        val kind: String
        val sealedKind: String
        val sealedName: Name
    }

    interface InheritThreadContextInvalid : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InheritThreadContextInvalid::class
        val declarationName: Name
    }

    interface InheritThreadContextNotOpen : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InheritThreadContextNotOpen::class
        val declarationName: Name
    }

    interface InheritNotReturnThis : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InheritNotReturnThis::class
    }

    interface SpawnArgInvalid : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SpawnArgInvalid::class
    }

    interface SpawnArgNoEffect : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SpawnArgNoEffect::class
    }

    interface InterfaceCallWithUnimplementedCall : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InterfaceCallWithUnimplementedCall::class
        val memberKind: String
        val memberName: Name
    }

    interface TypeUninitializedStaticField : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeUninitializedStaticField::class
        val fieldName: Name
    }

    interface InstanceFuncCannotBeUsedInFinalizer : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InstanceFuncCannotBeUsedInFinalizer::class
        val memberKind: String
    }

    interface NonAbstractClassCannotBeSealed : CaCfirDiagnostic<CjNamedDeclaration> {
        override val diagnosticClass get() = NonAbstractClassCannotBeSealed::class
    }

    interface StaticVariableUseGenericParameter : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = StaticVariableUseGenericParameter::class
        val typeParameterName: Name
    }

    interface CstructCannotImplInterfaces : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CstructCannotImplInterfaces::class
    }

    interface ExportSamePrivateDecl : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExportSamePrivateDecl::class
    }

    interface ExtendFunctionCannotOverridden : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExtendFunctionCannotOverridden::class
        val memberKind: String
        val memberName: Name
    }

    interface ExtendMemberCannotShadow : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExtendMemberCannotShadow::class
        val memberName: Name
        val targetTypeName: Name
    }

    interface ExtendIllegalMember : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExtendIllegalMember::class
    }

    interface ExtendCheckSequenceCannotDecide : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExtendCheckSequenceCannotDecide::class
    }

    interface ExportExtendDependNonExportExtend : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExportExtendDependNonExportExtend::class
        val functionNames: List<Name>
    }

    interface ExtendAJavaType : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExtendAJavaType::class
    }

    interface ExtendRefTargetCannotBeJavaImpl : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExtendRefTargetCannotBeJavaImpl::class
    }

    interface TypeCannotExtendImportedInterface : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = TypeCannotExtendImportedInterface::class
        val kind: String
        val typeName: Name
    }

    interface PropertyMustHaveAccessors : CaCfirDiagnostic<CjDeclaration> {
        override val diagnosticClass get() = PropertyMustHaveAccessors::class
    }

    interface ImmutablePropertyWithSetter : CaCfirDiagnostic<CjDeclaration> {
        override val diagnosticClass get() = ImmutablePropertyWithSetter::class
    }

    interface PropertyHaveSameDeclarationInInheritMut : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = PropertyHaveSameDeclarationInInheritMut::class
        val propertyName: Name
    }

    interface PropertyHaveSameDeclarationInInheritImmut : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = PropertyHaveSameDeclarationInInheritImmut::class
        val propertyName: Name
    }

    interface PropertyMustImplementBoth : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = PropertyMustImplementBoth::class
        val propertyName: Name
    }

    interface ExpectConst : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExpectConst::class
        val kind: String
    }

    interface CannotDefineVarInConstFunction : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CannotDefineVarInConstFunction::class
    }

    interface NoConstInit : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NoConstInit::class
    }

    interface ClassConstInitWithVar : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ClassConstInitWithVar::class
    }

    interface AnnotationArgTarget : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AnnotationArgTarget::class
    }

    interface AnnotationArgTargetArrayLit : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AnnotationArgTargetArrayLit::class
    }

    interface AnnotationNonPublic : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AnnotationNonPublic::class
    }

    interface AnnotationCustomPlace : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AnnotationCustomPlace::class
    }

    interface AnnotationErrorArgNum : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AnnotationErrorArgNum::class
        val annotationName: String
        val expectedArgs: String
    }

    interface AnnotationErrorArgRange : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AnnotationErrorArgRange::class
        val annotationName: String
        val supportedArgs: String
    }

    interface AnnotationErrorObject : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AnnotationErrorObject::class
        val annotationName: String
        val validTargets: String
    }

    interface CannotUseAnnotationJffi : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CannotUseAnnotationJffi::class
    }

    interface AnnotationNotApplicableJffi : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = AnnotationNotApplicableJffi::class
        val annotationName: String
        val target: String
    }

    interface InoutModifyCstringOrZerosized : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InoutModifyCstringOrZerosized::class
        val type: CaType
    }

    interface InoutModifyNonCtype : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InoutModifyNonCtype::class
    }

    interface InoutMustBeVarVariable : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InoutMustBeVarVariable::class
    }

    interface InoutModifyHeapVariable : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InoutModifyHeapVariable::class
    }

    interface InoutCanOnlyUsedInCfuncCalling : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InoutCanOnlyUsedInCfuncCalling::class
    }

    interface InoutMismatch : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InoutMismatch::class
        val type: CaType
    }

    interface InvalidInoutArgument : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InvalidInoutArgument::class
    }

    interface DuplicateInoutArgument : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DuplicateInoutArgument::class
    }

    interface VarrayArgsNumberMismatch : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = VarrayArgsNumberMismatch::class
    }

    interface VarraySubscriptNum : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = VarraySubscriptNum::class
    }

    interface VarrayInCfunc : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = VarrayInCfunc::class
    }

    interface VarrayArgTypeWithReftype : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = VarrayArgTypeWithReftype::class
        val type: CaType
    }

    interface ResumptionHandleTypeError : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ResumptionHandleTypeError::class
    }

    interface ResumptionIncorrectReturnType : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ResumptionIncorrectReturnType::class
        val resumptionType: CaType
        val tryBlockType: CaType
    }

    interface CommandResumptionMismatch : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CommandResumptionMismatch::class
        val resumptionParamType: CaType
        val commandResultType: CaType
    }

    interface ResumeWrongResumptionType : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ResumeWrongResumptionType::class
        val actualType: CaType
    }

    interface ReturnInTryHandleBlock : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ReturnInTryHandleBlock::class
    }

    interface UselessCommandType : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UselessCommandType::class
    }

    interface DeprecatedError : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedError::class
        val kind: String
        val name: Name
        val message: String
        val replacement: String
    }

    interface DeprecatedWarning : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecatedWarning::class
        val kind: String
        val name: Name
        val message: String
        val replacement: String
    }

    interface DeprecationWeakening : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecationWeakening::class
    }

    interface DeprecationOverrideError : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecationOverrideError::class
        val kind: String
        val name: Name
    }

    interface DeprecationOverrideWarning : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecationOverrideWarning::class
        val kind: String
        val name: Name
    }

    interface DeprecationRedefError : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecationRedefError::class
        val kind: String
        val name: Name
    }

    interface DeprecationRedefWarning : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DeprecationRedefWarning::class
        val kind: String
        val name: Name
    }

    interface CommonOpenClassNoInit : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CommonOpenClassNoInit::class
        val className: Name
    }

    interface MultipleCommonImplementations : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MultipleCommonImplementations::class
        val kind: String
    }

    interface CommonDirectExtensionHasDuplicatePrivateMembers : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CommonDirectExtensionHasDuplicatePrivateMembers::class
        val extendName: Name
        val memberKind: String
        val memberName: Name
    }

    interface CommonDirectExtensionHasCommonPrivateMembers : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CommonDirectExtensionHasCommonPrivateMembers::class
        val memberKind: String
        val memberName: Name
    }

    interface NotMatched : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = NotMatched::class
        val declarationName: Name
        val kind: String
        val matchKind: String
    }

    interface SpecificVarNotMatchLet : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SpecificVarNotMatchLet::class
        val specificName: Name
        val commonName: Name
    }

    interface SpecificInitCommonPrimaryConstructor : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SpecificInitCommonPrimaryConstructor::class
    }

    interface SpecificHasDifferentKind : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SpecificHasDifferentKind::class
        val specificKind: String
        val commonKind: String
    }

    interface SpecificPrimaryUnmatchedVarDecl : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SpecificPrimaryUnmatchedVarDecl::class
    }

    interface CommonNonExhaustivePlatformExhaustiveMismatch : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CommonNonExhaustivePlatformExhaustiveMismatch::class
        val commonKind: String
        val specificKind: String
    }

    interface SpecificHasDifferentType : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SpecificHasDifferentType::class
        val kind: String
    }

    interface SpecificMemberMustHaveImplementation : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SpecificMemberMustHaveImplementation::class
        val memberKind: String
        val containerKind: String
    }

    interface SpecificHasDifferentModifier : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SpecificHasDifferentModifier::class
        val kind: String
    }

    interface SpecificHasDifferentAnnotation : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SpecificHasDifferentAnnotation::class
        val kind: String
    }

    interface SpecificHasDeprecatedAnnotation : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SpecificHasDeprecatedAnnotation::class
        val annotationName: Name
        val kind: String
        val name: Name
    }

    interface CjmpParameterDefaultValueBothSides : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CjmpParameterDefaultValueBothSides::class
    }

    interface SpecificHasDifferentParameter : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SpecificHasDifferentParameter::class
    }

    interface SpecificHasDifferentSuperType : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SpecificHasDifferentSuperType::class
        val kind: String
    }

    interface SpecificHasDuplicateExtensions : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = SpecificHasDuplicateExtensions::class
        val extendName: Name
    }

    interface CommonPackageHasMain : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CommonPackageHasMain::class
    }

    interface CommonStaticLetCantBeInitializedInStaticInit : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CommonStaticLetCantBeInitializedInStaticInit::class
        val variableName: Name
    }

    interface CommonAssignToCommonImmutableInCtor : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CommonAssignToCommonImmutableInCtor::class
        val variableName: Name
    }

    interface CjmpAbstractClassMemberHasNoExplicitModifier : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CjmpAbstractClassMemberHasNoExplicitModifier::class
        val className: Name
        val memberKind: String
        val modifier: String
    }

    interface ExplicitlyAbstractCanNotHaveBody : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExplicitlyAbstractCanNotHaveBody::class
        val memberKind: String
    }

    interface ExplicitlyAbstractOnlyForCjmpAbstractClass : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ExplicitlyAbstractOnlyForCjmpAbstractClass::class
        val memberKind: String
    }

    interface OpenAbstractSpecificCanNotReplaceOpenCommon : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OpenAbstractSpecificCanNotReplaceOpenCommon::class
        val commonKind: String
        val specificKind: String
    }

    interface CjmpNonSpecificAbstractMemberInSpecificClass : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CjmpNonSpecificAbstractMemberInSpecificClass::class
        val className: Name
        val memberKind: String
    }

    interface CommonGenericFrozenNotSupported : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CommonGenericFrozenNotSupported::class
        val kind: String
    }

    interface CommonGenericRenameNotSupported : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CommonGenericRenameNotSupported::class
    }

    interface CommonSpecificAnnotationNotAllowed : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CommonSpecificAnnotationNotAllowed::class
        val annotationName: Name
    }

    interface JavaIncorrectUseBetweenTypes : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaIncorrectUseBetweenTypes::class
    }

    interface JavaNonJtype : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaNonJtype::class
        val typePosition: String
        val memberKind: String
        val memberName: Name
    }

    interface JavaInvalidUnit : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaInvalidUnit::class
        val typePosition: String
        val memberKind: String
        val memberName: Name
    }

    interface JavaAppInheritExt : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaAppInheritExt::class
        val verb: String
    }

    interface JavaUnsupportedDecl : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaUnsupportedDecl::class
        val declKind: String
        val memberKind: String
        val memberName: Name
    }

    interface MissingJavaInteropAnnotation : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MissingJavaInteropAnnotation::class
        val kind: String
        val name: Name
    }

    interface ShadowCannotInTypeArgs : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ShadowCannotInTypeArgs::class
        val name: Name
        val fieldName: Name
        val superType: CaType
    }

    interface UnsupportedTypeArgumentInJavaInterop : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = UnsupportedTypeArgumentInJavaInterop::class
    }

    interface StaticMemberInInterfaceMustHasBody : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = StaticMemberInInterfaceMustHasBody::class
    }

    interface DefineJavaAnnotation : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = DefineJavaAnnotation::class
    }

    interface InvalidUseOfJavaAnnotation : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InvalidUseOfJavaAnnotation::class
    }

    interface InvalidUseOfAnnotationJffi : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = InvalidUseOfAnnotationJffi::class
    }

    interface VariableOfJavaType : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = VariableOfJavaType::class
        val kind: String
        val type: CaType
    }

    interface GenericParameterOfJavaType : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = GenericParameterOfJavaType::class
        val genericName: Name
        val type: CaType
    }

    interface JavaInteropNotSupported : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaInteropNotSupported::class
        val featureName: String
    }

    interface JavaMirrorCtorArgMustBeJavaMirror : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaMirrorCtorArgMustBeJavaMirror::class
    }

    interface JavaMirrorMethodArgMustBeJavaMirror : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaMirrorMethodArgMustBeJavaMirror::class
    }

    interface JavaMirrorMethodRetUnsupported : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaMirrorMethodRetUnsupported::class
        val returnType: CaType
        val classKind: String
    }

    interface JavaMirrorPropMustBeJavaMirror : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaMirrorPropMustBeJavaMirror::class
    }

    interface JavaMirrorSubtypeMustBeAnnotated : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaMirrorSubtypeMustBeAnnotated::class
        val superName: Name
    }

    interface JavaMirrorCannotInheritPureCangjieType : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaMirrorCannotInheritPureCangjieType::class
    }

    interface JavaImplCannotInheritPureCangjieType : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaImplCannotInheritPureCangjieType::class
    }

    interface JavaMirrorSubtypeAnnoMustInheritMirror : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaMirrorSubtypeAnnoMustInheritMirror::class
    }

    interface JavaMirrorCannotBeExtendedWithInterface : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaMirrorCannotBeExtendedWithInterface::class
    }

    interface JavaImplCannotBeExtendedWithInterface : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaImplCannotBeExtendedWithInterface::class
    }

    interface JavaImplRedefinition : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaImplRedefinition::class
        val declarationName: Name
    }

    interface JavaMirrorInteroplibMustBeImported : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaMirrorInteroplibMustBeImported::class
    }

    interface JavaHasDefaultAnnotationArgs : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaHasDefaultAnnotationArgs::class
    }

    interface JavaHasDefaultAnnotationIsInWrongPlace : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaHasDefaultAnnotationIsInWrongPlace::class
    }

    interface JavaHasDefaultConflictWithStatic : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = JavaHasDefaultConflictWithStatic::class
    }

    interface CjmappingStructGenericNotSupported : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CjmappingStructGenericNotSupported::class
        val genericDescription: String
    }

    interface CjmappingStructInheritanceInterfaceNotSupported : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CjmappingStructInheritanceInterfaceNotSupported::class
    }

    interface CjmappingDeclNotSupported : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CjmappingDeclNotSupported::class
        val kind: String
    }

    interface CjmappingMethodArgNotSupported : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CjmappingMethodArgNotSupported::class
    }

    interface CjmappingMethodRetUnsupported : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CjmappingMethodRetUnsupported::class
        val returnType: CaType
        val containerKind: String
    }

    interface CjMappingGenericMethodNotGetInstanceConfig : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = CjMappingGenericMethodNotGetInstanceConfig::class
        val configName: String
    }

    interface ObjcInteropCtorParamMustBeObjcCompatible : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ObjcInteropCtorParamMustBeObjcCompatible::class
        val declarationKind: String
    }

    interface ObjcInteropMethodParamMustBeObjcCompatible : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ObjcInteropMethodParamMustBeObjcCompatible::class
        val declarationKind: String
    }

    interface ObjcInteropMethodRetMustBeObjcCompatible : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ObjcInteropMethodRetMustBeObjcCompatible::class
        val declarationKind: String
    }

    interface ObjcInteropPropMustBeObjcCompatible : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ObjcInteropPropMustBeObjcCompatible::class
        val declarationKind: String
    }

    interface ObjcInteropFieldMustBeObjcCompatible : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ObjcInteropFieldMustBeObjcCompatible::class
        val declarationKind: String
    }

    interface ObjcMirrorDeclCannotInherit : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ObjcMirrorDeclCannotInherit::class
    }

    interface ObjcMirrorSubtypeCannotMultipleInherit : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ObjcMirrorSubtypeCannotMultipleInherit::class
    }

    interface ObjcMirrorSubtypeMustBeAnnotated : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ObjcMirrorSubtypeMustBeAnnotated::class
    }

    interface ObjcMirrorSubtypeMustInheritMirror : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ObjcMirrorSubtypeMustInheritMirror::class
    }

    interface ObjcMirrorMustInheritMirror : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ObjcMirrorMustInheritMirror::class
    }

    interface ObjcMirrorInteroplibMustBeImported : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ObjcMirrorInteroplibMustBeImported::class
    }

    interface ObjcInteropNotSupported : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ObjcInteropNotSupported::class
        val featureName: String
    }

    interface ObjcPointerArgumentMustBeObjcCompatible : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ObjcPointerArgumentMustBeObjcCompatible::class
    }

    interface ObjcInteropToplevelParamMustBeObjcCompatible : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ObjcInteropToplevelParamMustBeObjcCompatible::class
        val functionName: String
    }

    interface ObjcInteropToplevelRetMustBeObjcCompatible : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ObjcInteropToplevelRetMustBeObjcCompatible::class
        val functionName: String
    }

    interface ObjcMethodMustHaveForeignName : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ObjcMethodMustHaveForeignName::class
        val declarationKind: String
        val methodName: Name
    }

    interface ObjcCtorMustHaveForeignName : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ObjcCtorMustHaveForeignName::class
        val declarationKind: String
    }

    interface ObjcFuncArgumentMustBeObjcCompatible : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ObjcFuncArgumentMustBeObjcCompatible::class
        val description: String
    }

    interface ObjcFuncCallPropertyCanOnlyBeCalled : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ObjcFuncCallPropertyCanOnlyBeCalled::class
        val description: String
    }

    interface ObjcImplMustHaveObjcMirrorSuperClass : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ObjcImplMustHaveObjcMirrorSuperClass::class
    }

    interface ObjcSetterNameOnImmutableProp : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ObjcSetterNameOnImmutableProp::class
    }

    interface ObjcCjmappingInheritanceInterfaceNotSupported : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ObjcCjmappingInheritanceInterfaceNotSupported::class
    }

    interface ObjcCjmappingGenericNotSupported : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ObjcCjmappingGenericNotSupported::class
        val genericDescription: String
    }

    interface ForeignNameAppearedInChild : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ForeignNameAppearedInChild::class
        val annotationName: Name
    }

    interface ForeignNameConflictingAnnotation : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ForeignNameConflictingAnnotation::class
        val declarationName: Name
        val annotationName: Name
    }

    interface ForeignNameConflictingDerivedAnnotation : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ForeignNameConflictingDerivedAnnotation::class
        val declarationName: Name
        val annotationName: Name
        val derivedName: Name
    }

    interface IfavailableArgNoName : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IfavailableArgNoName::class
    }

    interface IfavailableArgNotLiteral : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IfavailableArgNotLiteral::class
    }

    interface IfavailableUnknownArgName : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IfavailableUnknownArgName::class
        val paramName: String
    }

    interface IfavailableLevelLimit : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = IfavailableLevelLimit::class
    }

    interface ApilevelMultiAnno : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ApilevelMultiAnno::class
    }

    interface ApilevelMissingArg : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ApilevelMissingArg::class
        val argName: Name
    }

    interface OnlyLiteralSupport : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = OnlyLiteralSupport::class
        val kind: String
    }

    interface ApilevelRefHigher : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ApilevelRefHigher::class
        val name: Name
        val refLevel: Int
        val currentLevel: Int
    }

    interface ApilevelSyscapWarning : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ApilevelSyscapWarning::class
        val syscap: Name
    }

    interface ApilevelSyscapError : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ApilevelSyscapError::class
        val syscap: Name
    }

    interface ApilevelMultiDiffSyscap : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = ApilevelMultiDiffSyscap::class
    }

    interface HideMultiAnnotation : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = HideMultiAnnotation::class
    }

    interface HideAtFuncParam : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = HideAtFuncParam::class
    }

    interface HideMissingHide : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = HideMissingHide::class
    }

    interface HideCompileTimeInvisible : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = HideCompileTimeInvisible::class
    }

    interface HideDiffParam : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = HideDiffParam::class
        val paramValue: String
    }

    interface HideMustAtEnd : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = HideMustAtEnd::class
        val annotationName: String
    }

    interface UnusedImport : CaCfirDiagnostic<CjImportItem> {
        override val diagnosticClass get() = UnusedImport::class
        val importPath: FqName
    }

    interface MockDisabled : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MockDisabled::class
        val option: String
    }

    interface MockNotInTestMode : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MockNotInTestMode::class
        val option: String
    }

    interface MockUnsupportedType : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MockUnsupportedType::class
    }

    interface MockWrongStaticDecl : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MockWrongStaticDecl::class
    }

    interface MockDoesntSupportMocking : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MockDoesntSupportMocking::class
        val name: Name
        val packageName: FqName
        val option: String
    }

    interface MockFrozenUnsupported : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MockFrozenUnsupported::class
    }

    interface MockFrozenRequired : CaCfirDiagnostic<PsiElement> {
        override val diagnosticClass get() = MockFrozenRequired::class
        val functionName: Name
    }

}

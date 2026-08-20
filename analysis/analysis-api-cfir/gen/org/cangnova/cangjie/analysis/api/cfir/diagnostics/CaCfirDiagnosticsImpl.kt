

package org.cangnova.cangjie.analysis.api.cfir.diagnostics

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
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

internal class NoConstructorImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.NoConstructor

internal class RefNotBeTypeImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.RefNotBeType

internal class NotATypeImpl(
    override val typeName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.NotAType

internal class InvalidAccessControlImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InvalidAccessControl

internal class EnumTypeCannotBeUsedAsConstructorImpl(
    override val enumName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.EnumTypeCannotBeUsedAsConstructor

internal class ConflictingOverloadsImpl(
    override val conflictingSymbols: List<String>,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjNamedDeclaration>(cfirDiagnostic, token), CaCfirDiagnostic.ConflictingOverloads

internal class RedeclarationImpl(
    override val conflictingSymbols: List<String>,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.Redeclaration

internal class ClassifierRedeclarationImpl(
    override val conflictingSymbols: List<String>,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjNamedDeclaration>(cfirDiagnostic, token), CaCfirDiagnostic.ClassifierRedeclaration

internal class UnresolvedImportImpl(
    override val reference: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.UnresolvedImport

internal class ImportConflictImpl(
    override val name: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjImportItem>(cfirDiagnostic, token), CaCfirDiagnostic.ImportConflict

internal class ImportAliasConflictImpl(
    override val alias: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjImportItem>(cfirDiagnostic, token), CaCfirDiagnostic.ImportAliasConflict

internal class SuperTypesSelfReferenceImpl(
    override val className: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjTypeReference>(cfirDiagnostic, token), CaCfirDiagnostic.SuperTypesSelfReference

internal class InheritanceCycleImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InheritanceCycle

internal class SuperTypesDuplicateImpl(
    override val typeName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.SuperTypesDuplicate

internal class InterfaceCannotInheritClassImpl(
    override val interfaceName: Name,
    override val superTypeName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjNamedDeclaration>(cfirDiagnostic, token), CaCfirDiagnostic.InterfaceCannotInheritClass

internal class ClassInheritNonClassNorInterfaceImpl(
    override val className: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjTypeReference>(cfirDiagnostic, token), CaCfirDiagnostic.ClassInheritNonClassNorInterface

internal class MultipleClassSuperTypesImpl(
    override val className: Name,
    override val superTypes: List<Name>,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjTypeReference>(cfirDiagnostic, token), CaCfirDiagnostic.MultipleClassSuperTypes

internal class IllegalMultiInheritanceImpl(
    override val className: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjTypeReference>(cfirDiagnostic, token), CaCfirDiagnostic.IllegalMultiInheritance

internal class SuperclassMustBePlacedAtFirstImpl(
    override val superClassName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjTypeReference>(cfirDiagnostic, token), CaCfirDiagnostic.SuperclassMustBePlacedAtFirst

internal class NonInheritableSuperClassImpl(
    override val superClassName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjTypeReference>(cfirDiagnostic, token), CaCfirDiagnostic.NonInheritableSuperClass

internal class IllegalExtendedTypeImpl(
    override val typeName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjTypeReference>(cfirDiagnostic, token), CaCfirDiagnostic.IllegalExtendedType

internal class ExtendDuplicateInterfaceImpl(
    override val interfaceName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjTypeReference>(cfirDiagnostic, token), CaCfirDiagnostic.ExtendDuplicateInterface

internal class ExtendNotInterfaceImpl(
    override val typeName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjTypeReference>(cfirDiagnostic, token), CaCfirDiagnostic.ExtendNotInterface

internal class ExtendOrphanRuleImpl(
    override val targetTypeName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjTypeReference>(cfirDiagnostic, token), CaCfirDiagnostic.ExtendOrphanRule

internal class ExtendGenericUsageImpl(
    override val typeParameterNames: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjTypeReference>(cfirDiagnostic, token), CaCfirDiagnostic.ExtendGenericUsage

internal class ExtendSpecializationConflictImpl(
    override val interfaceName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjTypeReference>(cfirDiagnostic, token), CaCfirDiagnostic.ExtendSpecializationConflict

internal class ExtendDefaultImplementationConflictImpl(
    override val memberName: Name,
    override val interfaceName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjTypeReference>(cfirDiagnostic, token), CaCfirDiagnostic.ExtendDefaultImplementationConflict

internal class ExtendImmutableMutInterfaceImpl(
    override val interfaceName: Name,
    override val mutMemberName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjTypeReference>(cfirDiagnostic, token), CaCfirDiagnostic.ExtendImmutableMutInterface

internal class ExtendImmutableMutPropertyImpl(
    override val propertyName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjDeclaration>(cfirDiagnostic, token), CaCfirDiagnostic.ExtendImmutableMutProperty

internal class ExtendImmutableIndexAssignmentImpl(
    override val operatorName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjDeclaration>(cfirDiagnostic, token), CaCfirDiagnostic.ExtendImmutableIndexAssignment

internal class ExtendInterfaceNotExtendableImpl(
    override val interfaceName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjTypeReference>(cfirDiagnostic, token), CaCfirDiagnostic.ExtendInterfaceNotExtendable

internal class ExtendCTypeNotAllowedImpl(
    override val typeName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjTypeReference>(cfirDiagnostic, token), CaCfirDiagnostic.ExtendCTypeNotAllowed

internal class ExtendSuperNotAllowedImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjExpression>(cfirDiagnostic, token), CaCfirDiagnostic.ExtendSuperNotAllowed

internal class StructSuperNotAllowedImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjExpression>(cfirDiagnostic, token), CaCfirDiagnostic.StructSuperNotAllowed

internal class EnumSuperNotAllowedImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjExpression>(cfirDiagnostic, token), CaCfirDiagnostic.EnumSuperNotAllowed

internal class InterfaceSuperNotAllowedImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjExpression>(cfirDiagnostic, token), CaCfirDiagnostic.InterfaceSuperNotAllowed

internal class StaticCannotBeOpenAbstractOverrideImpl(
    override val declarationName: Name?,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjNamedDeclaration>(cfirDiagnostic, token), CaCfirDiagnostic.StaticCannotBeOpenAbstractOverride

internal class MissingFuncBodyImpl(
    override val memberKind: String,
    override val memberName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MissingFuncBody

internal class MutOnlyOnFunctionImpl(
    override val declarationName: Name?,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjNamedDeclaration>(cfirDiagnostic, token), CaCfirDiagnostic.MutOnlyOnFunction

internal class NothingToOverrideImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjNamedDeclaration>(cfirDiagnostic, token), CaCfirDiagnostic.NothingToOverride

internal class OverrideStaticErrorImpl(
    override val declarationKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.OverrideStaticError

internal class RedefInstanceErrorImpl(
    override val declarationKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.RedefInstanceError

internal class InvalidOperatorParameterCountImpl(
    override val operator: String,
    override val expectedCount: String,
    override val actualCount: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InvalidOperatorParameterCount

internal class OperatorOverloadBuiltInUnaryOperatorImpl(
    override val operator: String,
    override val receiverType: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.OperatorOverloadBuiltInUnaryOperator

internal class OperatorOverloadBuiltInBinaryOperatorImpl(
    override val operator: String,
    override val receiverType: String,
    override val parameterType: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.OperatorOverloadBuiltInBinaryOperator

internal class RepeatedModifierImpl(
    override val modifier: CjKeywordToken,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.RepeatedModifier

internal class RedundantModifierImpl(
    override val modifier: CjKeywordToken,
    override val redundantBecauseOf: CjKeywordToken,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.RedundantModifier

internal class IgnoreOpenImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.IgnoreOpen

internal class IncompatibleModifiersImpl(
    override val modifier1: CjKeywordToken,
    override val modifier2: CjKeywordToken,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.IncompatibleModifiers

internal class WrongModifierTargetImpl(
    override val modifier: CjKeywordToken,
    override val target: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.WrongModifierTarget

internal class WrongModifierContainingDeclarationImpl(
    override val modifier: CjKeywordToken,
    override val container: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.WrongModifierContainingDeclaration

internal class RedundantModifierForTargetImpl(
    override val modifier: CjKeywordToken,
    override val target: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.RedundantModifierForTarget

internal class DeprecatedModifierForTargetImpl(
    override val modifier: CjKeywordToken,
    override val target: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.DeprecatedModifierForTarget

internal class DeprecatedModifierContainingDeclarationImpl(
    override val modifier: CjKeywordToken,
    override val container: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.DeprecatedModifierContainingDeclaration

internal class DeprecatedModifierPairImpl(
    override val modifier: CjKeywordToken,
    override val conflictingModifier: CjKeywordToken,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.DeprecatedModifierPair

internal class CannotWeakenAccessPrivilegeImpl(
    override val baseMemberName: Name,
    override val baseVisibility: Visibility,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjNamedDeclaration>(cfirDiagnostic, token), CaCfirDiagnostic.CannotWeakenAccessPrivilege

internal class WeakVisibilityImpl(
    override val baseMemberName: Name,
    override val baseVisibility: Visibility,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.WeakVisibility

internal class ParamNamedMismatchedImpl(
    override val baseMemberName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjNamedDeclaration>(cfirDiagnostic, token), CaCfirDiagnostic.ParamNamedMismatched

internal class NoValueForParameterImpl(
    override val parameterName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.NoValueForParameter

internal class TooManyArgumentsImpl(
    override val targetName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.TooManyArguments

internal class WrongNumberOfArgumentsImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.WrongNumberOfArguments

internal class ParametersAndArgumentsMismatchImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ParametersAndArgumentsMismatch

internal class NamedParameterNotFoundImpl(
    override val parameterName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.NamedParameterNotFound

internal class ArgumentPassedTwiceImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ArgumentPassedTwice

internal class NamedArgumentsNotAllowedImpl(
    override val targetDescription: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.NamedArgumentsNotAllowed

internal class MixingNamedAndPositionalArgumentsImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MixingNamedAndPositionalArguments

internal class NeedNamedArgumentImpl(
    override val parameterName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.NeedNamedArgument

internal class AmbiguousConstructorCallImpl(
    override val className: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.AmbiguousConstructorCall

internal class AmbiguousFunctionCallImpl(
    override val functionName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.AmbiguousFunctionCall

internal class AmbiguousArgTypeImpl(
    override val functionName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.AmbiguousArgType

internal class RecursiveConstructorCallImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.RecursiveConstructorCall

internal class MultiplePrimaryConstructorsImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MultiplePrimaryConstructors

internal class ValueTypeRecursiveImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ValueTypeRecursive

internal class IllegalThisOrSuperCallImpl(
    override val calleeName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.IllegalThisOrSuperCall

internal class ThisOrSuperNotAllowedToInitializeNonStaticMemberImpl(
    override val calleeName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ThisOrSuperNotAllowedToInitializeNonStaticMember

internal class ThisOrSuperNotAllowedToInitializeStaticMemberImpl(
    override val calleeName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ThisOrSuperNotAllowedToInitializeStaticMember

internal class ThisSuperUseErrorOutsideClassImpl(
    override val calleeName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ThisSuperUseErrorOutsideClass

internal class InvalidThisCallOutsideCtorImpl(
    override val calleeName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InvalidThisCallOutsideCtor

internal class IllegalSuperAloneImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.IllegalSuperAlone

internal class IllegalThisOutsideStructConstructorImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.IllegalThisOutsideStructConstructor

internal class IllegalPlaceOfCallingThisOrSuperImpl(
    override val calleeName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.IllegalPlaceOfCallingThisOrSuper

internal class IllegalPlaceOfCallingThisPrimaryConstructorImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.IllegalPlaceOfCallingThisPrimaryConstructor

internal class AssignmentOfMemberVariableCannotUseThisOrSuperImpl(
    override val memberName: String,
    override val contextDescription: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.AssignmentOfMemberVariableCannotUseThisOrSuper

internal class IllegalMemberUsedInOpenConstructorImpl(
    override val memberKind: String,
    override val memberName: String,
    override val className: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.IllegalMemberUsedInOpenConstructor

internal class AbstractMethodCannotBeAccessedDirectlyImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.AbstractMethodCannotBeAccessedDirectly

internal class ThisAsExpressionInFuncImpl(
    override val contextDescription: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ThisAsExpressionInFunc

internal class StaticMembersCannotCallMembersImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.StaticMembersCannotCallMembers

internal class IllegalAccessNonStaticMemberImpl(
    override val memberName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.IllegalAccessNonStaticMember

internal class StaticFunctionCannotAccessNonStaticMemberImpl(
    override val memberName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.StaticFunctionCannotAccessNonStaticMember

internal class StaticLambdaCannotAccessNonStaticImpl(
    override val memberName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.StaticLambdaCannotAccessNonStatic

internal class StaticVariableCannotAccessNonStaticMemberImpl(
    override val memberName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.StaticVariableCannotAccessNonStaticMember

internal class ObjectCannotAccessStaticMemberImpl(
    override val memberName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ObjectCannotAccessStaticMember

internal class ExplicitSuperCallRequiredImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ExplicitSuperCallRequired

internal class NoNonParamConstructorInSuperClassImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.NoNonParamConstructorInSuperClass

internal class InvalidLoopControlImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InvalidLoopControl

internal class IllegalUsageOfMemberImpl(
    override val memberName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.IllegalUsageOfMember

internal class IllegalUsageOfSuperMemberImpl(
    override val memberName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.IllegalUsageOfSuperMember

internal class UsedBeforeInitializationImpl(
    override val variableName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.UsedBeforeInitialization

internal class ClassUninitializedFieldImpl(
    override val fieldName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ClassUninitializedField

internal class GenericNoMemberMatchInUpperBoundsImpl(
    override val memberName: Name,
    override val typeParameterName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.GenericNoMemberMatchInUpperBounds

internal class GenericNoMethodMatchInUpperBoundsImpl(
    override val methodName: Name,
    override val typeParameterName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.GenericNoMethodMatchInUpperBounds

internal class CannotModifyVarImpl(
    override val variableName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CannotModifyVar

internal class ImmutableFunctionCannotAccessMutableFunctionImpl(
    override val currentFunctionName: Name,
    override val targetFunctionName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ImmutableFunctionCannotAccessMutableFunction

internal class IllegalCaptureThisImpl(
    override val ownerKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.IllegalCaptureThis

internal class CaptureThisOrInstanceFieldInFuncImpl(
    override val capturedName: Name,
    override val functionDescription: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CaptureThisOrInstanceFieldInFunc

internal class AnnotationNoConstInitImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.AnnotationNoConstInit

internal class InvalidCfuncReturnTypeImpl(
    override val actualType: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjTypeReference>(cfirDiagnostic, token), CaCfirDiagnostic.InvalidCfuncReturnType

internal class InvalidCfuncParameterTypeImpl(
    override val actualType: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjTypeReference>(cfirDiagnostic, token), CaCfirDiagnostic.InvalidCfuncParameterType

internal class OnlyCfuncCanUseAnnotationImpl(
    override val annotationName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.OnlyCfuncCanUseAnnotation

internal class IllegalScopeUseOfAnnotationImpl(
    override val annotationName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.IllegalScopeUseOfAnnotation

internal class ThrowExprWithWrongTypeImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ThrowExprWithWrongType

internal class CatchTypeMustExtendExceptionImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjTypeReference>(cfirDiagnostic, token), CaCfirDiagnostic.CatchTypeMustExtendException

internal class UselessExceptionTypeImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjTypeReference>(cfirDiagnostic, token), CaCfirDiagnostic.UselessExceptionType

internal class RangeStepCannotBeZeroImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.RangeStepCannotBeZero

internal class EffectsFeatureDisabledImpl(
    override val constructName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjElement>(cfirDiagnostic, token), CaCfirDiagnostic.EffectsFeatureDisabled

internal class CommandIncompatibleTypeImpl(
    override val actualType: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjExpression>(cfirDiagnostic, token), CaCfirDiagnostic.CommandIncompatibleType

internal class CommandHandleTypeErrorImpl(
    override val actualType: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjTypeReference>(cfirDiagnostic, token), CaCfirDiagnostic.CommandHandleTypeError

internal class ImplicitResumeOutsideHandlerImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjResumeExpression>(cfirDiagnostic, token), CaCfirDiagnostic.ImplicitResumeOutsideHandler

internal class ResumeNoWithImpl(
    override val resumptionType: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjResumeExpression>(cfirDiagnostic, token), CaCfirDiagnostic.ResumeNoWith

internal class ResumeThrowingMismatchTypeImpl(
    override val actualType: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjResumeExpression>(cfirDiagnostic, token), CaCfirDiagnostic.ResumeThrowingMismatchType

internal class MismatchingHandleBlockImpl(
    override val actualType: CaType,
    override val expectedType: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjBlockExpression>(cfirDiagnostic, token), CaCfirDiagnostic.MismatchingHandleBlock

internal class NonExhaustiveMatchImpl(
    override val missingCases: List<String>,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.NonExhaustiveMatch

internal class TuplePatternNotMatchImpl(
    override val actualTypeText: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.TuplePatternNotMatch

internal class PatternNotMatchImpl(
    override val patternText: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.PatternNotMatch

internal class EnumPatternParamSizeErrorImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.EnumPatternParamSizeError

internal class NotOverloadInMatchImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.NotOverloadInMatch

internal class MatchCaseHasNoTypeImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MatchCaseHasNoType

internal class NameInConstraintIsNotATypeParameterImpl(
    override val name: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.NameInConstraintIsNotATypeParameter

internal class OnlyOneClassBoundAllowedImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjElement>(cfirDiagnostic, token), CaCfirDiagnostic.OnlyOneClassBoundAllowed

internal class RepeatedBoundImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjElement>(cfirDiagnostic, token), CaCfirDiagnostic.RepeatedBound

internal class ConflictingUpperBoundsImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjNamedDeclaration>(cfirDiagnostic, token), CaCfirDiagnostic.ConflictingUpperBounds

internal class CannotInferParameterTypeImpl(
    override val parameter: CaTypeParameterSymbol,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjElement>(cfirDiagnostic, token), CaCfirDiagnostic.CannotInferParameterType

internal class NewInferenceErrorImpl(
    override val message: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.NewInferenceError

internal class ArrayLiteralTypeCannotBeInferredImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ArrayLiteralTypeCannotBeInferred

internal class InconsistentArrayLiteralElementTypeImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InconsistentArrayLiteralElementType

internal class TypeInferenceOnlyInputTypesErrorImpl(
    override val parameter: CaTypeParameterSymbol,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjElement>(cfirDiagnostic, token), CaCfirDiagnostic.TypeInferenceOnlyInputTypesError

internal class BuilderInferenceMultiLambdaRestrictionImpl(
    override val typeParameterName: Name,
    override val declarationName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.BuilderInferenceMultiLambdaRestriction

internal class InferredTypeVariableIntoEmptyIntersectionImpl(
    override val typeVariable: String,
    override val incompatibleTypes: List<CaType>,
    override val kindDescription: String,
    override val causingTypesText: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InferredTypeVariableIntoEmptyIntersection

internal class InferredTypeVariableIntoPossibleEmptyIntersectionImpl(
    override val typeVariable: String,
    override val incompatibleTypes: List<CaType>,
    override val kindDescription: String,
    override val causingTypesText: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InferredTypeVariableIntoPossibleEmptyIntersection

internal class TypeMismatchImpl(
    override val expectedType: CaType,
    override val actualType: CaType,
    override val isMismatchDueToNullability: Boolean,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.TypeMismatch

internal class PatternInitializerTypeMismatchImpl(
    override val expectedType: CaType,
    override val actualType: CaType,
    override val isMismatchDueToNullability: Boolean,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjNamedDeclaration>(cfirDiagnostic, token), CaCfirDiagnostic.PatternInitializerTypeMismatch

internal class ReturnTypeMismatchImpl(
    override val expectedType: CaType,
    override val actualType: CaType,
    override val isMismatchDueToNullability: Boolean,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjExpression>(cfirDiagnostic, token), CaCfirDiagnostic.ReturnTypeMismatch

internal class ArgumentTypeMismatchImpl(
    override val expectedType: CaType,
    override val actualType: CaType,
    override val isMismatchDueToNullability: Boolean,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ArgumentTypeMismatch

internal class AssignmentTypeMismatchImpl(
    override val expectedType: CaType,
    override val actualType: CaType,
    override val isMismatchDueToNullability: Boolean,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjExpression>(cfirDiagnostic, token), CaCfirDiagnostic.AssignmentTypeMismatch

internal class TypeIncompatibleImpl(
    override val contextDescription: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.TypeIncompatible

internal class VarraySizeMismatchImpl(
    override val expectedSize: Long,
    override val actualSize: Long,
    override val elementType: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.VarraySizeMismatch

internal class GenericTypeShouldBeUsedWithTypeArgumentImpl(
    override val typeName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.GenericTypeShouldBeUsedWithTypeArgument

internal class ParseThisTypeNotAllowImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjTypeReference>(cfirDiagnostic, token), CaCfirDiagnostic.ParseThisTypeNotAllow

internal class InvalidPositionOfThisTypeImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjTypeReference>(cfirDiagnostic, token), CaCfirDiagnostic.InvalidPositionOfThisType

internal class InvisibleMemberImpl(
    override val member: String,
    override val visibility: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InvisibleMember

internal class InvisibleReferenceImpl(
    override val reference: String,
    override val visibility: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InvisibleReference

internal class OverridingReturnTypeMismatchImpl(
    override val actualType: CaType,
    override val expectedType: CaType,
    override val overriddenName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.OverridingReturnTypeMismatch

internal class CannotConvertLiteralImpl(
    override val literalDescription: String,
    override val expectedType: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CannotConvertLiteral

internal class ReturnTypeIncompatibleImpl(
    override val functionName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ReturnTypeIncompatible

internal class ReturnTypeInvarianceImpl(
    override val functionName: Name,
    override val interfaceType: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ReturnTypeInvariance

internal class PropertyOverrideImplementTypeDiffImpl(
    override val actualType: CaType,
    override val expectedType: CaType,
    override val overriddenName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.PropertyOverrideImplementTypeDiff

internal class MissingEntryImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MissingEntry

internal class UnexpectedParamForEntryImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.UnexpectedParamForEntry

internal class UnexpectedReturnTypeForEntryImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.UnexpectedReturnTypeForEntry

internal class CannotOverrideInvisibleMemberImpl(
    override val memberName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjNamedDeclaration>(cfirDiagnostic, token), CaCfirDiagnostic.CannotOverrideInvisibleMember

internal class AbstractMemberNotImplementedImpl(
    override val className: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjNamedDeclaration>(cfirDiagnostic, token), CaCfirDiagnostic.AbstractMemberNotImplemented

internal class LiteralNumericOverflowImpl(
    override val literalText: String,
    override val targetType: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.LiteralNumericOverflow

internal class ConstEvalDivideByZeroImpl(
    override val operatorName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ConstEvalDivideByZero

internal class ConstEvalArithmeticOverflowImpl(
    override val operatorName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ConstEvalArithmeticOverflow

internal class ConstEvalNegativeShiftCountImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ConstEvalNegativeShiftCount

internal class ConstEvalShiftCountOverflowImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ConstEvalShiftCountOverflow

internal class UndeclaredTypeNameImpl(
    override val typeName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.UndeclaredTypeName

internal class UnresolvedReferenceImpl(
    override val reference: String,
    override val operator: String?,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.UnresolvedReference

internal class InvalidBinaryOperatorImpl(
    override val operator: String,
    override val leftType: String,
    override val rightType: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InvalidBinaryOperator

internal class NoMatchingOperatorInvokeImpl(
    override val name: String,
    override val type: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.NoMatchingOperatorInvoke

internal class NoMatchFunctionDeclarationForCallImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.NoMatchFunctionDeclarationForCall

internal class NoMatchFunctionDeclarationForRefImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.NoMatchFunctionDeclarationForRef

internal class AmbiguousFunctionReferenceImpl(
    override val name: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.AmbiguousFunctionReference

internal class NoMatchOperatorFunctionCallImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.NoMatchOperatorFunctionCall

internal class InvalidNodeAfterCheckImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InvalidNodeAfterCheck

internal class UnableToInferDeclImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.UnableToInferDecl

internal class MismatchedTypesMultipleAssignImpl(
    override val actualType: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MismatchedTypesMultipleAssign

internal class MismatchedTypesBecauseImpl(
    override val expectedType: CaType,
    override val actualType: CaType,
    override val reason: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MismatchedTypesBecause

internal class AmbiguousUseImpl(
    override val name: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.AmbiguousUse

internal class ConflictWithSubPackageImpl(
    override val declarationName: Name,
    override val subPackageName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjNamedDeclaration>(cfirDiagnostic, token), CaCfirDiagnostic.ConflictWithSubPackage

internal class CoreObjectNotFoundWhenNoPreludeImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CoreObjectNotFoundWhenNoPrelude

internal class AccessibilityWithMainHintImpl(
    override val declarationKind: String,
    override val memberName: Name,
    override val visibility: Visibility,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.AccessibilityWithMainHint

internal class AccessibilityErrorImpl(
    override val declarationKind: String,
    override val visibility: Visibility,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.AccessibilityError

internal class ParamCountMismatchImpl(
    override val expected: Int,
    override val actual: Int,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ParamCountMismatch

internal class UnableToInferReturnTypeImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.UnableToInferReturnType

internal class IncompatibleFuncBodyAndReturnTypeImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.IncompatibleFuncBodyAndReturnType

internal class UnableToInferGenericFuncImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.UnableToInferGenericFunc

internal class InvalidCalledObjectImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InvalidCalledObject

internal class InvalidReturnImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InvalidReturn

internal class InvalidReturnInStaticInitImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InvalidReturnInStaticInit

internal class InvalidSubscriptAssignParameterImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjDeclaration>(cfirDiagnostic, token), CaCfirDiagnostic.InvalidSubscriptAssignParameter

internal class InvalidSubscriptAssignParameterNumImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjDeclaration>(cfirDiagnostic, token), CaCfirDiagnostic.InvalidSubscriptAssignParameterNum

internal class InvalidSubscriptAssignReturnImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjDeclaration>(cfirDiagnostic, token), CaCfirDiagnostic.InvalidSubscriptAssignReturn

internal class StaticFunctionOverloadConflictsImpl(
    override val functionName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjNamedDeclaration>(cfirDiagnostic, token), CaCfirDiagnostic.StaticFunctionOverloadConflicts

internal class UseMutableFuncAloneImpl(
    override val functionName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.UseMutableFuncAlone

internal class UnsafeFuncCanOnlyBeCalledImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.UnsafeFuncCanOnlyBeCalled

internal class AmbiguousMatchPrimitiveExtendImpl(
    override val functionName: Name,
    override val extendedTypes: List<Name>,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.AmbiguousMatchPrimitiveExtend

internal class CannotHaveDefaultParamImpl(
    override val functionKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CannotHaveDefaultParam

internal class TrailingLambdaCannotUsedForNonFunctionImpl(
    override val paramType: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.TrailingLambdaCannotUsedForNonFunction

internal class LambdaMustHaveTypeAnnotationImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.LambdaMustHaveTypeAnnotation

internal class UseFuncCaptureVarAloneImpl(
    override val description: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.UseFuncCaptureVarAlone

internal class FuncCaptureVarCannotAssignImpl(
    override val closureName: String,
    override val captureKind: String,
    override val subjectName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.FuncCaptureVarCannotAssign

internal class FuncCaptureVarCannotReturnImpl(
    override val closureName: String,
    override val captureKind: String,
    override val subjectName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.FuncCaptureVarCannotReturn

internal class FuncCaptureVarCannotParamImpl(
    override val closureName: String,
    override val captureKind: String,
    override val subjectName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.FuncCaptureVarCannotParam

internal class FuncCaptureVarCannotExprImpl(
    override val closureName: String,
    override val captureKind: String,
    override val subjectName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.FuncCaptureVarCannotExpr

internal class UnableToInferExprImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.UnableToInferExpr

internal class ExceedFloatLiteralRangeImpl(
    override val literalText: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ExceedFloatLiteralRange

internal class FloatLiteralTooLargeImpl(
    override val type: CaType,
    override val maximum: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.FloatLiteralTooLarge

internal class FloatLiteralTooSmallImpl(
    override val type: CaType,
    override val minimum: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.FloatLiteralTooSmall

internal class InvalidUnaryExprImpl(
    override val operator: String,
    override val type: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InvalidUnaryExpr

internal class InvalidUnaryExprWithTargetImpl(
    override val operator: String,
    override val type: CaType,
    override val returnType: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InvalidUnaryExprWithTarget

internal class InvalidSubscriptExprImpl(
    override val receiverType: CaType,
    override val indexDescription: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InvalidSubscriptExpr

internal class BuiltinIndexInBoundImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.BuiltinIndexInBound

internal class CannotAssignToSubscriptImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CannotAssignToSubscript

internal class NotMemberOfImpl(
    override val memberName: Name,
    override val kind: String,
    override val typeName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.NotMemberOf

internal class MemberNotImportedImpl(
    override val memberName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MemberNotImported

internal class CannotAssignToImmutableImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CannotAssignToImmutable

internal class UnqualifiedLeftValueAssignedImpl(
    override val name: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.UnqualifiedLeftValueAssigned

internal class DifferentOrPatternImpl(
    override val description: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.DifferentOrPattern

internal class VarInOrPatternImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.VarInOrPattern

internal class VarInOrConditionImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.VarInOrCondition

internal class UnreachablePatternImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.UnreachablePattern

internal class EnumConstructorWithParamMustHaveArgsImpl(
    override val constructorName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.EnumConstructorWithParamMustHaveArgs

internal class OptionalChainNonOptionalImpl(
    override val type: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.OptionalChainNonOptional

internal class CaptureBeforeInitializationImpl(
    override val variableName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CaptureBeforeInitialization

internal class CaptureHasShadowVariableImpl(
    override val variableName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CaptureHasShadowVariable

internal class InterpolationInConstPatternImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InterpolationInConstPattern

internal class InvalidStringImplementationImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InvalidStringImplementation

internal class CannotRefToPkgNameImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CannotRefToPkgName

internal class UseExprWithoutImportImpl(
    override val importPath: FqName,
    override val exprKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.UseExprWithoutImport

internal class GenericTypeInconsistentImpl(
    override val typeParameterName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.GenericTypeInconsistent

internal class GenericArgumentNoMatchImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.GenericArgumentNoMatch

internal class InvalidTypeParamOfEnumMemberAccessImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InvalidTypeParamOfEnumMemberAccess

internal class GenericTypeArgumentNotMatchConstraintImpl(
    override val actualType: CaType,
    override val upperBound: CaType,
    override val genericType: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.GenericTypeArgumentNotMatchConstraint

internal class GenericConstraintNotLooserImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.GenericConstraintNotLooser

internal class GenericInfiniteInstantiationImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.GenericInfiniteInstantiation

internal class GenericInstantiationCausesAmbiguousFunctionsImpl(
    override val instantiation: Name,
    override val functionName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.GenericInstantiationCausesAmbiguousFunctions

internal class CannotInstantiatedByIncompleteTypeImpl(
    override val typeParameterName: Name,
    override val typeArgument: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CannotInstantiatedByIncompleteType

internal class GenericParamExistInClassIrrelevantUpperboundRecursivelyImpl(
    override val typeParameterName: Name,
    override val upperBound: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.GenericParamExistInClassIrrelevantUpperboundRecursively

internal class GenericParamDirectlyRecursiveImpl(
    override val typeParameterName: Name,
    override val boundName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.GenericParamDirectlyRecursive

internal class UpperBoundMustBeClassOrInterfaceImpl(
    override val upperBound: CaType,
    override val typeParameterName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.UpperBoundMustBeClassOrInterface

internal class GenericStaticAccessImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.GenericStaticAccess

internal class PrimitiveTypeAsGenericsArgImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.PrimitiveTypeAsGenericsArg

internal class MeetConstraintIndirectlyImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MeetConstraintIndirectly

internal class GenericUpperBoundsMustBeJavaInJavaImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.GenericUpperBoundsMustBeJavaInJava

internal class InheritMemberKindInconsistentImpl(
    override val memberKind: String,
    override val memberName: Name,
    override val superMemberKind: String,
    override val containerName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InheritMemberKindInconsistent

internal class StaticAndNonStaticMemberCannotHaveSameNameImpl(
    override val memberStaticKind: String,
    override val memberName: Name,
    override val superMemberStaticKind: String,
    override val containerKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.StaticAndNonStaticMemberCannotHaveSameName

internal class MemberVariableCanNotShadowImpl(
    override val memberName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MemberVariableCanNotShadow

internal class CannotOverrideImpl(
    override val memberKind: String,
    override val memberName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CannotOverride

internal class InvalidOverrideMemberInClassImpl(
    override val memberKind: String,
    override val memberName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InvalidOverrideMemberInClass

internal class NeedMemberImplementationImpl(
    override val extendName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.NeedMemberImplementation

internal class InterfaceMemberMustBeImplementedImpl(
    override val memberKind: String,
    override val memberName: Name,
    override val extendName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InterfaceMemberMustBeImplemented

internal class IncompatibleMutModifierBetweenStructAndInterfaceImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.IncompatibleMutModifierBetweenStructAndInterface

internal class InheritSuperMemberKindInconsistentImpl(
    override val memberName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InheritSuperMemberKindInconsistent

internal class InheritMemberTypeInconsistentImpl(
    override val aspect: String,
    override val memberKind: String,
    override val memberName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InheritMemberTypeInconsistent

internal class InheritAbstractClassStaticUnimplementFuncImpl(
    override val className: Name,
    override val memberKind: String,
    override val memberName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InheritAbstractClassStaticUnimplementFunc

internal class InvalidMemberVisibilityInClassImpl(
    override val modifier: String,
    override val memberKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InvalidMemberVisibilityInClass

internal class CannotInheritSealedImpl(
    override val verb: String,
    override val kind: String,
    override val sealedKind: String,
    override val sealedName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjTypeReference>(cfirDiagnostic, token), CaCfirDiagnostic.CannotInheritSealed

internal class InheritThreadContextInvalidImpl(
    override val declarationName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InheritThreadContextInvalid

internal class InheritThreadContextNotOpenImpl(
    override val declarationName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InheritThreadContextNotOpen

internal class InheritNotReturnThisImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjNamedDeclaration>(cfirDiagnostic, token), CaCfirDiagnostic.InheritNotReturnThis

internal class SpawnArgInvalidImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.SpawnArgInvalid

internal class SpawnArgNoEffectImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.SpawnArgNoEffect

internal class InterfaceCallWithUnimplementedCallImpl(
    override val memberKind: String,
    override val memberName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InterfaceCallWithUnimplementedCall

internal class TypeUninitializedStaticFieldImpl(
    override val fieldName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.TypeUninitializedStaticField

internal class InstanceFuncCannotBeUsedInFinalizerImpl(
    override val memberKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InstanceFuncCannotBeUsedInFinalizer

internal class FinalizerForbiddenInClassImpl(
    override val className: Name,
    override val classKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.FinalizerForbiddenInClass

internal class CannotCurryingImpl(
    override val declarationKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CannotCurrying

internal class CannotHaveParameterImpl(
    override val declarationKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CannotHaveParameter

internal class ForbidGenericFinalizerImpl(
    override val finalizerName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ForbidGenericFinalizer

internal class NonAbstractClassCannotBeSealedImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjNamedDeclaration>(cfirDiagnostic, token), CaCfirDiagnostic.NonAbstractClassCannotBeSealed

internal class StaticVariableUseGenericParameterImpl(
    override val typeParameterName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.StaticVariableUseGenericParameter

internal class CstructCannotImplInterfacesImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CstructCannotImplInterfaces

internal class ExportSamePrivateDeclImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ExportSamePrivateDecl

internal class ExtendFunctionCannotOverriddenImpl(
    override val memberKind: String,
    override val memberName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ExtendFunctionCannotOverridden

internal class ExtendMemberCannotShadowImpl(
    override val memberName: Name,
    override val targetTypeName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ExtendMemberCannotShadow

internal class ExtendIllegalMemberImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ExtendIllegalMember

internal class ExtendCheckSequenceCannotDecideImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ExtendCheckSequenceCannotDecide

internal class ExportExtendDependNonExportExtendImpl(
    override val functionNames: List<Name>,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ExportExtendDependNonExportExtend

internal class ExtendAJavaTypeImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjTypeReference>(cfirDiagnostic, token), CaCfirDiagnostic.ExtendAJavaType

internal class ExtendRefTargetCannotBeJavaImplImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ExtendRefTargetCannotBeJavaImpl

internal class TypeCannotExtendImportedInterfaceImpl(
    override val kind: String,
    override val typeName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.TypeCannotExtendImportedInterface

internal class PropertyMustHaveAccessorsImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjDeclaration>(cfirDiagnostic, token), CaCfirDiagnostic.PropertyMustHaveAccessors

internal class ImmutablePropertyWithSetterImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjDeclaration>(cfirDiagnostic, token), CaCfirDiagnostic.ImmutablePropertyWithSetter

internal class PropertyHaveSameDeclarationInInheritMutImpl(
    override val propertyName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.PropertyHaveSameDeclarationInInheritMut

internal class PropertyHaveSameDeclarationInInheritImmutImpl(
    override val propertyName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.PropertyHaveSameDeclarationInInheritImmut

internal class PropertyMustImplementBothImpl(
    override val propertyName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.PropertyMustImplementBoth

internal class ExpectConstImpl(
    override val kind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ExpectConst

internal class CannotDefineVarInConstFunctionImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CannotDefineVarInConstFunction

internal class NoConstInitImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.NoConstInit

internal class ClassConstInitWithVarImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ClassConstInitWithVar

internal class AnnotationArgTargetImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.AnnotationArgTarget

internal class AnnotationArgTargetArrayLitImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.AnnotationArgTargetArrayLit

internal class AnnotationNonPublicImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.AnnotationNonPublic

internal class AnnotationCustomPlaceImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.AnnotationCustomPlace

internal class AnnotationErrorArgNumImpl(
    override val annotationName: String,
    override val expectedArgs: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.AnnotationErrorArgNum

internal class AnnotationErrorArgRangeImpl(
    override val annotationName: String,
    override val supportedArgs: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.AnnotationErrorArgRange

internal class AnnotationErrorObjectImpl(
    override val annotationName: String,
    override val validTargets: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.AnnotationErrorObject

internal class CannotUseAnnotationJffiImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CannotUseAnnotationJffi

internal class AnnotationNotApplicableJffiImpl(
    override val annotationName: String,
    override val target: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.AnnotationNotApplicableJffi

internal class InoutModifyCstringOrZerosizedImpl(
    override val type: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InoutModifyCstringOrZerosized

internal class InoutModifyNonCtypeImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InoutModifyNonCtype

internal class InoutMustBeVarVariableImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InoutMustBeVarVariable

internal class InoutModifyHeapVariableImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InoutModifyHeapVariable

internal class InoutCanOnlyUsedInCfuncCallingImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InoutCanOnlyUsedInCfuncCalling

internal class InoutMismatchImpl(
    override val type: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InoutMismatch

internal class InvalidInoutArgumentImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InvalidInoutArgument

internal class DuplicateInoutArgumentImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.DuplicateInoutArgument

internal class VarrayArgsNumberMismatchImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.VarrayArgsNumberMismatch

internal class VarraySubscriptNumImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.VarraySubscriptNum

internal class VarrayInCfuncImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.VarrayInCfunc

internal class VarrayArgTypeWithReftypeImpl(
    override val type: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.VarrayArgTypeWithReftype

internal class ResumptionHandleTypeErrorImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ResumptionHandleTypeError

internal class ResumptionIncorrectReturnTypeImpl(
    override val resumptionType: CaType,
    override val tryBlockType: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ResumptionIncorrectReturnType

internal class CommandResumptionMismatchImpl(
    override val resumptionParamType: CaType,
    override val commandResultType: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CommandResumptionMismatch

internal class ResumeWrongResumptionTypeImpl(
    override val actualType: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ResumeWrongResumptionType

internal class ReturnInTryHandleBlockImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ReturnInTryHandleBlock

internal class UselessCommandTypeImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.UselessCommandType

internal class DeprecatedErrorImpl(
    override val kind: String,
    override val name: Name,
    override val message: String,
    override val replacement: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.DeprecatedError

internal class DeprecatedWarningImpl(
    override val kind: String,
    override val name: Name,
    override val message: String,
    override val replacement: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.DeprecatedWarning

internal class DeprecationWeakeningImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.DeprecationWeakening

internal class DeprecationOverrideErrorImpl(
    override val kind: String,
    override val name: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.DeprecationOverrideError

internal class DeprecationOverrideWarningImpl(
    override val kind: String,
    override val name: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.DeprecationOverrideWarning

internal class DeprecationRedefErrorImpl(
    override val kind: String,
    override val name: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.DeprecationRedefError

internal class DeprecationRedefWarningImpl(
    override val kind: String,
    override val name: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.DeprecationRedefWarning

internal class CommonOpenClassNoInitImpl(
    override val className: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CommonOpenClassNoInit

internal class MultipleCommonImplementationsImpl(
    override val kind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MultipleCommonImplementations

internal class CommonDirectExtensionHasDuplicatePrivateMembersImpl(
    override val extendName: Name,
    override val memberKind: String,
    override val memberName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CommonDirectExtensionHasDuplicatePrivateMembers

internal class CommonDirectExtensionHasCommonPrivateMembersImpl(
    override val memberKind: String,
    override val memberName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CommonDirectExtensionHasCommonPrivateMembers

internal class NotMatchedImpl(
    override val declarationName: Name,
    override val kind: String,
    override val matchKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.NotMatched

internal class SpecificVarNotMatchLetImpl(
    override val specificName: Name,
    override val commonName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.SpecificVarNotMatchLet

internal class SpecificInitCommonPrimaryConstructorImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.SpecificInitCommonPrimaryConstructor

internal class SpecificHasDifferentKindImpl(
    override val specificKind: String,
    override val commonKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.SpecificHasDifferentKind

internal class SpecificPrimaryUnmatchedVarDeclImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.SpecificPrimaryUnmatchedVarDecl

internal class CommonNonExhaustivePlatformExhaustiveMismatchImpl(
    override val commonKind: String,
    override val specificKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CommonNonExhaustivePlatformExhaustiveMismatch

internal class SpecificHasDifferentTypeImpl(
    override val kind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.SpecificHasDifferentType

internal class SpecificMemberMustHaveImplementationImpl(
    override val memberKind: String,
    override val containerKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.SpecificMemberMustHaveImplementation

internal class SpecificHasDifferentModifierImpl(
    override val kind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.SpecificHasDifferentModifier

internal class SpecificHasDifferentAnnotationImpl(
    override val kind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.SpecificHasDifferentAnnotation

internal class SpecificHasDeprecatedAnnotationImpl(
    override val annotationName: Name,
    override val kind: String,
    override val name: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.SpecificHasDeprecatedAnnotation

internal class CjmpParameterDefaultValueBothSidesImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CjmpParameterDefaultValueBothSides

internal class SpecificHasDifferentParameterImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.SpecificHasDifferentParameter

internal class SpecificHasDifferentSuperTypeImpl(
    override val kind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.SpecificHasDifferentSuperType

internal class SpecificHasDuplicateExtensionsImpl(
    override val extendName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.SpecificHasDuplicateExtensions

internal class CommonPackageHasMainImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CommonPackageHasMain

internal class CommonStaticLetCantBeInitializedInStaticInitImpl(
    override val variableName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CommonStaticLetCantBeInitializedInStaticInit

internal class CommonAssignToCommonImmutableInCtorImpl(
    override val variableName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CommonAssignToCommonImmutableInCtor

internal class CjmpAbstractClassMemberHasNoExplicitModifierImpl(
    override val className: Name,
    override val memberKind: String,
    override val modifier: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CjmpAbstractClassMemberHasNoExplicitModifier

internal class ExplicitlyAbstractCanNotHaveBodyImpl(
    override val memberKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ExplicitlyAbstractCanNotHaveBody

internal class ExplicitlyAbstractOnlyForCjmpAbstractClassImpl(
    override val memberKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ExplicitlyAbstractOnlyForCjmpAbstractClass

internal class OpenAbstractSpecificCanNotReplaceOpenCommonImpl(
    override val commonKind: String,
    override val specificKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.OpenAbstractSpecificCanNotReplaceOpenCommon

internal class CjmpNonSpecificAbstractMemberInSpecificClassImpl(
    override val className: Name,
    override val memberKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CjmpNonSpecificAbstractMemberInSpecificClass

internal class CommonGenericFrozenNotSupportedImpl(
    override val kind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CommonGenericFrozenNotSupported

internal class CommonGenericRenameNotSupportedImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CommonGenericRenameNotSupported

internal class CommonSpecificAnnotationNotAllowedImpl(
    override val annotationName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CommonSpecificAnnotationNotAllowed

internal class JavaIncorrectUseBetweenTypesImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.JavaIncorrectUseBetweenTypes

internal class JavaNonJtypeImpl(
    override val typePosition: String,
    override val memberKind: String,
    override val memberName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.JavaNonJtype

internal class JavaInvalidUnitImpl(
    override val typePosition: String,
    override val memberKind: String,
    override val memberName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.JavaInvalidUnit

internal class JavaAppInheritExtImpl(
    override val verb: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.JavaAppInheritExt

internal class JavaUnsupportedDeclImpl(
    override val declKind: String,
    override val memberKind: String,
    override val memberName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.JavaUnsupportedDecl

internal class MissingJavaInteropAnnotationImpl(
    override val kind: String,
    override val name: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MissingJavaInteropAnnotation

internal class ShadowCannotInTypeArgsImpl(
    override val name: Name,
    override val fieldName: Name,
    override val superType: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ShadowCannotInTypeArgs

internal class UnsupportedTypeArgumentInJavaInteropImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.UnsupportedTypeArgumentInJavaInterop

internal class StaticMemberInInterfaceMustHasBodyImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.StaticMemberInInterfaceMustHasBody

internal class DefineJavaAnnotationImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.DefineJavaAnnotation

internal class InvalidUseOfJavaAnnotationImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InvalidUseOfJavaAnnotation

internal class InvalidUseOfAnnotationJffiImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.InvalidUseOfAnnotationJffi

internal class VariableOfJavaTypeImpl(
    override val kind: String,
    override val type: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.VariableOfJavaType

internal class GenericParameterOfJavaTypeImpl(
    override val genericName: Name,
    override val type: CaType,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.GenericParameterOfJavaType

internal class JavaInteropNotSupportedImpl(
    override val featureName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.JavaInteropNotSupported

internal class JavaMirrorCtorArgMustBeJavaMirrorImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.JavaMirrorCtorArgMustBeJavaMirror

internal class JavaMirrorMethodArgMustBeJavaMirrorImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.JavaMirrorMethodArgMustBeJavaMirror

internal class JavaMirrorMethodRetUnsupportedImpl(
    override val returnType: CaType,
    override val classKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.JavaMirrorMethodRetUnsupported

internal class JavaMirrorPropMustBeJavaMirrorImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.JavaMirrorPropMustBeJavaMirror

internal class JavaMirrorSubtypeMustBeAnnotatedImpl(
    override val superName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.JavaMirrorSubtypeMustBeAnnotated

internal class JavaMirrorCannotInheritPureCangjieTypeImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.JavaMirrorCannotInheritPureCangjieType

internal class JavaImplCannotInheritPureCangjieTypeImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.JavaImplCannotInheritPureCangjieType

internal class JavaMirrorSubtypeAnnoMustInheritMirrorImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.JavaMirrorSubtypeAnnoMustInheritMirror

internal class JavaMirrorCannotBeExtendedWithInterfaceImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.JavaMirrorCannotBeExtendedWithInterface

internal class JavaImplCannotBeExtendedWithInterfaceImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.JavaImplCannotBeExtendedWithInterface

internal class JavaImplRedefinitionImpl(
    override val declarationName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.JavaImplRedefinition

internal class JavaMirrorInteroplibMustBeImportedImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.JavaMirrorInteroplibMustBeImported

internal class JavaHasDefaultAnnotationArgsImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.JavaHasDefaultAnnotationArgs

internal class JavaHasDefaultAnnotationIsInWrongPlaceImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.JavaHasDefaultAnnotationIsInWrongPlace

internal class JavaHasDefaultConflictWithStaticImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.JavaHasDefaultConflictWithStatic

internal class CjmappingStructGenericNotSupportedImpl(
    override val genericDescription: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CjmappingStructGenericNotSupported

internal class CjmappingStructInheritanceInterfaceNotSupportedImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CjmappingStructInheritanceInterfaceNotSupported

internal class CjmappingDeclNotSupportedImpl(
    override val kind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CjmappingDeclNotSupported

internal class CjmappingMethodArgNotSupportedImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CjmappingMethodArgNotSupported

internal class CjmappingMethodRetUnsupportedImpl(
    override val returnType: CaType,
    override val containerKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CjmappingMethodRetUnsupported

internal class CjMappingGenericMethodNotGetInstanceConfigImpl(
    override val configName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.CjMappingGenericMethodNotGetInstanceConfig

internal class ObjcInteropCtorParamMustBeObjcCompatibleImpl(
    override val declarationKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ObjcInteropCtorParamMustBeObjcCompatible

internal class ObjcInteropMethodParamMustBeObjcCompatibleImpl(
    override val declarationKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ObjcInteropMethodParamMustBeObjcCompatible

internal class ObjcInteropMethodRetMustBeObjcCompatibleImpl(
    override val declarationKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ObjcInteropMethodRetMustBeObjcCompatible

internal class ObjcInteropPropMustBeObjcCompatibleImpl(
    override val declarationKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ObjcInteropPropMustBeObjcCompatible

internal class ObjcInteropFieldMustBeObjcCompatibleImpl(
    override val declarationKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ObjcInteropFieldMustBeObjcCompatible

internal class ObjcMirrorDeclCannotInheritImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ObjcMirrorDeclCannotInherit

internal class ObjcMirrorSubtypeCannotMultipleInheritImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ObjcMirrorSubtypeCannotMultipleInherit

internal class ObjcMirrorSubtypeMustBeAnnotatedImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ObjcMirrorSubtypeMustBeAnnotated

internal class ObjcMirrorSubtypeMustInheritMirrorImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ObjcMirrorSubtypeMustInheritMirror

internal class ObjcMirrorMustInheritMirrorImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ObjcMirrorMustInheritMirror

internal class ObjcMirrorInteroplibMustBeImportedImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ObjcMirrorInteroplibMustBeImported

internal class ObjcInteropNotSupportedImpl(
    override val featureName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ObjcInteropNotSupported

internal class ObjcPointerArgumentMustBeObjcCompatibleImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ObjcPointerArgumentMustBeObjcCompatible

internal class ObjcInteropToplevelParamMustBeObjcCompatibleImpl(
    override val functionName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ObjcInteropToplevelParamMustBeObjcCompatible

internal class ObjcInteropToplevelRetMustBeObjcCompatibleImpl(
    override val functionName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ObjcInteropToplevelRetMustBeObjcCompatible

internal class ObjcMethodMustHaveForeignNameImpl(
    override val declarationKind: String,
    override val methodName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ObjcMethodMustHaveForeignName

internal class ObjcCtorMustHaveForeignNameImpl(
    override val declarationKind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ObjcCtorMustHaveForeignName

internal class ObjcFuncArgumentMustBeObjcCompatibleImpl(
    override val description: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ObjcFuncArgumentMustBeObjcCompatible

internal class ObjcFuncCallPropertyCanOnlyBeCalledImpl(
    override val description: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ObjcFuncCallPropertyCanOnlyBeCalled

internal class ObjcImplMustHaveObjcMirrorSuperClassImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ObjcImplMustHaveObjcMirrorSuperClass

internal class ObjcSetterNameOnImmutablePropImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ObjcSetterNameOnImmutableProp

internal class ObjcCjmappingInheritanceInterfaceNotSupportedImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ObjcCjmappingInheritanceInterfaceNotSupported

internal class ObjcCjmappingGenericNotSupportedImpl(
    override val genericDescription: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ObjcCjmappingGenericNotSupported

internal class ForeignNameAppearedInChildImpl(
    override val annotationName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ForeignNameAppearedInChild

internal class ForeignNameConflictingAnnotationImpl(
    override val declarationName: Name,
    override val annotationName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ForeignNameConflictingAnnotation

internal class ForeignNameConflictingDerivedAnnotationImpl(
    override val declarationName: Name,
    override val annotationName: Name,
    override val derivedName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ForeignNameConflictingDerivedAnnotation

internal class IfavailableArgNoNameImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.IfavailableArgNoName

internal class IfavailableArgNotLiteralImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.IfavailableArgNotLiteral

internal class IfavailableUnknownArgNameImpl(
    override val paramName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.IfavailableUnknownArgName

internal class IfavailableLevelLimitImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.IfavailableLevelLimit

internal class ApilevelMultiAnnoImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ApilevelMultiAnno

internal class ApilevelMissingArgImpl(
    override val argName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ApilevelMissingArg

internal class OnlyLiteralSupportImpl(
    override val kind: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.OnlyLiteralSupport

internal class ApilevelRefHigherImpl(
    override val name: Name,
    override val refLevel: Int,
    override val currentLevel: Int,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ApilevelRefHigher

internal class ApilevelSyscapWarningImpl(
    override val syscap: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ApilevelSyscapWarning

internal class ApilevelSyscapErrorImpl(
    override val syscap: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ApilevelSyscapError

internal class ApilevelMultiDiffSyscapImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.ApilevelMultiDiffSyscap

internal class HideMultiAnnotationImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.HideMultiAnnotation

internal class HideAtFuncParamImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.HideAtFuncParam

internal class HideMissingHideImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.HideMissingHide

internal class HideCompileTimeInvisibleImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.HideCompileTimeInvisible

internal class HideDiffParamImpl(
    override val paramValue: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.HideDiffParam

internal class HideMustAtEndImpl(
    override val annotationName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.HideMustAtEnd

internal class UnusedImportImpl(
    override val importPath: FqName,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjImportItem>(cfirDiagnostic, token), CaCfirDiagnostic.UnusedImport

internal class UnusedExpressionImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<CjExpression>(cfirDiagnostic, token), CaCfirDiagnostic.UnusedExpression

internal class UnusedVariableImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.UnusedVariable

internal class UnusedFunctionImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.UnusedFunction

internal class TypealiasUnusedTypeParametersImpl(
    override val typeParameters: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.TypealiasUnusedTypeParameters

internal class TypealiasCycleImpl(
    override val typeAlias: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.TypealiasCycle

internal class MockDisabledImpl(
    override val option: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MockDisabled

internal class MockNotInTestModeImpl(
    override val option: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MockNotInTestMode

internal class MockUnsupportedTypeImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MockUnsupportedType

internal class MockWrongStaticDeclImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MockWrongStaticDecl

internal class MockDoesntSupportMockingImpl(
    override val name: Name,
    override val packageName: FqName,
    override val option: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MockDoesntSupportMocking

internal class MockFrozenUnsupportedImpl(
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MockFrozenUnsupported

internal class MockFrozenRequiredImpl(
    override val functionName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MockFrozenRequired

internal class MacroNotExpandedImpl(
    override val macroName: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroNotExpanded

internal class MacroExpansionFailedImpl(
    override val macroName: String,
    override val reason: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroExpansionFailed

internal class MacroDiagReportErrorImpl(
    override val message: String,
    override val hint: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroDiagReportError

internal class MacroDiagReportWarningImpl(
    override val message: String,
    override val hint: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroDiagReportWarning

internal class MacroUndefinedPackageImpl(
    override val packageName: String,
    override val reason: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroUndefinedPackage

internal class MacroUndeclaredIdentifierImpl(
    override val name: Name,
    override val reason: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroUndeclaredIdentifier

internal class MacroExpectMacroDefinitionImpl(
    override val target: String,
    override val reason: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroExpectMacroDefinition

internal class MacroDependencyCompileFailedImpl(
    override val packageName: String,
    override val reason: String,
    override val diagnosticsRef: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroDependencyCompileFailed

internal class MacroAmbiguousMatchImpl(
    override val macroName: String,
    override val targets: List<FqName>,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroAmbiguousMatch

internal class MacroCannotFindDependencyBchirImpl(
    override val packageName: String,
    override val path: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroCannotFindDependencyBchir

internal class MacroExpectPlainMacroImpl(
    override val macroName: String,
    override val reason: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroExpectPlainMacro

internal class MacroExpectAttributedMacroImpl(
    override val macroName: String,
    override val reason: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroExpectAttributedMacro

internal class MacroExpandAtexclImpl(
    override val macroName: String,
    override val reason: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroExpandAtexcl

internal class MacroInvalidAttrTokensImpl(
    override val macroName: String,
    override val reason: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroInvalidAttrTokens

internal class MacroInvalidInputTokensImpl(
    override val macroName: String,
    override val reason: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroInvalidInputTokens

internal class MacroInvalidEscapeImpl(
    override val macroName: String,
    override val reason: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroInvalidEscape

internal class MacroSamePackageDefCallImpl(
    override val macroName: String,
    override val packageName: FqName,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroSamePackageDefCall

internal class MacroAliasConflictImpl(
    override val alias: Name,
    override val targets: List<FqName>,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroAliasConflict

internal class MacroExecutorUnavailableImpl(
    override val hint: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroExecutorUnavailable

internal class MacroCannotOpenLibImpl(
    override val libPath: String,
    override val reason: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroCannotOpenLib

internal class MacroCannotFindMethodImpl(
    override val macroName: String,
    override val reason: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroCannotFindMethod

internal class MacroEvaluateFailedImpl(
    override val macroName: String,
    override val reason: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroEvaluateFailed

internal class MacroExpandFailedImpl(
    override val macroName: String,
    override val reason: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroExpandFailed

internal class MacroExpandCodeShouldNotHaveMacrocallImpl(
    override val macroName: String,
    override val reason: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroExpandCodeShouldNotHaveMacrocall

internal class MacroCallSaveFileFailedImpl(
    override val macroName: String,
    override val reason: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroCallSaveFileFailed

internal class MacroExecutorProtocolErrorImpl(
    override val reason: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroExecutorProtocolError

internal class MacroExecutorServerDisconnectedImpl(
    override val reason: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroExecutorServerDisconnected

internal class MacroExecutorTimeoutImpl(
    override val reason: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroExecutorTimeout

internal class MacroExecutorServerCrashImpl(
    override val reason: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroExecutorServerCrash

internal class MacroReevaluationFailedImpl(
    override val macroName: String,
    override val reason: String,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroReevaluationFailed

internal class MacroUnresolvedImpl(
    override val macroName: Name,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroUnresolved

internal class MacroCycleImpl(
    override val macroName: String,
    override val cycleChain: List<String>,
    cfirDiagnostic: CjPsiDiagnostic,
    token: CaLifetimeToken,
) : CaAbstractCfirDiagnostic<PsiElement>(cfirDiagnostic, token), CaCfirDiagnostic.MacroCycle


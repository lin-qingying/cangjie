@file:OptIn(SuspiciousFakeSourceCheck::class)

package org.cangnova.cangjie.source

import com.intellij.lang.LighterASTNode
import com.intellij.lang.TreeBackedLighterAST
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.utils.getElementTextWithContext
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

sealed class CjSourceElementKind {
    abstract val shouldSkipErrorTypeReporting: Boolean
}

object CjRealSourceElementKind : CjSourceElementKind() {
    override val shouldSkipErrorTypeReporting: Boolean
        get() = false
}

sealed class CjFakeSourceElementKind(final override val shouldSkipErrorTypeReporting: Boolean = false) : CjSourceElementKind() {
    /**
     * for some fir expression implicit return typeRef is generated
     * some of them are: break, continue, return, throw, string concat,
     * destruction parameters, function literals, explicitly boolean expressions
     */
    object ImplicitTypeRef : CjFakeSourceElementKind(shouldSkipErrorTypeReporting = true)

    /**
     * for errors on smartcast types then the type is brought in by an implicit this receiver expression
     */
    object ImplicitThisReceiverExpression : CjFakeSourceElementKind()

    /**
     * for implicit context parameter arguments of calls.
     */
    object ImplicitContextParameterArgument : CjFakeSourceElementKind()

    /**
     * for type arguments that were inferred as opposed to specified
     * explicitly via `<>`
     */
    object ImplicitTypeArgument : CjFakeSourceElementKind()

    /**
     * for ConeErrorTypes seen through a typealias expansion
     */
    object ErroneousTypealiasExpansion : CjFakeSourceElementKind(shouldSkipErrorTypeReporting = true)

    /**
     * for return types of anonymous functions, because ImplicitTypeRef
     * may sometimes hide the diagnostic turning red code into green
     */
    object ImplicitFunctionReturnType : CjFakeSourceElementKind()

    /**
     * for each class special class self type ref is created
     * and have a fake source referencing it
     */
    object ClassSelfTypeRef : CjFakeSourceElementKind()

    /**
     * FirErrorTypeRef may be built using unresolved firExpression
     * and have a fake source referencing it
     */
    object ErrorTypeRef : CjFakeSourceElementKind()

    /**
     * for properties without accessors default getter & setter are generated
     * they have a fake source which refers to property
     */
    object DefaultAccessor : CjFakeSourceElementKind(shouldSkipErrorTypeReporting = true)

    /**
     * for delegated properties, getter & setter calls to the delegate
     * they have a fake source which refers to the call that creates the delegate
     */
    object DelegatedPropertyAccessor : CjFakeSourceElementKind()

    /**
     * for kt classes without implicit primary constructor one is generated
     * with a fake source which refers to containing class
     */
    object ImplicitConstructor : CjFakeSourceElementKind()

    /**
     * for constructor type parameters, because they refer to the same source
     * as the class type parameters themselves
     */
    object ConstructorTypeParameter : CjFakeSourceElementKind()

    /**
     * for constructors which do not have delegated constructor call the fake one is generated
     * with a fake sources which refers to the original constructor
     */
    object DelegatingConstructorCall : CjFakeSourceElementKind()

    /**
     * for enum entry with bodies the initializer in a form of anonymous object is generated
     * with a fake sources which refers to the enum entry
     */
    object EnumInitializer : CjFakeSourceElementKind()

    /**
     *  for lambdas with implicit return the return statement is generated which is labeled
     *  with a fake sources which refers to the target expression
     */
    object GeneratedLambdaLabel : CjFakeSourceElementKind()

    /**
     * for error element which is created for dangling modifier lists
     */
    object DanglingModifierList : CjFakeSourceElementKind()

    /** for lambdas & functions with expression bodies the return statement is added
     * with a fake sources which refers to the return target
     */
    sealed class ImplicitReturn : CjFakeSourceElementKind() {
        object FromExpressionBody : ImplicitReturn()

        object FromLastStatement : ImplicitReturn()
    }

    sealed class ImplicitUnit : CjFakeSourceElementKind() {
        /** this source is used for implicit returns from empty lambdas {}
         * fake source refers to the lambda expression
         */
        object ForEmptyLambda : ImplicitUnit()

        /** this source is used for 'return' without given value converted to 'return Unit'
         * fake source refers to the return statement
         */
        object Return : ImplicitUnit()

        /** this source is used for `a[i] = b` or `a[i] += b` converted to `{ a[i] = b; Unit }`
         * fake source refers to the assignment statement
         */
        object IndexedAssignmentCoercion : ImplicitUnit()
    }

    /**
     * delegates are wrapped into FirWrappedDelegateExpression
     * with a fake sources which refers to delegated expression
     */
    object WrappedDelegate : CjFakeSourceElementKind()

    /**
     *  `for (i in list) { println(i) }` is converted to
     *  ```
     *  val <iterator>: = list.iterator()
     *  while(<iterator>.hasNext()) {
     *    val i = <iterator>.next()
     *    println(i)
     *  }
     *  ```
     *  where the generated WHILE loop has source element of initial FOR loop,
     *  other generated elements are marked as fake ones
     */
    object DesugaredForLoop : CjFakeSourceElementKind()

    object ImplicitInvokeCall : CjFakeSourceElementKind()

    /**
     * Consider an atomic qualified access like `i`. In the FIR tree, both the FirQualifiedAccessExpression and its calleeReference uses
     * `i` as the source. Hence, this fake kind is set on the `calleeReference` to make sure no PSI element is shared by multiple FIR
     * elements. This also applies to `this` and `super` references.
     */
    object ReferenceInAtomicQualifiedAccess : CjFakeSourceElementKind()

    /**
     * for enum classes we have valueOf & values functions generated
     * with a fake sources which refers to this the enum class
     */
    object EnumGeneratedDeclaration : CjFakeSourceElementKind()

    /**
     * for enum classes we can have an implicit supertype ref to `Enum` with a fake source.
     */
    object EnumSuperTypeRef : CjFakeSourceElementKind()

    /**
     * for record classes we can have an implicit supertype ref to `Record` with a fake source.
     */
    object RecordSuperTypeRef : CjFakeSourceElementKind()

    /**
     * `when (x) { "abc" -> 42 }` --> `when(val $subj = x) { $subj == "abc" -> 42 }`
     * where `$subj == "42"` has fake psi source which refers to "42" as inner expression
     * and `$subj` fake source refers to "42" as `KtWhenCondition`.
     */
    object WhenCondition : CjFakeSourceElementKind()

    /**
     * for additional FIR built for code fragments
     */
    object CodeFragment : CjFakeSourceElementKind()

    /**
     * `when { is Int -> 42 }` --> `when { $subj is Int -> 42 }`
     * where `$subj` is unresolved because there was no subject.
     */
    object UnresolvedWhenConditionSubject : CjFakeSourceElementKind()

    /**
     * for primary constructor parameter the corresponding class property is generated
     * with a fake sources which refers to this the corresponding parameter
     */
    object PropertyFromParameter : CjFakeSourceElementKind(shouldSkipErrorTypeReporting = true)

    /**
     * `if (true) 1` --> `if(true) { 1 }`
     * with a fake sources for the block which refers to the wrapped expression
     */
    object SingleExpressionBlock : CjFakeSourceElementKind()

    /**
     * this source is used for a single fake block created for indexed assignments expression,
     * see ImplicitUnit.IndexedAssignmentCoercion
     */
    object IndexedAssignmentCoercionBlock : CjFakeSourceElementKind()

    /**
     * Contract statements are wrapped in a special block to be reused between a contract FIR and a function body.
     */
    object ContractBlock : CjFakeSourceElementKind()

    /**
     * `x++` -> `x = x.inc()`
     * `x = x++` -> `x = { val <unary> = x; x = <unary>.inc(); <unary> }`
     */
    sealed class DesugaredIncrementOrDecrement : CjFakeSourceElementKind()
    object DesugaredPrefixInc : DesugaredIncrementOrDecrement()
    object DesugaredPrefixDec : DesugaredIncrementOrDecrement()
    object DesugaredPostfixInc : DesugaredIncrementOrDecrement()
    object DesugaredPostfixDec : DesugaredIncrementOrDecrement()

    /**
     * In `++a[1]`, `a.get(1)` will be called twice. This kind is used for the second call reference.
     */
    sealed class DesugaredPrefixSecondGetReference : CjFakeSourceElementKind()
    object DesugaredPrefixIncSecondGetReference : DesugaredPrefixSecondGetReference()
    object DesugaredPrefixDecSecondGetReference : DesugaredPrefixSecondGetReference()

    /**
     * `x !in list` --> `!(x in list)` where `!` and `!(x in list)` will have a fake source
     */
    object DesugaredInvertedContains : CjFakeSourceElementKind()

    /**
     * For data classes, fir generates componentN() & copy() functions.
     * For componentN() functions, the source will refer to the corresponding param and will be marked as a fake one.
     * For copy() functions, the source will refer class to the param and will be marked as a fake one.
     */
    object DataClassGeneratedMembers : CjFakeSourceElementKind(shouldSkipErrorTypeReporting = true)

    /**
     * For synthetic overrides implemented by delegation
     */
    object MembersImplementedByDelegation : CjFakeSourceElementKind()

    /**
     * `(vararg x: Int)` --> `(x: Array<out Int>)` where array type ref has a fake source kind
     */
    object ArrayTypeFromVarargParameter : CjFakeSourceElementKind()

    sealed class DestructuringInitializer : CjFakeSourceElementKind()

    /**
     * `val (a,b) = x` --> `val a = x.component1(); val b = x.component2()`
     * where componentN calls will have the fake source elements refer to the corresponding KtDestructuringDeclarationEntry
     */
    object DesugaredComponentFunctionCall : DestructuringInitializer()

    /**
     * `(val a, val bb = b) = x` --> `val a = x.a; val bb = x.b`
     * where property accesses a and b will have the fake source elements refer to the corresponding KtDestructuringDeclarationEntry
     */
    object DesugaredNameBasedDestructuring : DestructuringInitializer()

    /**
     * when smart casts applied to the expression, it is wrapped into FirSmartCastExpression
     * which type reference will have a fake source refer to a original source element of it
     */
    object SmartCastedTypeRef : CjFakeSourceElementKind(shouldSkipErrorTypeReporting = true)

    /**
     * when smart casts applied to the expression, it is wrapped into FirSmartCastExpression
     * this kind used for such FirSmartCastExpressions itself
     */
    object SmartCastExpression : CjFakeSourceElementKind()

    /**
     * for safe call expressions like a?.foo() the FirSafeCallExpression is generated
     * and it have a fake source
     */
    object DesugaredSafeCallExpression : CjFakeSourceElementKind()

    /**
     * `a > b` will be wrapped in FirComparisonExpression
     * with real source which points to initial `a > b` expression
     * and inner FirFunctionCall will refer to a fake source
     */
    object GeneratedComparisonExpression : CjFakeSourceElementKind()

    /**
     * `a ?: b` --> `when(val $subj = a) { .... }`
     * where `val $subj = a` has a fake source
     */
    object WhenGeneratedSubject : CjFakeSourceElementKind()

    /**
     * `list[0]` -> `list.get(0)` where name reference will have a fake source element
     */
    object ArrayAccessNameReference : CjFakeSourceElementKind()

    /**
     * `a += b` -> `a = a + b` or `a.plusAssign(b)`
     * `=`, `+`, and `plusAssign` will have a fake source element
     */
    sealed class DesugaredAugmentedAssign : CjFakeSourceElementKind()
    object DesugaredPlusAssign : DesugaredAugmentedAssign()
    object DesugaredMinusAssign : DesugaredAugmentedAssign()
    object DesugaredTimesAssign : DesugaredAugmentedAssign()
    object DesugaredDivAssign : DesugaredAugmentedAssign()
    object DesugaredRemAssign : DesugaredAugmentedAssign()

    object AssignmentPluginAltered : CjFakeSourceElementKind()

    /**
     * `a[b]++`
     * `b` -> `val <index0> = b` where `b` will have fake property
     */
    object ArrayIndexExpressionReference : CjFakeSourceElementKind()

    /**
     * `super.foo()` --> `super<Supertype>.foo()`
     * where `Supertype` has a fake source
     */
    object SuperCallImplicitType : CjFakeSourceElementKind()

    /**
     * `fun foo(vararg args: Int) {}`
     * `fun bar(1, 2, 3)` --> (resolved) `fun bar(VarargArgument(1, 2, 3))`
     */
    object VarargArgument : CjFakeSourceElementKind()

    /**
     * Part of desugared x?.y
     */
    object CheckedSafeCallSubject : CjFakeSourceElementKind()

    /**
     * `{ it + 1 }` --> `{ it -> it + 1 }`
     * where `it` parameter declaration has fake source
     */
    object ItLambdaParameter : CjFakeSourceElementKind()

    /**
     * For function type `context(Foo) () -> Unit`,
     * the context parameter with type `Foo` of the anonymous function.
     */
    object LambdaContextParameter : CjFakeSourceElementKind()

    /**
     * While it doesn't have an explicit source, it still has a type that might be a ConeErrorType
     */
    object LambdaReceiver : CjFakeSourceElementKind()

    /**
     * Example:
     *
     * ```kotlin
     * fun foo() {
     *     val (a, b) = listOf(1, 2)
     * }
     * ```
     *
     * When constructing the FIR for a destructuring declaration, we initially create an `FirBlock`
     * containing the properties `<destruct>`, `a`, and `b`.
     * If the original PSI is well-formed, this block is discarded,
     * and the properties are added to the outer block (i.e., to the function body).
     * However, if the PSI is invalid, this synthetic block may persist in the FIR tree.
     */
    object DestructuringBlock : CjFakeSourceElementKind()

    /**
     * 模式绑定变量是语义层从 pattern 中投影出的独立 declaration 视图。
     *
     * 它依然锚定原始 pattern PSI，便于导航与诊断，
     * 但不应再被当成“源码里直接存在的独立声明节点”。
     */
    object PatternBindingVariable : CjFakeSourceElementKind()

    /**
     * `{ (a, b) -> foo() }` -> `{ x -> val (a, b) = x; { foo() } }`
     * where the inner block `{ foo() }` has fake source
     */
    object LambdaDestructuringBlock : CjFakeSourceElementKind()

    /**
     * for java annotations implicit constructor is generated
     * with a fake source which refers to containing class
     */
    object ImplicitJavaAnnotationConstructor : CjFakeSourceElementKind()

    /**
     * for FIR elements from Java enhancement
     */
    object Enhancement : CjFakeSourceElementKind()

    /**
     * for java annotations constructor implicit parameters are generated
     * with a fake source which refers to declared annotation methods
     */
    object ImplicitAnnotationAnnotationConstructorParameter : CjFakeSourceElementKind()

    /**
     * for java records implicit constructor is generated
     * with a fake source which refers to containing class
     */
    object ImplicitJavaRecordConstructor : CjFakeSourceElementKind()

    /**
     * for java record constructor implicit parameters are generated
     * with a fake source which refers to declared record components
     */
    object ImplicitRecordConstructorParameter : CjFakeSourceElementKind()

    /**
     * for java records implicit component functions are generated
     * with a fake source which refers to corresponding component
     */
    object JavaRecordComponentFunction : CjFakeSourceElementKind()

    /**
     * for java records implicit component fields are generated
     * with a fake source which refers to corresponding component
     */
    object JavaRecordComponentField : CjFakeSourceElementKind()

    /**
     * for the implicit field storing the delegated object for class delegation
     * with a fake source that refers to the KtExpression that creates the delegate
     */
    object ClassDelegationField : CjFakeSourceElementKind()

    /**
     * for annotation moved to another element due to annotation use-site target
     */
    object FromUseSiteTarget : CjFakeSourceElementKind()

    /**
     * for `@ParameterName` annotation call added to function types with names in the notation
     * with a fake source that refers to the value parameter in the function type notation
     * e.g., `(x: Int) -> Unit` becomes `Function1<@ParameterName("x") Int, Unit>`
     */
    object ParameterNameAnnotationCall : CjFakeSourceElementKind()

    /**
     * for implicit conversion from int to long with `.toLong` function
     * e.g. val x: Long = 1 + 1 becomes val x: Long = (1 + 1).toLong()
     */
    object IntToLongConversion : CjFakeSourceElementKind()

    /**
     * for extension receiver type the corresponding receiver parameter is generated
     * with a fake sources which refers to this the type
     */
    object ReceiverFromType : CjFakeSourceElementKind()

    /**
     * for all implicit receivers (now used for qualifiers only)
     */
    object ImplicitReceiver : CjFakeSourceElementKind()

    /**
     * for when on the LHS of an assignment an error expression appears
     */
    object AssignmentLValueError : CjFakeSourceElementKind()

    /**
     * For when the LHS of a desugared assignment has a null source.
     * In this case, the psi of [KtFakePsiSourceElement] should be set to the psi of the assignment
     */
    object DesugaredAssignmentLValueSourceIsNull : CjFakeSourceElementKind()

    /**
     * for return type of value parameters in lambdas
     */
    object ImplicitReturnTypeOfLambdaValueParameter : CjFakeSourceElementKind()

    /**
     * Synthetic calls for if/when/try/etc.
     */
    object SyntheticCall : CjFakeSourceElementKind()

    /**
     * When property doesn't have an initializer and explicit return type, but its getter's return type is specified
     */
    object PropertyTypeFromGetterReturnType : CjFakeSourceElementKind()

    /**
     * Scripts get implicit imports from their configurations
     */
    object ImplicitImport : CjFakeSourceElementKind(shouldSkipErrorTypeReporting = true)

    /**
     * For provided parameters inside a script
     */
    object ScriptParameter : CjFakeSourceElementKind()

    /**
     * For script base class
     */
    object ScriptBaseClass : CjFakeSourceElementKind(shouldSkipErrorTypeReporting = true)

    /**
     * For repl base class.
     */
    object ReplBaseClass : CjFakeSourceElementKind(shouldSkipErrorTypeReporting = true)

    /**
     * For repl eval function.
     */
    object ReplEvalFunction : CjFakeSourceElementKind(shouldSkipErrorTypeReporting = true)

    /**
     * For repl result field.
     */
    object ReplResultField : CjFakeSourceElementKind(shouldSkipErrorTypeReporting = true)

    /**
     * When a lambda is converted to a SAM type, the expression is wrapped in an extra node
     */
    object SamConversion : CjFakeSourceElementKind()

    /**
     * For synthetic functions created for SAM constructors.
     */
    object SamConstructor : CjFakeSourceElementKind()

    /**
     * For it.functionFromAny() calls on a stub type
     */
    object CastToAnyForStubTypes : CjFakeSourceElementKind()

    /**
     * We use the whole context parameter as the fake source for default values.
     */
    object ContextParameterDefaultValue : CjFakeSourceElementKind()

    /**
     * For plugin-generated things
     */
    object PluginGenerated : CjFakeSourceElementKind()

    /**
     * To store diagnostic for erroneously resolved `arrayOf` which is being transformed to array literal.
     * Note that this may happen both with original `arrayOf` and with synthetic `arrayOf` itself created to resolve array literal.
     */
    object ErrorExpressionForTransformedArrayOf : CjFakeSourceElementKind()

    /**
     * To store some diagnostic for erroneously resolved top-level lambda
     * See [org.jetbrains.kotlin.config.LanguageFeature.ResolveTopLevelLambdasAsSyntheticCallArgument] and its usages
     */
    object ErrorExpressionForTopLevelLambda : CjFakeSourceElementKind()

    /**
     * To store diagnostics for erroneously resolved top-level collection literals.
     */
    object ErrorExpressionForTopLevelCollectionLiteral : CjFakeSourceElementKind()

    /**
     * Arbitrary error expression for which we failed to build the real PSI.
     */
    object ErrorExpression : CjFakeSourceElementKind()

    /**
     * When resolving ENTRY as `MyEnum.ENTRY` this is used for the `MyEnum` part
     */
    object QualifierForContextSensitiveResolution : CjFakeSourceElementKind()

    /**
     * When resolving a collection literal, a collection package qualifier or the explicit companion object added to the call as the explicit receiver.
     */
    object DesugaredReceiverForOperatorOfCall : CjFakeSourceElementKind()

    /**
     * When resolving a collection literal, this is used as a source for the generated callee reference.
     */
    object CalleeReferenceForOperatorOfCall : CjFakeSourceElementKind()
}

sealed class AbstractCjSourceElement {
    abstract val startOffset: Int
    abstract val endOffset: Int

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractCjSourceElement) return false
        if (startOffset != other.startOffset) return false
        if (endOffset != other.endOffset) return false
        return true
    }

    override fun hashCode(): Int {
        var result = startOffset
        result = 31 * result + endOffset
        return result
    }
}

class CjOffsetsOnlySourceElement(
    override val startOffset: Int,
    override val endOffset: Int,
) : AbstractCjSourceElement()

sealed class CjSourceElement : AbstractCjSourceElement() {
    abstract val elementType: IElementType?
    abstract val kind: CjSourceElementKind
    abstract val lighterASTNode: LighterASTNode
    abstract val treeStructure: FlyweightCapableTreeStructure<LighterASTNode>

    abstract fun getElementTextInContextForDebug(): String

    abstract override fun hashCode(): Int
    abstract override fun equals(other: Any?): Boolean
}

sealed class CjPsiSourceElement(
    val psi: PsiElement,
) : CjSourceElement() {
    companion object {
        @JvmStatic
        private val lighterASTNodeUpdater = AtomicReferenceFieldUpdater.newUpdater(
            CjPsiSourceElement::class.java,
            LighterASTNode::class.java,
            "_lighterASTNode",
        )

        @JvmStatic
        private val treeStructureNodeUpdater = AtomicReferenceFieldUpdater.newUpdater(
            CjPsiSourceElement::class.java,
            FlyweightCapableTreeStructure::class.java,
            "_treeStructure",
        )
    }

    override val elementType: IElementType?
        get() = psi.node?.elementType

    override val startOffset: Int
        get() = psi.textRange.startOffset

    override val endOffset: Int
        get() = psi.textRange.endOffset

    @Volatile
    private var _lighterASTNode: LighterASTNode? = null
    final override val lighterASTNode: LighterASTNode
        get() {
            _lighterASTNode?.let { return it }
            lighterASTNodeUpdater.compareAndSet(
                this,
                null,
                TreeBackedLighterAST.wrap(psi.node),
            )
            return _lighterASTNode!!
        }

    @Volatile
    private var _treeStructure: FlyweightCapableTreeStructure<LighterASTNode>? = null
    final override val treeStructure: FlyweightCapableTreeStructure<LighterASTNode>
        get() {
            _treeStructure?.let { return it }
            treeStructureNodeUpdater.compareAndSet(
                this,
                null,
                WrappedTreeStructure(psi.containingFile),
            )
            return _treeStructure!!
        }

    override fun getElementTextInContextForDebug(): String = getElementTextWithContext(psi)

    internal class WrappedTreeStructure(file: PsiFile) : FlyweightCapableTreeStructure<LighterASTNode> {
        private val lighterAST = TreeBackedLighterAST(file.node)

        fun unwrap(node: LighterASTNode) = lighterAST.unwrap(node)

        override fun toString(node: LighterASTNode): CharSequence = unwrap(node).text

        override fun getRoot(): LighterASTNode = lighterAST.root

        override fun getParent(node: LighterASTNode): LighterASTNode? =
            unwrap(node).psi.parent?.node?.let { TreeBackedLighterAST.wrap(it) }

        override fun getChildren(node: LighterASTNode, nodesRef: Ref<Array<LighterASTNode>>): Int {
            val psi = unwrap(node).psi
            val children = mutableListOf<PsiElement>()
            var child = psi.firstChild
            while (child != null) {
                children += child
                child = child.nextSibling
            }
            if (children.isEmpty()) {
                nodesRef.set(LighterASTNode.EMPTY_ARRAY)
            } else {
                nodesRef.set(children.map { TreeBackedLighterAST.wrap(it.node) }.toTypedArray())
            }
            return children.size
        }

        override fun disposeChildren(children: Array<out LighterASTNode>?, count: Int) {}

        override fun getStartOffset(node: LighterASTNode): Int = getStartOffset(unwrap(node).psi)

        private fun getStartOffset(element: PsiElement): Int {
            var child = element.firstChild
            if (child != null) {
                while (child is PsiComment || child is PsiWhiteSpace) {
                    child = child.nextSibling
                }
                if (child != null) {
                    return getStartOffset(child)
                }
            }
            return element.textRange.startOffset
        }

        override fun getEndOffset(node: LighterASTNode): Int = getEndOffset(unwrap(node).psi)

        private fun getEndOffset(element: PsiElement): Int {
            var child = element.lastChild
            if (child != null) {
                while (child is PsiComment || child is PsiWhiteSpace) {
                    child = child.prevSibling
                }
                if (child != null) {
                    return getEndOffset(child)
                }
            }
            return element.textRange.endOffset
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CjPsiSourceElement
        return psi == other.psi
    }

    override fun hashCode(): Int = psi.hashCode()
}

class CjRealPsiSourceElement(psi: PsiElement) : CjPsiSourceElement(psi) {
    override val kind: CjSourceElementKind
        get() = CjRealSourceElementKind
}

@RequiresOptIn
annotation class SuspiciousFakeSourceCheck

@SuspiciousFakeSourceCheck
open class CjFakePsiSourceElement(
    psi: PsiElement,
    override val kind: CjFakeSourceElementKind,
) : CjPsiSourceElement(psi) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false
        other as CjFakePsiSourceElement
        return kind == other.kind
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + kind.hashCode()
        return result
    }
}

@SuspiciousFakeSourceCheck
class CjFakePsiSourceElementWithCustomOffsetStrategy(
    psi: PsiElement,
    kind: CjFakeSourceElementKind,
    val strategy: CjSourceElementOffsetStrategy.Custom,
) : CjFakePsiSourceElement(psi, kind) {
    override val startOffset: Int
        get() = strategy.startOffset

    override val endOffset: Int
        get() = strategy.endOffset

    override fun equals(other: Any?): Boolean = this === other ||
            other is CjFakePsiSourceElementWithCustomOffsetStrategy &&
            super.equals(other) &&
            strategy == other.strategy

    override fun hashCode(): Int = 31 * super.hashCode() + strategy.hashCode()
}

sealed class CjSourceElementOffsetStrategy {
    data object Default : CjSourceElementOffsetStrategy()

    sealed class Custom : CjSourceElementOffsetStrategy() {
        abstract val startOffset: Int
        abstract val endOffset: Int

        class Initialized(
            override val startOffset: Int,
            override val endOffset: Int,
        ) : Custom() {
            override fun equals(other: Any?): Boolean = this === other ||
                    other is Initialized &&
                    startOffset == other.startOffset &&
                    endOffset == other.endOffset

            override fun hashCode(): Int = 31 * startOffset.hashCode() + endOffset.hashCode()
        }

        class Delegated(
            val startOffsetAnchor: CjSourceElement,
            val endOffsetAnchor: CjSourceElement,
        ) : Custom() {
            override val startOffset: Int
                get() = startOffsetAnchor.startOffset

            override val endOffset: Int
                get() = endOffsetAnchor.endOffset

            override fun equals(other: Any?): Boolean = this === other ||
                    other is Delegated &&
                    startOffsetAnchor == other.startOffsetAnchor &&
                    endOffsetAnchor == other.endOffsetAnchor

            override fun hashCode(): Int = 31 * startOffsetAnchor.hashCode() + endOffsetAnchor.hashCode()
        }
    }
}

fun CjSourceElement.fakeElement(
    newKind: CjFakeSourceElementKind,
    offsetStrategy: CjSourceElementOffsetStrategy = CjSourceElementOffsetStrategy.Default,
): CjSourceElement {
    if (kind == newKind) return this
    return when (this) {
        is CjBinarySourceElement -> this
        is CjLightSourceElement -> {
            val (startOffset, endOffset) = if (offsetStrategy is CjSourceElementOffsetStrategy.Custom) {
                offsetStrategy.startOffset to offsetStrategy.endOffset
            } else {
                startOffset to endOffset
            }
            CjLightSourceElement(lighterASTNode, startOffset, endOffset, treeStructure, newKind)
        }

        is CjPsiSourceElement -> when (offsetStrategy) {
            is CjSourceElementOffsetStrategy.Default -> CjFakePsiSourceElement(psi, newKind)
            is CjSourceElementOffsetStrategy.Custom -> CjFakePsiSourceElementWithCustomOffsetStrategy(psi, newKind, offsetStrategy)
        }
    }
}

fun CjSourceElement.realElement(): CjSourceElement = when (this) {
    is CjBinarySourceElement -> this
    is CjRealPsiSourceElement -> this
    is CjLightSourceElement -> CjLightSourceElement(lighterASTNode, startOffset, endOffset, treeStructure, CjRealSourceElementKind)
    is CjPsiSourceElement -> CjRealPsiSourceElement(psi)
}

class CjLightSourceElement(
    override val lighterASTNode: LighterASTNode,
    override val startOffset: Int,
    override val endOffset: Int,
    override val treeStructure: FlyweightCapableTreeStructure<LighterASTNode>,
    override val kind: CjSourceElementKind = CjRealSourceElementKind,
) : CjSourceElement() {
    override val elementType: IElementType
        get() = lighterASTNode.tokenType

    fun unwrapToCjPsiSourceElement(): CjPsiSourceElement? {
        if (treeStructure !is CjPsiSourceElement.WrappedTreeStructure) return null
        val node = treeStructure.unwrap(lighterASTNode)
        return node.psi?.toCjPsiSourceElement(kind)
    }

    override fun getElementTextInContextForDebug(): String = treeStructure.toString(lighterASTNode).toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CjLightSourceElement
        if (lighterASTNode != other.lighterASTNode) return false
        if (startOffset != other.startOffset) return false
        if (endOffset != other.endOffset) return false
        if (treeStructure != other.treeStructure) return false
        if (kind != other.kind) return false
        return true
    }

    override fun hashCode(): Int {
        var result = lighterASTNode.hashCode()
        result = 31 * result + startOffset
        result = 31 * result + endOffset
        result = 31 * result + treeStructure.hashCode()
        result = 31 * result + kind.hashCode()
        return result
    }
}

val AbstractCjSourceElement?.psi: PsiElement?
    get() = (this as? CjPsiSourceElement)?.psi

val CjSourceElement?.text: CharSequence?
    get() = when (this) {
        is CjBinarySourceElement -> getElementTextInContextForDebug()
        is CjPsiSourceElement -> psi.text
        is CjLightSourceElement -> treeStructure.toString(lighterASTNode)
        else -> null
    }

@Suppress("NOTHING_TO_INLINE")
inline fun PsiElement.toCjPsiSourceElement(
    kind: CjSourceElementKind = CjRealSourceElementKind,
): CjPsiSourceElement = when (kind) {
    is CjRealSourceElementKind -> CjRealPsiSourceElement(this)
    is CjFakeSourceElementKind -> CjFakePsiSourceElement(this, kind)
}

@Suppress("NOTHING_TO_INLINE")
inline fun LighterASTNode.toCjLightSourceElement(
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    kind: CjSourceElementKind = CjRealSourceElementKind,
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
): CjLightSourceElement = CjLightSourceElement(this, startOffset, endOffset, tree, kind)

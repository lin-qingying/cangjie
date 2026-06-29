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

/**
 * 源元素种类的根分类。
 *
 * 真实源元素直接对应用户源码，fake 源元素对应前端脱糖、隐式声明或错误恢复过程中合成出的语义节点。
 */
sealed class CjSourceElementKind {
    /**
     * 是否跳过由错误类型引发的二次诊断上报。
     */
    abstract val shouldSkipErrorTypeReporting: Boolean
}

/**
 * 真实源码元素种类，表示语义节点与用户源码位置一一对应。
 */
object CjRealSourceElementKind : CjSourceElementKind() {
    /**
     * 真实源码元素需要正常参与错误类型诊断上报。
     */
    override val shouldSkipErrorTypeReporting: Boolean
        get() = false
}

/**
 * 合成源码元素种类的基类。
 *
 * fake source 保留原始源码锚点，但声明该语义节点是由前端转换、补全或错误恢复生成的。
 */
sealed class CjFakeSourceElementKind(
    /**
     * 该 fake source 是否应屏蔽错误类型传播导致的级联诊断。
     */
    final override val shouldSkipErrorTypeReporting: Boolean = false,
) : CjSourceElementKind() {
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
        /**
         * 表达式函数体生成隐式 return 语句时使用的 fake source。
         */
        object FromExpressionBody : ImplicitReturn()

        /**
         * 代码块最后一条语句生成隐式 return 语句时使用的 fake source。
         */
        object FromLastStatement : ImplicitReturn()
    }

    /**
     * 表示前端为无值返回或语句表达式补出的隐式 Unit。
     */
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

    /**
     * 隐式 invoke 调用生成的 fake source。
     */
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
    /**
     * 前缀自增 `++x` 脱糖生成节点的 fake source。
     */
    object DesugaredPrefixInc : DesugaredIncrementOrDecrement()
    /**
     * 前缀自减 `--x` 脱糖生成节点的 fake source。
     */
    object DesugaredPrefixDec : DesugaredIncrementOrDecrement()
    /**
     * 后缀自增 `x++` 脱糖生成节点的 fake source。
     */
    object DesugaredPostfixInc : DesugaredIncrementOrDecrement()
    /**
     * 后缀自减 `x--` 脱糖生成节点的 fake source。
     */
    object DesugaredPostfixDec : DesugaredIncrementOrDecrement()

    /**
     * In `++a[1]`, `a.get(1)` will be called twice. This kind is used for the second call reference.
     */
    sealed class DesugaredPrefixSecondGetReference : CjFakeSourceElementKind()
    /**
     * 前缀自增数组访问中第二次 get 调用引用的 fake source。
     */
    object DesugaredPrefixIncSecondGetReference : DesugaredPrefixSecondGetReference()
    /**
     * 前缀自减数组访问中第二次 get 调用引用的 fake source。
     */
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

    /**
     * 解构声明脱糖过程中生成节点的 fake source 基类。
     */
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
    /**
     * `+=` 脱糖生成节点的 fake source。
     */
    object DesugaredPlusAssign : DesugaredAugmentedAssign()
    /**
     * `-=` 脱糖生成节点的 fake source。
     */
    object DesugaredMinusAssign : DesugaredAugmentedAssign()
    /**
     * `*=` 脱糖生成节点的 fake source。
     */
    object DesugaredTimesAssign : DesugaredAugmentedAssign()
    /**
     * `/=` 脱糖生成节点的 fake source。
     */
    object DesugaredDivAssign : DesugaredAugmentedAssign()
    /**
     * `%=` 脱糖生成节点的 fake source。
     */
    object DesugaredRemAssign : DesugaredAugmentedAssign()

    /**
     * 赋值插件改写后生成节点的 fake source。
     */
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
     * Implicit imports supplied by the language configuration.
     */
    object ImplicitImport : CjFakeSourceElementKind(shouldSkipErrorTypeReporting = true)

    /**
     * 引用已失败 import 的包限定符使用点。
     *
     * import 声明本身负责报告解析失败；使用点只保留错误接收者形状，
     * 用于阻断后续成员访问级联诊断。
     */
    object UnresolvedImportQualifier : CjFakeSourceElementKind(shouldSkipErrorTypeReporting = true)

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

/**
 * 仓颉源码元素的最小抽象。
 *
 * 该层只承载文本区间，供诊断排序、范围映射和不依赖具体语法树的源码定位使用。
 */
sealed class AbstractCjSourceElement {
    /**
     * 源元素在文件文本中的起始偏移。
     */
    abstract val startOffset: Int
    /**
     * 源元素在文件文本中的结束偏移。
     */
    abstract val endOffset: Int

    /**
     * 按源码区间判断两个抽象源元素是否相等。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractCjSourceElement) return false
        if (startOffset != other.startOffset) return false
        if (endOffset != other.endOffset) return false
        return true
    }

    /**
     * 返回基于源码起止偏移的哈希值。
     */
    override fun hashCode(): Int {
        var result = startOffset
        result = 31 * result + endOffset
        return result
    }
}

/**
 * 仅包含偏移范围、不绑定 PSI 或 LightTree 节点的源码元素。
 */
class CjOffsetsOnlySourceElement(
    /**
     * 源元素在文件文本中的起始偏移。
     */
    override val startOffset: Int,
    /**
     * 源元素在文件文本中的结束偏移。
     */
    override val endOffset: Int,
) : AbstractCjSourceElement()

/**
 * 绑定到具体语法结构的仓颉源码元素。
 *
 * 该层统一暴露元素类型、源码种类、轻量树节点和调试文本。
 */
sealed class CjSourceElement : AbstractCjSourceElement() {
    /**
     * 源元素对应的语法元素类型；无法取得时为 null。
     */
    abstract val elementType: IElementType?
    /**
     * 当前源元素是真实源码还是某种 fake source。
     */
    abstract val kind: CjSourceElementKind
    /**
     * 当前源元素对应的轻量树节点。
     */
    abstract val lighterASTNode: LighterASTNode
    /**
     * 轻量树节点所属的树结构。
     */
    abstract val treeStructure: FlyweightCapableTreeStructure<LighterASTNode>

    /**
     * 返回带上下文的调试文本，用于诊断和异常消息。
     */
    abstract fun getElementTextInContextForDebug(): String

    /**
     * 具体源元素必须按自身锚点实现稳定哈希。
     */
    abstract override fun hashCode(): Int
    /**
     * 具体源元素必须按自身锚点实现相等性。
     */
    abstract override fun equals(other: Any?): Boolean
}

/**
 * 基于 IntelliJ PSI 的仓颉源码元素。
 */
sealed class CjPsiSourceElement(
    /**
     * 当前源码元素锚定的 PSI 元素。
     */
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

    /**
     * PSI 节点的元素类型。
     */
    override val elementType: IElementType?
        get() = psi.node?.elementType

    /**
     * PSI 文本范围的起始偏移。
     */
    override val startOffset: Int
        get() = psi.textRange.startOffset

    /**
     * PSI 文本范围的结束偏移。
     */
    override val endOffset: Int
        get() = psi.textRange.endOffset

    /**
     * 延迟构造并缓存的轻量树节点包装。
     */
    @Volatile
    private var _lighterASTNode: LighterASTNode? = null
    /**
     * 当前 PSI 元素对应的轻量树节点。
     */
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

    /**
     * 延迟构造并缓存的 PSI 包装树结构。
     */
    @Volatile
    private var _treeStructure: FlyweightCapableTreeStructure<LighterASTNode>? = null
    /**
     * 当前 PSI 文件的轻量树结构视图。
     */
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

    /**
     * 返回当前 PSI 元素及其上下文文本。
     */
    override fun getElementTextInContextForDebug(): String = getElementTextWithContext(psi)

    /**
     * 将 PSI 文件包装成 LightTree 接口所需的树结构。
     */
    internal class WrappedTreeStructure(file: PsiFile) : FlyweightCapableTreeStructure<LighterASTNode> {
        /**
         * 基于 PSI 文件节点创建的轻量树适配器。
         */
        private val lighterAST = TreeBackedLighterAST(file.node)

        /**
         * 将轻量树节点还原为底层 PSI AST 节点包装。
         */
        fun unwrap(node: LighterASTNode) = lighterAST.unwrap(node)

        /**
         * 返回节点对应 PSI 的文本。
         */
        override fun toString(node: LighterASTNode): CharSequence = unwrap(node).text

        /**
         * 返回当前文件轻量树根节点。
         */
        override fun getRoot(): LighterASTNode = lighterAST.root

        /**
         * 返回节点父级的轻量树包装。
         */
        override fun getParent(node: LighterASTNode): LighterASTNode? =
            unwrap(node).psi.parent?.node?.let { TreeBackedLighterAST.wrap(it) }

        /**
         * 收集节点的直接子 PSI 元素并转换为轻量树节点数组。
         */
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

        /**
         * PSI 包装树不持有需要释放的子节点资源。
         */
        override fun disposeChildren(children: Array<out LighterASTNode>?, count: Int) {}

        /**
         * 返回节点去除前导空白和注释后的起始偏移。
         */
        override fun getStartOffset(node: LighterASTNode): Int = getStartOffset(unwrap(node).psi)

        /**
         * 递归计算 PSI 元素的有效起始偏移。
         */
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

        /**
         * 返回节点去除尾随空白和注释后的结束偏移。
         */
        override fun getEndOffset(node: LighterASTNode): Int = getEndOffset(unwrap(node).psi)

        /**
         * 递归计算 PSI 元素的有效结束偏移。
         */
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

    /**
     * PSI 源元素按具体实现类型和底层 PSI 元素判断相等。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CjPsiSourceElement
        return psi == other.psi
    }

    /**
     * 返回底层 PSI 元素的哈希值。
     */
    override fun hashCode(): Int = psi.hashCode()
}

/**
 * 真实 PSI 源元素，表示语义节点直接锚定用户源码 PSI。
 */
class CjRealPsiSourceElement(psi: PsiElement) : CjPsiSourceElement(psi) {
    /**
     * 真实 PSI 源元素固定使用真实源元素种类。
     */
    override val kind: CjSourceElementKind
        get() = CjRealSourceElementKind
}

/**
 * 标记需要显式确认 fake source 使用点的 opt-in 注解。
 */
@RequiresOptIn
annotation class SuspiciousFakeSourceCheck

/**
 * 基于 PSI 的 fake source 元素。
 *
 * 它复用真实 PSI 作为锚点，但通过 [kind] 说明该语义节点来自隐式生成、脱糖或错误恢复。
 */
@SuspiciousFakeSourceCheck
open class CjFakePsiSourceElement(
    psi: PsiElement,
    /**
     * 当前 fake source 的具体来源种类。
     */
    override val kind: CjFakeSourceElementKind,
) : CjPsiSourceElement(psi) {
    /**
     * fake PSI 源元素按底层 PSI 和 fake kind 共同判断相等。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false
        other as CjFakePsiSourceElement
        return kind == other.kind
    }

    /**
     * 返回底层 PSI 哈希与 fake kind 哈希组合后的值。
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + kind.hashCode()
        return result
    }
}

/**
 * 使用自定义 offset 策略的 fake PSI 源元素。
 */
@SuspiciousFakeSourceCheck
class CjFakePsiSourceElementWithCustomOffsetStrategy(
    psi: PsiElement,
    kind: CjFakeSourceElementKind,
    /**
     * 覆盖默认 PSI 文本范围的自定义 offset 策略。
     */
    val strategy: CjSourceElementOffsetStrategy.Custom,
) : CjFakePsiSourceElement(psi, kind) {
    /**
     * 自定义策略给出的起始偏移。
     */
    override val startOffset: Int
        get() = strategy.startOffset

    /**
     * 自定义策略给出的结束偏移。
     */
    override val endOffset: Int
        get() = strategy.endOffset

    /**
     * 同时比较底层 fake PSI 元素和自定义 offset 策略。
     */
    override fun equals(other: Any?): Boolean = this === other ||
            other is CjFakePsiSourceElementWithCustomOffsetStrategy &&
            super.equals(other) &&
            strategy == other.strategy

    /**
     * 返回底层 fake PSI 哈希与策略哈希组合后的值。
     */
    override fun hashCode(): Int = 31 * super.hashCode() + strategy.hashCode()
}

/**
 * fake source 的 offset 计算策略。
 */
sealed class CjSourceElementOffsetStrategy {
    /**
     * 使用源元素自身默认文本范围。
     */
    data object Default : CjSourceElementOffsetStrategy()

    /**
     * 显式覆盖默认文本范围的 offset 策略基类。
     */
    sealed class Custom : CjSourceElementOffsetStrategy() {
        /**
         * 自定义起始偏移。
         */
        abstract val startOffset: Int
        /**
         * 自定义结束偏移。
         */
        abstract val endOffset: Int

        /**
         * 直接用固定起止偏移初始化的自定义策略。
         */
        class Initialized(
            /**
             * 固定起始偏移。
             */
            override val startOffset: Int,
            /**
             * 固定结束偏移。
             */
            override val endOffset: Int,
        ) : Custom() {
            /**
             * 按固定起止偏移判断策略相等。
             */
            override fun equals(other: Any?): Boolean = this === other ||
                    other is Initialized &&
                    startOffset == other.startOffset &&
                    endOffset == other.endOffset

            /**
             * 返回固定起止偏移组合后的哈希值。
             */
            override fun hashCode(): Int = 31 * startOffset.hashCode() + endOffset.hashCode()
        }

        /**
         * 从两个源元素锚点代理获取起止偏移的自定义策略。
         */
        class Delegated(
            /**
             * 提供起始偏移的源元素锚点。
             */
            val startOffsetAnchor: CjSourceElement,
            /**
             * 提供结束偏移的源元素锚点。
             */
            val endOffsetAnchor: CjSourceElement,
        ) : Custom() {
            /**
             * 起始锚点的起始偏移。
             */
            override val startOffset: Int
                get() = startOffsetAnchor.startOffset

            /**
             * 结束锚点的结束偏移。
             */
            override val endOffset: Int
                get() = endOffsetAnchor.endOffset

            /**
             * 按两个锚点判断代理策略相等。
             */
            override fun equals(other: Any?): Boolean = this === other ||
                    other is Delegated &&
                    startOffsetAnchor == other.startOffsetAnchor &&
                    endOffsetAnchor == other.endOffsetAnchor

            /**
             * 返回两个锚点哈希组合后的值。
             */
            override fun hashCode(): Int = 31 * startOffsetAnchor.hashCode() + endOffsetAnchor.hashCode()
        }
    }
}

/**
 * 基于当前源元素创建指定 fake kind 的源元素。
 *
 * 对 LightTree 和 PSI 源元素会保留原始锚点，对二进制源元素直接返回自身。
 */
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

/**
 * 将 fake 源元素还原为真实源元素视图。
 */
fun CjSourceElement.realElement(): CjSourceElement = when (this) {
    is CjBinarySourceElement -> this
    is CjRealPsiSourceElement -> this
    is CjLightSourceElement -> CjLightSourceElement(lighterASTNode, startOffset, endOffset, treeStructure, CjRealSourceElementKind)
    is CjPsiSourceElement -> CjRealPsiSourceElement(psi)
}

/**
 * 基于 LightTree 节点的仓颉源码元素。
 */
class CjLightSourceElement(
    /**
     * 当前源元素锚定的轻量树节点。
     */
    override val lighterASTNode: LighterASTNode,
    /**
     * 轻量树节点对应的起始偏移。
     */
    override val startOffset: Int,
    /**
     * 轻量树节点对应的结束偏移。
     */
    override val endOffset: Int,
    /**
     * 轻量树节点所属的树结构。
     */
    override val treeStructure: FlyweightCapableTreeStructure<LighterASTNode>,
    /**
     * 当前源元素是真实源码还是 fake source。
     */
    override val kind: CjSourceElementKind = CjRealSourceElementKind,
) : CjSourceElement() {
    /**
     * 轻量树节点的 token 类型。
     */
    override val elementType: IElementType
        get() = lighterASTNode.tokenType

    /**
     * 当轻量树来自 PSI 包装结构时，将当前节点还原为 PSI 源元素。
     */
    fun unwrapToCjPsiSourceElement(): CjPsiSourceElement? {
        if (treeStructure !is CjPsiSourceElement.WrappedTreeStructure) return null
        val node = treeStructure.unwrap(lighterASTNode)
        return node.psi?.toCjPsiSourceElement(kind)
    }

    /**
     * 返回轻量树节点对应的调试文本。
     */
    override fun getElementTextInContextForDebug(): String = treeStructure.toString(lighterASTNode).toString()

    /**
     * 按轻量树节点、范围、树结构和 kind 判断相等。
     */
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

    /**
     * 返回轻量树节点、范围、树结构和 kind 组合后的哈希值。
     */
    override fun hashCode(): Int {
        var result = lighterASTNode.hashCode()
        result = 31 * result + startOffset
        result = 31 * result + endOffset
        result = 31 * result + treeStructure.hashCode()
        result = 31 * result + kind.hashCode()
        return result
    }
}

/**
 * 若抽象源元素实际为 PSI 源元素，则返回其底层 PSI。
 */
val AbstractCjSourceElement?.psi: PsiElement?
    get() = (this as? CjPsiSourceElement)?.psi

/**
 * 返回源元素可直接取得的文本内容。
 */
val CjSourceElement?.text: CharSequence?
    get() = when (this) {
        is CjBinarySourceElement -> getElementTextInContextForDebug()
        is CjPsiSourceElement -> psi.text
        is CjLightSourceElement -> treeStructure.toString(lighterASTNode)
        else -> null
    }

/**
 * 将 PSI 元素包装为仓颉 PSI 源元素。
 */
@Suppress("NOTHING_TO_INLINE")
inline fun PsiElement.toCjPsiSourceElement(
    kind: CjSourceElementKind = CjRealSourceElementKind,
): CjPsiSourceElement = when (kind) {
    is CjRealSourceElementKind -> CjRealPsiSourceElement(this)
    is CjFakeSourceElementKind -> CjFakePsiSourceElement(this, kind)
}

/**
 * 将轻量树节点包装为仓颉 LightTree 源元素。
 */
@Suppress("NOTHING_TO_INLINE")
inline fun LighterASTNode.toCjLightSourceElement(
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    kind: CjSourceElementKind = CjRealSourceElementKind,
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
): CjLightSourceElement = CjLightSourceElement(this, startOffset, endOffset, tree, kind)

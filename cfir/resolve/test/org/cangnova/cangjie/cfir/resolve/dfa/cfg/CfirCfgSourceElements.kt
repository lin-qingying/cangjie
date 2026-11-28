package org.cangnova.cangjie.cfir.resolve.dfa.cfg

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import java.util.concurrent.atomic.AtomicInteger
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.source.CjRealSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.toCjPsiSourceElement
import org.cangnova.cangjie.test.util.CjTestUtil

/**
 * CFG 协议测试的真实语法 source。
 * 环境只负责解析；每次 factory 调用都产生独立 PSI 声明，不提供默认或伪 source。
 */
internal class CfirCfgSourceElements : AutoCloseable {
    private val disposable = Disposer.newDisposable("CFIR CFG source declarations")
    private val environment = CangJieCoreEnvironment.createForTests(disposable)
    private val sequence = AtomicInteger()

    data class Lambda(val declaration: CjSourceElement, val body: CjSourceElement)
    data class Resource(val declaration: CjSourceElement, val initializer: CjSourceElement)
    data class Fragment(val declaration: CjSourceElement, val body: CjSourceElement)

    /** 外层 lambda 与其真实 body 分别取 source；返回函数的测试使用合法嵌套 lambda。 */
    fun lambda(returnsFunction: Boolean = false): Lambda = read {
        val result = if (returnsFunction) "{ => () }" else "()"
        val file = file("func owner(): Unit { let value = { => " + result + " } }")
        val expression = PsiTreeUtil.findChildrenOfType(file, CjLambdaExpression::class.java).single {
            PsiTreeUtil.getParentOfType(it, CjLambdaExpression::class.java, true) == null
        }
        val body = checkNotNull(expression.bodyExpression)
        check(expression.node.elementType === CjNodeTypes.LAMBDA_EXPRESSION)
        check(body.node.elementType === CjNodeTypes.BLOCK)
        Lambda(source(expression), source(body))
    }

    /** 资源锚是 CjTryResource，不能退化为 parameter token 或 initializer call。 */
    fun resource(name: String): Resource = read {
        val file = file(
            "package test\n" +
                "class Resource <: std.core.Resource {\n" +
                "    public init() {}\n" +
                "    public func isClosed(): Bool { false }\n" +
                "    public func close(): Unit {}\n" +
                "}\n" +
                "func owner(): Unit { try (" + name + " = Resource()) {} }\n"
        )
        val resource = PsiTreeUtil.findChildrenOfType(file, CjTryResource::class.java).single()
        check(resource.name == name)
        check(resource.node.elementType === CjNodeTypes.TRY_RESOURCE)
        val initializer = checkNotNull(resource.expression)
        check(initializer is CjCallExpression)
        Resource(source(resource), source(initializer))
    }

    /** 字段必须来自 class 成员语法，不能使用顶层模式声明充当 field。 */
    fun field(): CjSourceElement = read {
        val file = file("package test\nclass Holder { let field: Unit = (); public init() {} }")
        val field = PsiTreeUtil.collectElements(file) { it.node?.elementType === CjNodeTypes.FIELD }.single()
        source(field)
    }

    /** 定位参数锚来自 owner 函数的真实 value parameter，保持原 isNamed=false。 */
    fun parameter(): CjSourceElement = read {
        val file = file("func owner(parameter: Unit): Unit {}")
        val function = file.declarations.filterIsInstance<CjNamedFunction>().single()
        val parameter = function.valueParameters.single()
        check(parameter.name == "parameter")
        check(parameter.node.elementType === CjNodeTypes.VALUE_PARAMETER)
        source(parameter)
    }

    /** CodeFragment 使用真实 fragment root 及 content block，不借用普通文件或 binary source。 */
    fun fragment(): Fragment = read {
        val contextFile = file("func owner(): Unit { () }")
        val context = checkNotNull(contextFile.declarations.filterIsInstance<CjNamedFunction>().single().bodyExpression)
        val fragment = CjPsiFactory(environment.project, markGenerated = false).createBlockCodeFragment("()", context)
        val block = fragment.getContentElement()
        assertParsed(fragment)
        check(fragment.node.elementType === CjNodeTypes.BLOCK_CODE_FRAGMENT)
        check(block.node.elementType === CjNodeTypes.BLOCK)
        Fragment(source(fragment), source(block))
    }

    private fun file(text: String): CjFile =
        CjTestUtil.createFile("cfgSource" + sequence.getAndIncrement() + ".cj", text, environment.project).also(::assertParsed)

    /** 解析错误必须阻断 fixture 构造，不能从错误树中选一个节点补足非空 guard。 */
    private fun assertParsed(root: PsiElement) {
        val errors = PsiTreeUtil.findChildrenOfType(root, PsiErrorElement::class.java)
        check(errors.isEmpty()) { errors.joinToString("\n") { it.errorDescription } }
    }

    private fun source(element: PsiElement): CjSourceElement =
        element.toCjPsiSourceElement(CjRealSourceElementKind)

    private fun <T> read(action: () -> T): T =
        ApplicationManager.getApplication().runReadAction(Computable { action() })

    /** 在整个测试类完成后关闭 headless project，保留所有 snapshot 引用到测试结束。 */
    override fun close() {
        ApplicationManager.getApplication().runWriteAction { Disposer.dispose(disposable) }
    }
}

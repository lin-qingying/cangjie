

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders

import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.providers.CfirCallableSignature
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile

/**
 * 悬空文件会话使用的依赖符号提供器包装层。
 *
 * 悬空文件可能同时看到多个库副本。该包装层把绝大多数查询委托给 [delegate]，并在顶层 callable 查询结果中去除
 * 二进制库重复候选，避免调试表达式、临时文件分析等场景把同签名库声明解析成不可编译的歧义调用。
 *
 * @property delegate 被包装的真实依赖符号提供器。
 */
class LLDanglingFileDependenciesSymbolProvider(private val delegate: CfirSymbolProvider) : CfirSymbolProvider(delegate.session) {
    /**
     * 依赖提供器的名称索引，保持与被包装 [delegate] 完全一致。
     */
    override val symbolNamesProvider: CfirSymbolNamesProvider
        get() = delegate.symbolNamesProvider

    /**
     * 从 [delegate] 查询 [classId] 对应的 class-like 符号。
     */
    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? {
        return delegate.getClassLikeSymbolByClassId(classId)
    }


    /**
     * 查询顶层 callable 符号并过滤同根二进制库中的重复候选。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<CfirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        destination += delegate.getTopLevelCallableSymbols(packageFqName, name).let(::filterSymbols)
    }


    /**
     * 查询顶层函数符号并过滤同签名库函数重复候选。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<CfirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        destination += delegate.getTopLevelFunctionSymbols(packageFqName, name).let(::filterSymbols)
    }


    /**
     * 查询顶层属性符号并过滤同签名库属性重复候选。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<CfirPropertySymbol>, packageFqName: FqName, name: Name) {
        destination += delegate.getTopLevelPropertySymbols(packageFqName, name).let(::filterSymbols)
    }

    /**
     * 使用 [delegate] 判断 [fqName] 包是否存在。
     */
    override fun hasPackage(fqName: FqName): Boolean {
        return delegate.hasPackage(fqName)
    }

    /**
     * 过滤顶层 callable 结果中的二进制库重复声明。
     *
     * 复杂工程中可能同时出现多个相同或不同版本的库副本。由于库之间缺少可靠依赖图，工程库会被视为依赖全部其他库，
     * classpath 中因此可能存在同名同签名的多个声明。普通解析可以报告歧义，但调试表达式等后续需要把 CFIR 交给后端的场景
     * 不能继续携带未完成的歧义调用。
     *
     * 该方法按 [CandidateSignature] 与二进制库根分组，只保留索引返回顺序中的第一个库根候选；非库声明原样保留。
     */
    private fun <T : CfirCallableSymbol<*>> filterSymbols(symbols: List<T>): List<T> {
        if (symbols.size < 2) {
            return symbols
        }

        val binarySymbols = LinkedHashMap<CandidateSignature, MutableMap<VirtualFile, MutableList<T>>>()
        val otherSymbols = ArrayList<T>()

        for (symbol in symbols) {
            if (symbol.callableId?.className == null) {
                val callableId = symbol.callableId

                val symbolFile = symbol.cfir.psi?.containingFile
                val symbolVirtualFile = symbolFile?.virtualFile
                if (symbolFile is CjFile && symbolFile.isCompiled && symbolVirtualFile != null) {
                    val symbolRootVirtualFile = getSymbolRootFile(symbolVirtualFile, symbolFile.packageFqName)
                    if (symbolRootVirtualFile != null) {
                        val key = CandidateSignature(callableId!!, CfirCallableSignature.createSignature(symbol))
                        binarySymbols
                            .getOrPut(key, ::LinkedHashMap)
                            .getOrPut(symbolRootVirtualFile, ::ArrayList)
                            .add(symbol)
                        continue
                    }
                }
            }

            otherSymbols.add(symbol)
        }

        if (binarySymbols.isNotEmpty()) {
            return buildList {
                addAll(otherSymbols)
                for (binarySymbolGroup in binarySymbols.values) {
                    // For consistency with class symbol fetching, callable symbols are returned in the same order as indices returned.
                    val firstBinarySymbolGroupValue = binarySymbolGroup.values.first()
                    if (firstBinarySymbolGroupValue.isNotEmpty()) {
                        addAll(firstBinarySymbolGroupValue)
                    }
                }
            }
        }

        return symbols
    }

    /**
     * 用于判断二进制库顶层 callable 是否重复的候选签名键。
     *
     * [callableId] 区分包级名称，[signature] 区分参数和返回形态，二者共同代表后端可见的 callable 身份。
     */
    private data class CandidateSignature(val callableId: CallableId, val signature: CfirCallableSignature)

    /**
     * 根据 [virtualFile] 和声明所在 [packageFqName] 反推出二进制库根目录。
     *
     * 当文件路径不符合包名目录结构时返回 `null`，表示该符号不能参与按库根去重。
     */
    private fun getSymbolRootFile(virtualFile: VirtualFile, packageFqName: FqName): VirtualFile? {
        val packageFqNameSegments = packageFqName.pathSegments().asReversed()
        val nestingLevel = packageFqNameSegments.size

        var current = virtualFile
        var index = 0

        while (true) {
            assert(index <= nestingLevel)

            val parent = current.parent ?: return null

            if (index == nestingLevel) {
                // Parent containing the root package is a class file root
                return parent
            }

            if (parent.name != packageFqNameSegments[index].asString()) {
                // Unexpected directory structure, the class is in a non-conventional root
                return null
            }

            current = parent
            index += 1
        }
    }
}

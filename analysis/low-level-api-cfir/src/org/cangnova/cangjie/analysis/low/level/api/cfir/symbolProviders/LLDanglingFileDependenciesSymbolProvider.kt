

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

class LLDanglingFileDependenciesSymbolProvider(private val delegate: CfirSymbolProvider) : CfirSymbolProvider(delegate.session) {
    override val symbolNamesProvider: CfirSymbolNamesProvider
        get() = delegate.symbolNamesProvider

    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? {
        return delegate.getClassLikeSymbolByClassId(classId)
    }


    @CfirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<CfirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        destination += delegate.getTopLevelCallableSymbols(packageFqName, name).let(::filterSymbols)
    }


    @CfirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<CfirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        destination += delegate.getTopLevelFunctionSymbols(packageFqName, name).let(::filterSymbols)
    }


    @CfirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<CfirPropertySymbol>, packageFqName: FqName, name: Name) {
        destination += delegate.getTopLevelPropertySymbols(packageFqName, name).let(::filterSymbols)
    }

    override fun hasPackage(fqName: FqName): Boolean {
        return delegate.hasPackage(fqName)
    }

    // In complex projects, there might be several library copies (with the same or different versions).
    // As there is no way to build a reliable dependency graph between libraries, a project library depends on all other libraries.
    // As a result, there might be several declarations in the classpath with the same name and signature.
    // Normally, K2 issues a 'resolution ambiguity' error on calls to such libraries. It is sort of acceptable for resolution, as
    // resolution errors are never shown in the library code. However, the backend, to which 'evaluate expression' needs to pass CFIR
    // afterwards, is not designed for compiling ambiguous (and non-completed) calls.
    // The code below scans for declaration duplicates, and chooses one from the first class input for each individual name and signature.
    // Non-library declarations are returned as is.
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

    private data class CandidateSignature(val callableId: CallableId, val signature: CfirCallableSignature)

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

package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.resolvedTypeFromPrototype
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.name.Name

/**
 * 模式绑定与控制结构直接分支体共享的声明空间。
 *
 * 官方在 PreCheck 中收集块内声明，随后 ChkVarOrEnumPattern 才按顺序引入延迟绑定。
 * 这里预收集名称用于冲突判定，不把尚未执行的局部声明发布到 tower；嵌套块与外层
 * 声明均不属于本空间，因此合法的遮蔽和 initializer 向外层查找保持原有顺序。
 */
internal class CfirPatternBindingScope(body: CfirBlock) {
    private val declarations = mutableMapOf<Name, MutableSet<CfirBasedSymbol<*>>>()

    init {
        for (statement in body.statements) {
            when (statement) {
                is CfirPatternVariable -> statement.pattern.bindingVariables().forEach(::record)
                is CfirPatternBindingVariable -> record(statement)
                is CfirFieldVariable -> record(statement.name, statement.symbol)
                is CfirProperty -> record(statement.name, statement.symbol)
                is CfirNamedFunction -> record(statement.name, statement.symbol)
                is CfirClassLikeDeclaration -> record(statement.name, statement.symbol)
                else -> Unit
            }
        }
    }

    fun conflictsWith(binding: CfirPatternBindingVariable): Boolean =
        declarations[binding.name].orEmpty().any { it !== binding.symbol }

    fun record(binding: CfirPatternBindingVariable) {
        record(binding.name, binding.symbol)
    }

    private fun record(name: Name, symbol: CfirBasedSymbol<*>) {
        declarations.getOrPut(name, ::linkedSetOf).add(symbol)
    }
}

/**
 * 按官方模式检查顺序发布 case / let-condition 绑定，并返回阻断表达式类型合成的错误。
 *
 * VarOrEnumPattern 延迟产生的绑定在冲突后会被 RemoveDeclByName 撤销；显式类型模式
 * 的绑定已在 PreCheck 中存在，冲突后仍保留名字及错误类型。tuple / enum 的 payload
 * 按顺序短路，保留先前成功的绑定；诊断仍由统一的局部声明冲突 checker 报告。
 */
internal fun CfirPartialBodyResolveTransformer.registerScopedPatternBindings(
    pattern: CfirPattern,
    bindingScope: CfirPatternBindingScope,
): ConeErrorType? {
    fun register(binding: CfirPatternBindingVariable?, retainOnConflict: Boolean): ConeErrorType? {
        if (binding == null) return null
        val errorType = if (bindingScope.conflictsWith(binding)) {
            ConeErrorType(ConeUnreportedDuplicateDiagnostic(ConeSimpleDiagnostic(
                "Conflicting pattern binding ${binding.name}",
            )))
        } else {
            null
        }
        if (errorType != null) {
            binding.replaceReturnTypeRef(binding.returnTypeRef.resolvedTypeFromPrototype(errorType, binding.returnTypeRef.source))
        }
        if (errorType == null || retainOnConflict) {
            bindingScope.record(binding)
            context.storeVariable(binding, session)
        }
        return errorType
    }

    return when (pattern) {
        is CfirBindingPattern -> register(pattern.bindingVariable, retainOnConflict = false)
            ?: pattern.nestedPattern?.let { registerScopedPatternBindings(it, bindingScope) }
        is CfirVarOrEnumPattern -> register(pattern.bindingVariable, retainOnConflict = false)
        is CfirTypePattern -> register(pattern.bindingVariable, retainOnConflict = true)
        is CfirTuplePattern -> pattern.elements.firstNotNullOfOrNull {
            registerScopedPatternBindings(it, bindingScope)
        }
        is CfirEnumPattern -> pattern.arguments.firstNotNullOfOrNull {
            registerScopedPatternBindings(it, bindingScope)
        }
        is CfirOrPattern -> {
            var firstError: ConeErrorType? = null
            for (binding in pattern.visibleBindingVariables()) {
                val error = register(binding, retainOnConflict = true)
                if (firstError == null) firstError = error
            }
            firstError
        }
        is CfirWildcardPattern, is CfirConstPattern, is CfirExpressionPattern -> null
    }
}

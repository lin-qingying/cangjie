package org.cangnova.cangjie.chir.core.api

import org.cangnova.cangjie.chir.core.builder.ChirBuilder
import org.cangnova.cangjie.chir.core.checker.ChirValidationReport
import org.cangnova.cangjie.chir.core.checker.ChirValidator
import org.cangnova.cangjie.chir.core.checker.DefaultChirValidator
import org.cangnova.cangjie.chir.core.context.ChirContext
import org.cangnova.cangjie.chir.core.model.ChirPackage

/**
 * Public facade for registering CHIR packages into a context and running validation.
 *
 * This API intentionally exposes only high-level contracts and keeps implementation details
 * inside the `core.*` subdomains.
 */
class ChirPublicApi(
    /**
     * 注册 CHIR 包使用的上下文。
     */
    private val context: ChirContext,

    /**
     * 写入上下文的构建器。
     */
    private val builder: ChirBuilder,

    /**
     * 注册后执行的校验器。
     */
    private val validator: ChirValidator = DefaultChirValidator(),
) {
    /**
     * Registers a CHIR package and returns the validation report produced on the accepted graph.
     */
    fun registerAndValidate(chirPackage: ChirPackage): ChirValidationReport {
        val accepted = builder.registerPackage(chirPackage)
        check(accepted) { "failed to register package ${chirPackage.name}" }
        return validator.validatePackage(chirPackage, context)
    }
}

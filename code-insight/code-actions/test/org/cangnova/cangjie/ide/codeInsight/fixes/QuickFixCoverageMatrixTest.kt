package org.cangnova.cangjie.ide.codeInsight.fixes

import org.cangnova.cangjie.analysis.api.cfir.diagnostics.CaCfirDiagnostic
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

/**
 * quick-fix 注册表覆盖矩阵校验（F5）。
 *
 * 防回退目标：
 * 1. 注册键必须全部是**具体** `CaDiagnosticWithPsi` 子接口——注册到泛型基类
 *    （`CaDiagnosticWithPsi::class`）会静默失效，builder 只打 error 日志；
 * 2. 每条「已承诺补齐」的诊断必须仍有一个 factory 注册着——删除注册会让
 *    本测试失败，CI 拦截覆盖率下降；
 * 3. 通道归属必须与设计一致：重修复在 lazy 通道、AddImport 在 import-on-the-fly 通道。
 */
class QuickFixCoverageMatrixTest {

    /** 修复所属的注册通道。 */
    private enum class Channel { LIST, LAZY, IMPORT_ON_THE_FLY }

    /**
     * 最低覆盖承诺集：P0/P1 交付的「诊断 → 通道」映射。
     *
     * 新增注册时应把对应键加入本集合；移除注册必须显式更新此表并说明原因。
     */
    private val promisedCoverage: List<Pair<KClass<out CaDiagnosticWithPsi<*>>, Channel>> = listOf(
        // ---- import 族（普通通道）----
        CaCfirDiagnostic.UnresolvedImport::class to Channel.LIST,
        CaCfirDiagnostic.UseExprWithoutImport::class to Channel.LIST,
        CaCfirDiagnostic.UnusedImport::class to Channel.LIST,
        // ---- 修饰符族（普通通道）----
        CaCfirDiagnostic.RepeatedModifier::class to Channel.LIST,
        CaCfirDiagnostic.RedundantModifier::class to Channel.LIST,
        CaCfirDiagnostic.IncompatibleModifiers::class to Channel.LIST,
        CaCfirDiagnostic.WrongModifierTarget::class to Channel.LIST,
        CaCfirDiagnostic.WrongModifierContainingDeclaration::class to Channel.LIST,
        CaCfirDiagnostic.RedundantModifierForTarget::class to Channel.LIST,
        CaCfirDiagnostic.DeprecatedModifierForTarget::class to Channel.LIST,
        // ---- match / 弃用族（普通通道）----
        CaCfirDiagnostic.NonExhaustiveMatch::class to Channel.LIST,
        CaCfirDiagnostic.DeprecatedWarning::class to Channel.LIST,
        // ---- 抽象成员族（lazy 通道）----
        CaCfirDiagnostic.AbstractMemberNotImplemented::class to Channel.LAZY,
        CaCfirDiagnostic.InterfaceMemberMustBeImplemented::class to Channel.LAZY,
        // ---- import-on-the-fly 通道 ----
        CaCfirDiagnostic.UseExprWithoutImport::class to Channel.IMPORT_ON_THE_FLY,
    )

    /**
     * 注册键全部为具体诊断类：不允许 `CaDiagnosticWithPsi::class` 这类泛型基类键。
     */
    @Test
    fun allRegisteredKeysAreConcreteDiagnosticClasses() {
        val genericKeys = registeredKeysByChannel().values.flatten()
            .filter { it == CaDiagnosticWithPsi::class }
        assertTrue(
            genericKeys.isEmpty(),
            "注册键必须是具体诊断类，发现泛型基类键（会静默失效）: $genericKeys",
        )
    }

    /**
     * 最低覆盖承诺集逐条核对：每条「诊断 + 通道」组合必须有 factory 注册。
     */
    @Test
    fun promisedCoverageIsPresent() {
        val keysByChannel = registeredKeysByChannel()
        val missing = promisedCoverage.mapNotNull { (key, channel) ->
            val present = key in keysByChannel.getValue(channel)
            if (present) null else "${channel.name}: ${key.simpleName}"
        }
        assertTrue(
            missing.isEmpty(),
            "承诺覆盖集出现缺口（P0/P1 交付的注册被移除？）: $missing",
        )
    }

    /**
     * 读取本 registrar 三个通道各自的注册键集合。
     *
     * `CangJieQuickFixesList.quickFixes` 是私有只读映射，测试以同模块反射读取快照；
     * 可见性若调整请同步此处。
     */
    private fun registeredKeysByChannel(): Map<Channel, Set<KClass<out CaDiagnosticWithPsi<*>>>> {
        val registrar = org.cangnova.cangjie.ide.codeInsight.fixes.CangJieQuickFixRegistrar()
        val listField = CangJieQuickFixRegistrar::class.java.getDeclaredField("list")
        val lazyField = CangJieQuickFixRegistrar::class.java.getDeclaredField("lazyList")
        val importField = CangJieQuickFixRegistrar::class.java.getDeclaredField("importOnTheFlyList")
        listOf(listField, lazyField, importField).forEach { it.isAccessible = true }

        return mapOf(
            Channel.LIST to readKeys(listField.get(registrar)),
            Channel.LAZY to readKeys(lazyField.get(registrar)),
            Channel.IMPORT_ON_THE_FLY to readKeys(importField.get(registrar)),
        )
    }

    /**
     * 从 `CangJieQuickFixesList` 实例读出注册键集合。
     */
    @Suppress("UNCHECKED_CAST")
    private fun readKeys(list: Any): Set<KClass<out CaDiagnosticWithPsi<*>>> {
        val quickFixesField = list.javaClass.getDeclaredField("quickFixes")
        quickFixesField.isAccessible = true
        val map = quickFixesField.get(list) as Map<KClass<out CaDiagnosticWithPsi<*>>, *>
        return map.keys
    }
}

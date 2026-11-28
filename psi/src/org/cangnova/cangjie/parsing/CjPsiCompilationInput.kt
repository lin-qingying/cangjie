package org.cangnova.cangjie.parsing

import com.intellij.openapi.util.Key
import org.cangnova.cangjie.psi.CjFile

/**
 * 一个目标 PSI 根的完整输入。目标编译必须执行完整文法，不能从旧树的存活声明重放角色；
 * 该配置仅属于新建根，不随平台 PSI copy 或局部 diff 复制到另一输入。
 */
internal class CjPsiCompilationInput(val seed: CjParserSeed)

private val PSI_COMPILATION_INPUT_KEY: Key<CjPsiCompilationInput> = Key.create("CANGJIE_PSI_COMPILATION_INPUT")

internal var CjFile.psiCompilationInput: CjPsiCompilationInput?
    get() = getUserData(PSI_COMPILATION_INPUT_KEY)
    set(value) { putUserData(PSI_COMPILATION_INPUT_KEY, value) }

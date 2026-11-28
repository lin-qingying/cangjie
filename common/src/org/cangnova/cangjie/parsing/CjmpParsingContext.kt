package org.cangnova.cangjie.parsing

/** 文件编译调用与语法文法分离；SyntaxOnly 不代表经过编译角色检查。 */
sealed interface CjmpParsingContext {
    data object SyntaxOnly : CjmpParsingContext

    /** 两个编译谓词独立，版本规则由调用方按同一次 session 确定。 */
    data class Compilation(
        val compileCommon: Boolean,
        val compileSpecific: Boolean,
        val enabled: Boolean,
        val initialFileRole: CjmpFileRole = CjmpFileRole.NONE,
        /** alpha.20 起接受任一编译角色和同文件混合。 */
        val mixedCommonSpecificDeclarations: Boolean = false,
        /** alpha.20 起允许 common 直接成员嵌套在 specific 外层。 */
        val commonMemberInSpecificOuter: Boolean = false,
        /** 主构造器成员参数的独立角色比较规则。 */
        val parameterRoleMatching: Boolean = false,
        /** alpha.66 前 common/specific 泛型声明由 parser 拒绝。 */
        val genericDeclarationsAvailable: Boolean = true,
        /** alpha.66 前 specific 参数默认值属于语法诊断。 */
        val parameterDefaultValueRulesAvailable: Boolean = true,
        /** 当前版本支持 specific 源码拼写。 */
        val modifierSyntaxAvailable: Boolean = true,
        /** 早期 alpha 支持 platform 源码拼写。 */
        val platformModifierSyntaxAvailable: Boolean = false,
        /** 旧版主构造器修饰符目标表的独立版本门。 */
        val primaryConstructorRoleModifiersAvailable: Boolean = false,
    ) : CjmpParsingContext
}

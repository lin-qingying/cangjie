package org.cangnova.cangjie.cfir.checkers.generator.diagnostics

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model.DiagnosticList
import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model.PositioningStrategy
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjImportItem
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjTypeReference
import org.cangnova.cangjie.util.PrivateForInline

/**
 * 诊断信息列表定义对象
 * 
 * 该对象包含了仓颉编译器中CFIR检查阶段所有可能的诊断错误定义。
 * 通过分组的方式组织不同类型的诊断，每个诊断包含错误类型、发生位置和参数信息。
 */
@Suppress("UNUSED_VARIABLE", "LocalVariableName", "ClassName", "unused")
@OptIn(PrivateForInline::class)
object DIAGNOSTICS_LIST : DiagnosticList("CfirErrors") {

    /**
     * 解析（Resolve）相关的诊断
     * 处理符号解析、声明查找等过程中出现的错误
     */
    val RESOLVE by object : DiagnosticGroup("Resolve") {
        val NO_CONSTRUCTOR by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
    }

    /**
     * 重声明（Redeclaration）相关的诊断
     * 处理同名分类器、可调用声明冲突
     */
    val REDECLARATION by object : DiagnosticGroup("Redeclaration") {
        val CONFLICTING_OVERLOADS by error<CjNamedDeclaration>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS) {
            parameter<Collection<String>>("conflictingSymbols")
        }

        val REDECLARATION by error<CjNamedDeclaration>(PositioningStrategy.ACTUAL_DECLARATION_NAME) {
            parameter<Collection<String>>("conflictingSymbols")
        }

        val CLASSIFIER_REDECLARATION by error<CjNamedDeclaration>(PositioningStrategy.ACTUAL_DECLARATION_NAME) {
            parameter<Collection<String>>("conflictingSymbols")
        }
    }

    /**
     * 导入（Imports）相关的诊断
     * 处理import语句的各种错误：导入目标不存在、名称冲突等
     */
    val IMPORTS by object : DiagnosticGroup("Imports") {
        // 导入目标不存在：被导入的包或符号不存在
        val UNRESOLVED_IMPORT by  error<PsiElement>(PositioningStrategy.IMPORT_LAST_NAME) {
            parameter<String>("reference")
        }

        // 导入名称冲突：导入的符号与本地已有符号重名
        val IMPORT_CONFLICT by error<CjImportItem> {
            parameter<Name>("name")  // 发生冲突的名称
        }

        // 导入别名冲突：使用as关键字定义的别名与已有符号重名
        val IMPORT_ALIAS_CONFLICT by error<CjImportItem> {
            parameter<Name>("alias")  // 发生冲突的别名
        }
    }

    /**
     * 超类型（SuperTypes）相关的诊断
     * 处理类继承、接口实现等继承关系中的错误
     */
    val SUPERTYPES by object : DiagnosticGroup("SuperTypes") {
        // 超类型自引用：类或接口试图继承或实现自己
        val SUPER_TYPES_SELF_REFERENCE by error<CjTypeReference> {
            parameter<Name>("className")  // 自引用的类名
        }

        // 超类型重复：同一类型在继承列表中出现多次
        val SUPER_TYPES_DUPLICATE by error<CjTypeReference> {
            parameter<Name>("typeName")  // 重复出现的类型名
        }

        // 接口不能继承类：接口试图继承一个具体的类（违反接口规范）
        val INTERFACE_CANNOT_INHERIT_CLASS by error<CjTypeReference> {
            parameter<Name>("interfaceName")  // 试图继承的接口名
            parameter<Name>("superTypeName")  // 被继承的类型名
        }

        // 类有多个超类：一个类试图继承多个具体的类（不支持多重继承）
        val MULTIPLE_CLASS_SUPER_TYPES by error<CjTypeReference> {
            parameter<Name>("className")  // 该类名
            parameter<Collection<Name>>("superTypes")  // 多个超类名称列表
        }
    }

    /**
     * 扩展（Extend）相关的诊断
     * 处理类型扩展、接口扩展等相关错误
     */
    val EXTEND by object : DiagnosticGroup("Extend") {
        // 非法的扩展类型：尝试扩展不允许扩展的类型
        val ILLEGAL_EXTENDED_TYPE by error<CjTypeReference> {
            parameter<Name>("typeName")  // 不允许扩展的类型名
        }

        // 扩展重复接口：在扩展中重复添加同一个接口
        val EXTEND_DUPLICATE_INTERFACE by error<CjTypeReference> {
            parameter<Name>("interfaceName")  // 重复的接口名
        }

        // 扩展非接口类型：扩展操作的对象不是接口
        val EXTEND_NOT_INTERFACE by error<CjTypeReference> {
            parameter<Name>("typeName")  // 被扩展的非接口类型名
        }

        // 扩展孤儿规则冲突：目标类型和接口都不在当前包
        val EXTEND_ORPHAN_RULE by error<CjTypeReference> {
            parameter<Name>("targetTypeName")
        }

        // 扩展泛型使用错误：声明了泛型参数但未参与扩展语义
        val EXTEND_GENERIC_USAGE by error<CjDeclaration> {
            parameter<Name>("typeParameterName")
        }

        // 扩展特化冲突：同一目标上同一接口出现互相冲突的特化
        val EXTEND_SPECIALIZATION_CONFLICT by error<CjTypeReference> {
            parameter<Name>("interfaceName")
        }

        // 扩展默认实现冲突：跨声明引入同一接口时，其默认实现成员发生冲突
        val EXTEND_DEFAULT_IMPLEMENTATION_CONFLICT by error<CjTypeReference> {
            parameter<Name>("memberName")
            parameter<Name>("interfaceName")
        }

        // 不可变类型（struct/enum）的 extend 不能实现包含 mut 成员的接口
        val EXTEND_IMMUTABLE_MUT_INTERFACE by error<CjTypeReference> {
            parameter<Name>("interfaceName")
            parameter<Name>("mutMemberName")
        }

        // 不可变类型的 extend 不能定义 mut 属性
        val EXTEND_IMMUTABLE_MUT_PROPERTY by error<CjDeclaration>(PositioningStrategy.MUT_MODIFIER) {
            parameter<Name>("propertyName")
        }

        // 不可变非 enum 类型的 extend 不能定义索引赋值操作符
        val EXTEND_IMMUTABLE_INDEX_ASSIGNMENT by error<CjDeclaration> {
            parameter<Name>("operatorName")
        }

        // 接口不可被 extend 实现（如 core.Any / core.CType）
        val EXTEND_INTERFACE_NOT_EXTENDABLE by error<CjTypeReference> {
            parameter<Name>("interfaceName")
        }

        // C/Java 互操作类型不能被 extend
        val EXTEND_C_TYPE_NOT_ALLOWED by error<CjTypeReference> {
            parameter<Name>("typeName")
        }

        // extend 体内不允许使用 super 关键字
        val EXTEND_SUPER_NOT_ALLOWED by error<CjExpression>()

        // struct 体内不允许使用 super 关键字
        val STRUCT_SUPER_NOT_ALLOWED by error<CjExpression>()

        // enum 体内不允许使用 super 关键字
        val ENUM_SUPER_NOT_ALLOWED by error<CjExpression>()

        // interface 体内不允许使用 super 关键字
        val INTERFACE_SUPER_NOT_ALLOWED by error<CjExpression>()
    }

    /**
     * 声明状态（DeclarationStatus）相关的诊断
     * 处理访问修饰符、可变性等声明属性的合法性检查
     */
    val DECLARATION_STATUS by object : DiagnosticGroup("DeclarationStatus") {
        // static 声明不能同时使用 open/abstract/override 修饰符
        val STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE by error<CjDeclaration> {
            parameter<Name?>("declarationName")  // 声明的名称（可能为空）
        }

        // mut 修饰符只能用于属性声明以及 struct 体内的函数声明
        val MUT_ONLY_ON_FUNCTION by error<CjDeclaration> {
            parameter<Name?>("declarationName")  // 声明的名称（可能为空）
        }

        // 标记了 override 但没有可覆盖的父成员
        val NOTHING_TO_OVERRIDE by error<CjNamedDeclaration>(PositioningStrategy.OVERRIDE_MODIFIER)

        val OVERRIDE_STATIC_ERROR by error<PsiElement> {
            parameter<String>("declarationKind")
        }

        val REDEF_INSTANCE_ERROR by error<PsiElement> {
            parameter<String>("declarationKind")
        }

        val INVALID_OPERATOR_PARAMETER_COUNT by error<PsiElement> {
            parameter<String>("operator")
            parameter<String>("expectedCount")
            parameter<String>("actualCount")
        }

        val REPEATED_MODIFIER by error<PsiElement> {
            parameter<CjKeywordToken>("modifier")
        }

        val REDUNDANT_MODIFIER by warning<PsiElement> {
            parameter<CjKeywordToken>("modifier")
            parameter<CjKeywordToken>("redundantBecauseOf")
        }

        // 普通修饰符组合不兼容
        val INCOMPATIBLE_MODIFIERS by error<PsiElement> {
            parameter<CjKeywordToken>("modifier1")
            parameter<CjKeywordToken>("modifier2")
        }

        val WRONG_MODIFIER_TARGET by error<PsiElement> {
            parameter<CjKeywordToken>("modifier")
            parameter<String>("target")
        }

        val WRONG_MODIFIER_CONTAINING_DECLARATION by error<PsiElement> {
            parameter<CjKeywordToken>("modifier")
            parameter<String>("container")
        }

        val REDUNDANT_MODIFIER_FOR_TARGET by warning<PsiElement> {
            parameter<CjKeywordToken>("modifier")
            parameter<String>("target")
        }

        val DEPRECATED_MODIFIER_FOR_TARGET by warning<PsiElement> {
            parameter<CjKeywordToken>("modifier")
            parameter<String>("target")
        }

        val DEPRECATED_MODIFIER_CONTAINING_DECLARATION by warning<PsiElement> {
            parameter<CjKeywordToken>("modifier")
            parameter<String>("container")
        }

        val DEPRECATED_MODIFIER_PAIR by warning<PsiElement> {
            parameter<CjKeywordToken>("modifier")
            parameter<CjKeywordToken>("conflictingModifier")
        }

        // override 成员可见性低于被覆盖成员可见性
        val CANNOT_WEAKEN_ACCESS_PRIVILEGE by error<CjNamedDeclaration>(PositioningStrategy.ACTUAL_DECLARATION_NAME) {
            parameter<Name>("baseMemberName")
            parameter<Visibility>("baseVisibility")
        }
    }

    /**
     * 模式匹配（Match）相关的诊断
     * 处理when表达式和模式匹配的完整性检查
     */
    val MATCH by object : DiagnosticGroup("Match") {
        // 非穷尽匹配：when表达式没有覆盖所有可能的情况
        val NON_EXHAUSTIVE_MATCH by error<PsiElement> {
            parameter<Collection<String>>("missingCases")  // 缺失的匹配分支列表
        }
    }
    val CONSTRAINT by object : DiagnosticGroup("Constraint") {
        val NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<Name>("name")
        }

        val ONLY_ONE_CLASS_BOUND_ALLOWED by error<CjElement>()

        val REPEATED_BOUND by error<CjElement>()

        val CONFLICTING_UPPER_BOUNDS by error<CjNamedDeclaration>(PositioningStrategy.ACTUAL_DECLARATION_NAME)

        val CANNOT_INFER_PARAMETER_TYPE by error<CjElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<CfirTypeParameterSymbol>("parameter")
        }

        val NEW_INFERENCE_ERROR by error<PsiElement> {
            parameter<String>("message")
        }

        val TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR by error<CjElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<CfirTypeParameterSymbol>("parameter")
        }

        val BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION by error<PsiElement> {
            parameter<Name>("typeParameterName")
            parameter<Name>("declarationName")
        }

        val INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION by error<PsiElement> {
            parameter<String>("typeVariable")
            parameter<Collection<ConeCangJieType>>("incompatibleTypes")
            parameter<String>("kindDescription")
            parameter<String>("causingTypesText")
        }

        val INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION by warning<PsiElement> {
            parameter<String>("typeVariable")
            parameter<Collection<ConeCangJieType>>("incompatibleTypes")
            parameter<String>("kindDescription")
            parameter<String>("causingTypesText")
        }
    }

    /**
     * 类型检查（TypeCheck）相关的诊断
     * 处理类型不匹配、类型转换等类型系统错误
     */
    val TYPE_CHECK by object : DiagnosticGroup("TypeCheck") {
        // 类型不匹配：表达式的类型与期望类型不符
        val TYPE_MISMATCH by error<PsiElement> {
            parameter<ConeCangJieType>("expectedType")  // 期望的类型
            parameter<ConeCangJieType>("actualType")  // 实际的类型
            parameter<Boolean>("isMismatchDueToNullability")  // 是否因为可空性导致不匹配
        }

        // the type argument is CjNamedDeclaration because PSI of FirProperty can be KtParameter in 'for' loops
        val PATTERN_INITIALIZER_TYPE_MISMATCH by error<CjNamedDeclaration>(PositioningStrategy.PATTERN_VARIABLE_INITIALIZER) {
            parameter<ConeCangJieType>("expectedType")
            parameter<ConeCangJieType>("actualType")
            parameter<Boolean>("isMismatchDueToNullability")
        }

        // 返回类型不匹配：函数返回值的类型与声明的返回类型不符
        val RETURN_TYPE_MISMATCH by error<CjExpression> {
            parameter<ConeCangJieType>("expectedType")  // 期望的返回类型
            parameter<ConeCangJieType>("actualType")  // 实际的返回类型
            parameter<Boolean>("isMismatchDueToNullability")  // 是否因为可空性导致不匹配
        }

        // 参数类型不匹配：函数调用时传入的参数类型与形参类型不符
        val ARGUMENT_TYPE_MISMATCH by error<PsiElement> {
            parameter<ConeCangJieType>("expectedType")  // 形参期望的类型
            parameter<ConeCangJieType>("actualType")  // 实参的实际类型
            parameter<Boolean>("isMismatchDueToNullability")  // 是否因为可空性导致不匹配
        }

        // 赋值类型不匹配：赋值右侧表达式的类型与左侧变量类型不符
        val ASSIGNMENT_TYPE_MISMATCH by error<CjExpression>(PositioningStrategy.OPERATOR) {
            parameter<ConeCangJieType>("expectedType")  // 变量的目标类型
            parameter<ConeCangJieType>("actualType")  // 赋值表达式的实际类型
            parameter<Boolean>("isMismatchDueToNullability")  // 是否因为可空性导致不匹配
            // PositioningStrategy.OPERATOR 表示将错误标记位置设置在赋值操作符处
        }

        // 泛型类型在无法从上下文推断时必须显式提供类型参数
        val GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<Name>("typeName")
        }

        // 可见性错误：成员在当前上下文不可见
        val INVISIBLE_MEMBER by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<String>("member")
            parameter<String>("visibility")
        }

        // 可见性错误：引用在当前上下文不可见
        val INVISIBLE_REFERENCE by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<String>("reference")
            parameter<String>("visibility")
        }

        // override 返回类型不协变
        val OVERRIDING_RETURN_TYPE_MISMATCH by error<PsiElement> {
            parameter<ConeCangJieType>("actualType")
            parameter<ConeCangJieType>("expectedType")
            parameter<Name>("overriddenName")
        }

        // override 目标不可见
        val CANNOT_OVERRIDE_INVISIBLE_MEMBER by error<CjNamedDeclaration>(PositioningStrategy.OVERRIDE_MODIFIER) {
            parameter<Name>("memberName")
        }

        // 父类未开放继承
        val CLASS_NOT_OPEN_FOR_INHERITANCE by error<CjTypeReference> {
            parameter<Name>("className")
        }

        // 非抽象类/结构体未实现继承来的抽象成员
        val ABSTRACT_MEMBER_NOT_IMPLEMENTED by error<CjNamedDeclaration>(PositioningStrategy.ACTUAL_DECLARATION_NAME) {
            parameter<Name>("className")
        }


    }

    /**
     * 常量求值（ConstEval）相关的诊断
     * 处理编译期常量表达式求值时发生的错误
     */
    val CONST_EVAL by object : DiagnosticGroup("ConstEval") {
        // 数字字面量溢出：数字字面值超出了目标类型的范围
        val LITERAL_NUMERIC_OVERFLOW by error<PsiElement> {
            parameter<String>("literalText")  // 数字字面量的文本表示
            parameter<ConeCangJieType>("targetType")  // 目标类型（如Int32、Int64等）
        }

        // 常量求值除以零：在编译期求值时，被除数为0
        val CONST_EVAL_DIVIDE_BY_ZERO by error<PsiElement> {
            parameter<String>("operatorName")  // 运算符名称（如 "div"、"rem"）
        }

        // 常量求值算术溢出：在编译期求值时，算术运算导致数值溢出
        val CONST_EVAL_ARITHMETIC_OVERFLOW by error<PsiElement> {
            parameter<String>("operatorName")  // 导致溢出的运算符名称
        }
    }


    /**
     * 未解析（Unresolved）相关的诊断
     * 处理无法找到对应定义的符号引用错误
     */
    val UNRESOLVED by object : DiagnosticGroup("Unresolved") {
        // 未解析的引用：代码中引用了一个不存在或无法找到的名称
        val UNRESOLVED_REFERENCE by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<String>("reference")  // 无法解析的引用名称
            parameter<String?>("operator")  // 相关的运算符（可选，如重载操作符）
            // PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED 表示错误位置指向被引用的名称部分
        }

        val INVALID_BINARY_OPERATOR by error<PsiElement>(PositioningStrategy.OPERATOR) {
            parameter<String>("operator")
            parameter<String>("leftType")
            parameter<String>("rightType")
        }

        // 变量已解析但其类型上没有匹配的 invoke 操作符
        val NO_MATCHING_OPERATOR_INVOKE by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<String>("name")       // 变量名
            parameter<ConeCangJieType>("type") // 接收器类型
        }
    }
}

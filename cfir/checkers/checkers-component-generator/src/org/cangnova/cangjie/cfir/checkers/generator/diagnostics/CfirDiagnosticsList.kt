/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

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
import org.cangnova.cangjie.psi.*
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
        val REF_NOT_BE_TYPE by error<PsiElement>()
        val NOT_A_TYPE by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<String>("typeName")
        }
        val INVALID_ACCESS_CONTROL by error<PsiElement>()

        /**
         * enum 类型名不能直接当作类型构造器调用，例如 `A(1)`。
         * 这里要求用户改用枚举构造器（如 `A1(1)`），而不是继续退化成通用无构造器错误。
         */
        val ENUM_TYPE_CANNOT_BE_USED_AS_CONSTRUCTOR by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<Name>("enumName")
        }
    }

    /**
     * 重声明（Redeclaration）相关的诊断
     * 处理同名分类器、可调用声明冲突
     */
    val REDECLARATION by object : DiagnosticGroup("Redeclaration") {
        val CONFLICTING_OVERLOADS by error<CjNamedDeclaration>(PositioningStrategy.ACTUAL_DECLARATION_NAME) {
            parameter<Collection<String>>("conflictingSymbols")
        }

        val REDECLARATION by error<PsiElement>(PositioningStrategy.ACTUAL_DECLARATION_NAME) {
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
        val IMPORT_CONFLICT by error<CjImportItem>(PositioningStrategy.IMPORT_LAST_NAME) {
            parameter<Name>("name")  // 发生冲突的名称
        }

        // 导入别名冲突：使用as关键字定义的别名与已有符号重名
        val IMPORT_ALIAS_CONFLICT by error<CjImportItem>(PositioningStrategy.IMPORT_ALIAS) {
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

        // 继承图中发现多节点循环
        val INHERITANCE_CYCLE by error<PsiElement>(PositioningStrategy.DEFAULT)

        // 超类型重复：同一类型在继承列表中出现多次
        val SUPER_TYPES_DUPLICATE by error<PsiElement>(PositioningStrategy.ACTUAL_DECLARATION_NAME) {
            parameter<Name>("typeName")  // 重复出现的类型名
        }

        // 接口不能继承类：接口试图继承一个具体的类（违反接口规范）
        val INTERFACE_CANNOT_INHERIT_CLASS by error<CjNamedDeclaration>(PositioningStrategy.ACTUAL_DECLARATION_NAME) {
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
        val EXTEND_GENERIC_USAGE by error<CjTypeReference> {
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
        val STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE by error<CjNamedDeclaration>(PositioningStrategy.ACTUAL_DECLARATION_NAME) {
            parameter<Name?>("declarationName")  // 声明的名称（可能为空）
        }

        // class 成员缺少函数体/属性访问器，导致不能作为 abstract 成员存在
        val MISSING_FUNC_BODY by error<PsiElement> {
            parameter<String>("memberKind")
            parameter<Name>("memberName")
        }

        // mut 修饰符只能用于属性声明以及 struct 体内的函数声明
        val MUT_ONLY_ON_FUNCTION by error<CjNamedDeclaration>(PositioningStrategy.ACTUAL_DECLARATION_NAME) {
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

        // primitive 内建一元 operator 已由语言内建提供，用户 extend 不能重新定义。
        val OPERATOR_OVERLOAD_BUILT_IN_UNARY_OPERATOR by error<PsiElement> {
            parameter<String>("operator")
            parameter<String>("receiverType")
        }

        // primitive 内建二元 operator 已由语言内建提供，用户 extend 不能重新定义。
        val OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR by error<PsiElement> {
            parameter<String>("operator")
            parameter<String>("receiverType")
            parameter<String>("parameterType")
        }

        val REPEATED_MODIFIER by error<PsiElement> {
            parameter<CjKeywordToken>("modifier")
        }

        val REDUNDANT_MODIFIER by warning<PsiElement> {
            parameter<CjKeywordToken>("modifier")
            parameter<CjKeywordToken>("redundantBecauseOf")
        }

        // 非可继承 class 中的 open 成员会被官方编译器忽略。
        val IGNORE_OPEN by warning<PsiElement>()

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

        // 继承/extend 实现成员可见性低于接口或基类成员可见性
        val WEAK_VISIBILITY by error<PsiElement> {
            parameter<Name>("baseMemberName")
            parameter<Visibility>("baseVisibility")
        }

        // override / redef 的形参与父声明的命名参数语义不一致
        val PARAM_NAMED_MISMATCHED by error<CjNamedDeclaration>(PositioningStrategy.ACTUAL_DECLARATION_NAME) {
            parameter<Name>("baseMemberName")
        }
    }

    /**
     * 调用解析（CallResolution）相关的诊断
     * 处理参数映射、命名参数与构造器调用阶段的绑定错误。
     */
    val CALL_RESOLUTION by object : DiagnosticGroup("CallResolution") {
        // 缺少必填参数
        val NO_VALUE_FOR_PARAMETER by error<PsiElement>(PositioningStrategy.VALUE_ARGUMENTS_LIST) {
            parameter<Name>("parameterName")
        }

        // 实参个数超出可调用目标可接受范围
        val TOO_MANY_ARGUMENTS by error<PsiElement> {
            parameter<Name>("targetName")
        }

        // 命名参数名在目标参数列表中不存在
        val NAMED_PARAMETER_NOT_FOUND by error<PsiElement>(PositioningStrategy.NAME_OF_NAMED_ARGUMENT) {
            parameter<Name>("parameterName")
        }

        // 同一个参数被多次传入
        val ARGUMENT_PASSED_TWICE by error<PsiElement>(PositioningStrategy.NAME_OF_NAMED_ARGUMENT)

        // 命名参数前缀出现，但目标参数不支持 named argument
        val NAMED_ARGUMENTS_NOT_ALLOWED by error<PsiElement>(PositioningStrategy.NAME_OF_NAMED_ARGUMENT) {
            parameter<String>("targetDescription")
        }

        // 命名实参与位置实参混用顺序非法
        val MIXING_NAMED_AND_POSITIONAL_ARGUMENTS by error<PsiElement>()

        // 形参被声明为命名参数，但调用时未带参数名前缀
        val NEED_NAMED_ARGUMENT by error<PsiElement> {
            parameter<Name>("parameterName")
        }

        // 构造器候选存在但调用发生歧义
        val AMBIGUOUS_CONSTRUCTOR_CALL by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<Name>("className")
        }

        // 普通函数调用的候选都可适用，但无法选出唯一最优目标
        val AMBIGUOUS_FUNCTION_CALL by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<Name>("functionName")
        }

        // 调用实参的重载函数引用类型无法唯一确定
        val AMBIGUOUS_ARG_TYPE by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<Name>("functionName")
        }

        // 构造器委托调用形成递归
        val RECURSIVE_CONSTRUCTOR_CALL by error<PsiElement>(PositioningStrategy.ACTUAL_DECLARATION_NAME)

        // 类型声明中存在多个主构造器，对齐官方 sema_multiple_primary_constructors。
        val MULTIPLE_PRIMARY_CONSTRUCTORS by error<PsiElement>()

        // 值类型字段或构造器参数形成递归
        val VALUE_TYPE_RECURSIVE by error<PsiElement>()

        // this/super 构造器委托调用出现在非法位置
        val ILLEGAL_THIS_OR_SUPER_CALL by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<String>("calleeName")
        }

        // 字段初始化器中 this/super 初始化非 static 成员，对齐官方 sema_this_or_super_not_allowed_to_initialize_non_static_member。
        val THIS_OR_SUPER_NOT_ALLOWED_TO_INITIALIZE_NON_STATIC_MEMBER by error<PsiElement>(
            PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED
        ) {
            parameter<String>("calleeName")
        }

        // 字段初始化器中 this/super 初始化 static 成员，对齐官方 sema_this_or_super_not_allowed_to_initialize_static_member。
        val THIS_OR_SUPER_NOT_ALLOWED_TO_INITIALIZE_STATIC_MEMBER by error<PsiElement>(
            PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED
        ) {
            parameter<String>("calleeName")
        }

        // this/super 出现在 class/struct/interface 外部，对齐官方 sema_this_super_use_error_outside_class。
        val THIS_SUPER_USE_ERROR_OUTSIDE_CLASS by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<String>("calleeName")
        }

        // this(...)/super(...) 出现在构造器外部，对齐官方 sema_invalid_this_call_outside_ctor。
        val INVALID_THIS_CALL_OUTSIDE_CTOR by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<String>("calleeName")
        }

        // 裸 super 不能作为普通表达式，对齐官方 sema_illegal_super_alone。
        val ILLEGAL_SUPER_ALONE by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)

        // struct 构造器或成员函数外使用 this，对齐官方 sema_illegal_this_outside_struct_constructor。
        val ILLEGAL_THIS_OUTSIDE_STRUCT_CONSTRUCTOR by error<PsiElement>(
            PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED
        )

        // this/super 构造器委托调用不是构造器第一条语句，对齐官方 sema_illegal_place_of_calling_this_or_super。
        val ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER by error<PsiElement>() {
            parameter<String>("calleeName")
        }

        // 主构造器中调用 this(...)，对齐官方 sema_illegal_place_of_calling_this_primary_constructor。
        val ILLEGAL_PLACE_OF_CALLING_THIS_PRIMARY_CONSTRUCTOR by error<PsiElement>()

        // 构造器默认参数或委托参数中读取尚未完成初始化的实例成员
        val ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER by error<PsiElement>(
            PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED
        ) {
            parameter<String>("memberName")
            parameter<String>("contextDescription")
        }

        // open/abstract class 构造器中禁止访问实例函数或属性。
        val ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR by error<PsiElement>(
            PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED
        ) {
            parameter<String>("memberKind")
            parameter<String>("memberName")
            parameter<Name>("className")
        }

        // 通过 super 直接访问抽象函数，对齐官方 sema_abstract_method_cannot_be_accessed_directly。
        val ABSTRACT_METHOD_CANNOT_BE_ACCESSED_DIRECTLY by error<PsiElement>()

        // open/abstract class 构造器中禁止把 this 当作普通表达式。
        val THIS_AS_EXPRESSION_IN_FUNC by error<PsiElement>() {
            parameter<String>("contextDescription")
        }

        // static 函数体中禁止引用实例 this。
        val STATIC_MEMBERS_CANNOT_CALL_MEMBERS by error<PsiElement>()

        // 类型名不能访问实例成员，对齐官方 sema_illegal_access_non_static_member。
        val ILLEGAL_ACCESS_NON_STATIC_MEMBER by error<PsiElement>() {
            parameter<Name>("memberName")
        }

        // static 函数体中禁止引用实例成员。
        val STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER by error<PsiElement>() {
            parameter<Name>("memberName")
        }

        // static 上下文中的 lambda 体禁止引用实例成员。
        val STATIC_LAMBDA_CANNOT_ACCESS_NON_STATIC by error<PsiElement>() {
            parameter<Name>("memberName")
        }

        // static 变量初始化器中禁止引用实例成员。
        val STATIC_VARIABLE_CANNOT_ACCESS_NON_STATIC_MEMBER by error<PsiElement>() {
            parameter<Name>("memberName")
        }

        // 对象不能访问 static 成员，对齐官方 sema_object_cannot_access_static_member。
        val OBJECT_CANNOT_ACCESS_STATIC_MEMBER by error<PsiElement>() {
            parameter<Name>("memberName")
        }

        // 父类不存在可隐式调用的无参构造器，要求显式 super(...)
        val EXPLICIT_SUPER_CALL_REQUIRED by error<PsiElement>(PositioningStrategy.ACTUAL_DECLARATION_NAME)

        // 父类没有可隐式调用的无参构造器，对齐官方 sema_no_non_param_constructor_in_super_class。
        val NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS by error<PsiElement>()

        // break/continue 必须位于循环体内
        val INVALID_LOOP_CONTROL by error<PsiElement>()
    }

    /**
     * 初始化与使用合法性（Initialization / LegalityOfUsage）
     *
     * 这里承接 definite assignment 风格的语义错误：
     * - 局部变量或实例字段在确定初始化前被读取；
     * - 构造器结束时仍有实例字段没有被初始化。
     */
    val INITIALIZATION by object : DiagnosticGroup("Initialization") {
        val USED_BEFORE_INITIALIZATION by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<Name>("variableName")
        }

        val CLASS_UNINITIALIZED_FIELD by error<PsiElement>(PositioningStrategy.ACTUAL_DECLARATION_NAME) {
            parameter<Name>("fieldName")
        }
    }

    /**
     * 泛型访问语义（Generic access）
     *
     * 这组诊断专门描述“通过类型参数接收者访问其 upper bounds 中不存在的成员/方法”，
     * 不再退化为普通 `UNRESOLVED_REFERENCE`。
     */
    val GENERIC_ACCESS by object : DiagnosticGroup("GenericAccess") {
        val GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<Name>("memberName")
            parameter<Name>("typeParameterName")
        }

        val GENERIC_NO_METHOD_MATCH_IN_UPPER_BOUNDS by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<Name>("methodName")
            parameter<Name>("typeParameterName")
        }
    }

    /**
     * mut / immutable 语义
     *
     * 当前先覆盖 struct 成员函数中的 `this` 视角限制：
     * - immutable 成员函数不能修改当前实例字段；
     * - immutable 成员函数不能调用当前实例上的 mut 成员函数。
     */
    val MUTABILITY by object : DiagnosticGroup("Mutability") {
        val CANNOT_MODIFY_VAR by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<Name>("variableName")
        }

        val IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION by error<PsiElement> {
            parameter<Name>("currentFunctionName")
            parameter<Name>("targetFunctionName")
        }
    }

    /**
     * 注解声明和注解使用相关诊断。
     */
    val ANNOTATION by object : DiagnosticGroup("Annotation") {
        val ANNOTATION_NO_CONST_INIT by error<PsiElement>(PositioningStrategy.ACTUAL_DECLARATION_NAME)
    }

    /**
     * interop / foreign function 相关的语义诊断。
     *
     * 这一组只承接“声明或类型已经进入语义层之后”的规则，
     * 不把 parser / lexer 层面的 foreign 语法错误混到这里。
     */
    val INTEROP by object : DiagnosticGroup("Interop") {
        val INVALID_CFUNC_RETURN_TYPE by error<CjTypeReference> {
            parameter<ConeCangJieType>("actualType")
        }

        val INVALID_CFUNC_PARAMETER_TYPE by error<CjTypeReference> {
            parameter<ConeCangJieType>("actualType")
        }

        val ONLY_CFUNC_CAN_USE_ANNOTATION by error<PsiElement> {
            parameter<String>("annotationName")
        }

        val ILLEGAL_SCOPE_USE_OF_ANNOTATION by error<PsiElement> {
            parameter<String>("annotationName")
        }
    }

    /**
     * throw / try / catch 相关诊断。
     *
     * 当前这一组先提供 diagnostics2 期望面所需的诊断定义，
     * 真实检测逻辑后续按对应 checker 接入。
     */
    val EXCEPTION by object : DiagnosticGroup("Exception") {
        val THROW_EXPR_WITH_WRONG_TYPE by error<PsiElement>(PositioningStrategy.THROW_KEYWORD)

        val CATCH_TYPE_MUST_EXTEND_EXCEPTION by error<CjTypeReference>()

        val USELESS_EXCEPTION_TYPE by warning<CjTypeReference>()
    }

    /**
     * range 表达式相关诊断。
     */
    val RANGE by object : DiagnosticGroup("Range") {
        val RANGE_STEP_CANNOT_BE_ZERO by error<PsiElement>()
    }

    /**
     * effects 相关诊断。
     *
     * effect 语法在 PSI 层始终保留，真正的 feature gate 与语义约束在 CFIR 产出。
     */
    val EFFECTS by object : DiagnosticGroup("Effects") {
        val EFFECTS_FEATURE_DISABLED by error<CjElement> {
            parameter<String>("constructName")
        }

        val COMMAND_INCOMPATIBLE_TYPE by error<CjExpression> {
            parameter<ConeCangJieType>("actualType")
        }

        val COMMAND_HANDLE_TYPE_ERROR by error<CjTypeReference> {
            parameter<ConeCangJieType>("actualType")
        }

        val IMPLICIT_RESUME_OUTSIDE_HANDLER by error<CjResumeExpression>()

        val RESUME_NO_WITH by error<CjResumeExpression> {
            parameter<ConeCangJieType>("resumptionType")
        }

        val RESUME_THROWING_MISMATCH_TYPE by error<CjResumeExpression> {
            parameter<ConeCangJieType>("actualType")
        }

        val MISMATCHING_HANDLE_BLOCK by error<CjBlockExpression> {
            parameter<ConeCangJieType>("actualType")
            parameter<ConeCangJieType>("expectedType")
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

        val TUPLE_PATTERN_NOT_MATCH by error<PsiElement> {
            parameter<String>("actualTypeText")
        }

        val PATTERN_NOT_MATCH by error<PsiElement> {
            parameter<String>("patternText")
        }

        val ENUM_PATTERN_PARAM_SIZE_ERROR by error<PsiElement>()

        val NOT_OVERLOAD_IN_MATCH by error<PsiElement>()

        // 无 selector 的 match 中，某个 case 的结果类型无法计算
        val MATCH_CASE_HAS_NO_TYPE by error<PsiElement>()
    }
    /**
     * where 约束和泛型上界相关诊断。
     */
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

        /**
         * 空数组字面量缺少元素和上下文目标类型时，无法推断 Array 的元素类型。
         * 对齐官方 `sema_empty_arrayLit_type_undefined`，主诊断位置标在 `[`。
         */
        val ARRAY_LITERAL_TYPE_CANNOT_BE_INFERRED by error<PsiElement>(PositioningStrategy.ARRAY_LITERAL_LEFT_BRACKET)

        /**
         * 无目标类型数组字面量的元素类型无法求得可见公共父类型。
         * 对齐官方 `sema_inconsistency_elemType`，诊断挂在整个数组字面量上。
         */
        val INCONSISTENT_ARRAY_LITERAL_ELEMENT_TYPE by error<PsiElement>()

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

        // 赋值类型不匹配：官方 Sema 将 sema_mismatched_types 锚定在赋值右侧表达式。
        val ASSIGNMENT_TYPE_MISMATCH by error<CjExpression> {
            parameter<ConeCangJieType>("expectedType")  // 变量的目标类型
            parameter<ConeCangJieType>("actualType")  // 赋值表达式的实际类型
            parameter<Boolean>("isMismatchDueToNullability")  // 是否因为可空性导致不匹配
        }

        /**
         * 类型本身不支持当前语义场景。
         *
         * 对齐官方 Sema 的 `sema_type_incompatible`，例如复合赋值左值类型不在
         * `COMPOUND_ASSIGN_TYPE_MAP` 中时，错误属于整个语义场景不可用，而不是
         * 右值到左值的普通类型不匹配。
         */
        val TYPE_INCOMPATIBLE by error<PsiElement> {
            parameter<String>("contextDescription")
        }

        /**
         * `VArray<T, N>` 与 `VArray<T, M>` 的元素类型一致但长度不同。
         *
         * 这不是普通 `TYPE_MISMATCH` 的文案换皮，而是官方 Sema 中独立建模的
         * VArray 长度语义；因此需要在通用 mismatch 之下继续分流。
         */
        val VARRAY_SIZE_MISMATCH by error<PsiElement> {
            parameter<Long>("expectedSize")
            parameter<Long>("actualSize")
            parameter<ConeCangJieType>("elementType")
        }

        // 泛型类型在无法从上下文推断时必须显式提供类型参数
        val GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<Name>("typeName")
        }

        // This 类型只允许出现在 class 实例成员函数返回类型中。
        val parse_this_type_not_allow by error<CjTypeReference>()

        // This 类型出现在 parser 允许、但 Sema 禁止的位置，例如 class static 成员函数返回类型。
        val INVALID_POSITION_OF_THIS_TYPE by error<CjTypeReference>()

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
        val OVERRIDING_RETURN_TYPE_MISMATCH by error<PsiElement>(PositioningStrategy.ACTUAL_DECLARATION_NAME) {
            parameter<ConeCangJieType>("actualType")
            parameter<ConeCangJieType>("expectedType")
            parameter<Name>("overriddenName")
        }

        // 官方 sema_return_type_incompatible：实现/重定义/内建 operator 合成时返回类型不兼容。
        val RETURN_TYPE_INCOMPATIBLE by error<PsiElement> {
            parameter<Name>("functionName")
        }

        // extend 关系不能作为 override/implement 返回类型协变依据。
        val RETURN_TYPE_INVARIANCE by error<PsiElement> {
            parameter<Name>("functionName")
            parameter<ConeCangJieType>("interfaceType")
        }

        // 属性 override/implement 的类型必须完全一致。
        val PROPERTY_OVERRIDE_IMPLEMENT_TYPE_DIFF by error<PsiElement> {
            parameter<ConeCangJieType>("actualType")
            parameter<ConeCangJieType>("expectedType")
            parameter<Name>("overriddenName")
        }

        // 可执行编译目标缺少程序入口。
        val MISSING_ENTRY by error<PsiElement>()

        // override 目标不可见
        val CANNOT_OVERRIDE_INVISIBLE_MEMBER by error<CjNamedDeclaration>(PositioningStrategy.OVERRIDE_MODIFIER) {
            parameter<Name>("memberName")
        }

        // 父类未开放继承
        val CLASS_NOT_OPEN_FOR_INHERITANCE by error<CjTypeReference> {
            parameter<Name>("className")
        }

        // 非抽象类/结构体未实现继承来的抽象成员。官方 cjc 报在类型声明起始处。
        val ABSTRACT_MEMBER_NOT_IMPLEMENTED by error<CjNamedDeclaration> {
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

        // 常量求值位移计数为负数
        val CONST_EVAL_NEGATIVE_SHIFT_COUNT by error<PsiElement>()

        // 常量求值位移计数超出目标整型宽度
        val CONST_EVAL_SHIFT_COUNT_OVERFLOW by error<PsiElement>()
    }


    /**
     * 未解析（Unresolved）相关的诊断
     * 处理无法找到对应定义的符号引用错误
     */
    val UNRESOLVED by object : DiagnosticGroup("Unresolved") {
        // 类型位置上的名称无法解析，对齐官方 sema_undeclared_type_name。
        val UNDECLARED_TYPE_NAME by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<String>("typeName")
        }

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

        // 函数调用没有可访问的匹配声明，对齐官方 sema_no_match_function_declaration_for_call。
        val NO_MATCH_FUNCTION_DECLARATION_FOR_CALL by error<PsiElement>(PositioningStrategy.DEFAULT)

        // 函数引用没有可访问的匹配声明，对齐官方 sema_no_match_function_declaration_for_ref。
        val NO_MATCH_FUNCTION_DECLARATION_FOR_REF by error<PsiElement>(PositioningStrategy.DEFAULT)

        // 非函数表达式使用 `()` 调用，对齐官方 sema_no_match_operator_function_call。
        val NO_MATCH_OPERATOR_FUNCTION_CALL by error<PsiElement>(PositioningStrategy.DEFAULT)
    }

    // ========================================================================
    // 以下为对齐 C++ 官方编译器 DiagnosticSema.def 新增的诊断分组
    // ========================================================================

    /**
     * 通用语义错误（General）
     *
     * 对齐 C++ sema_invalid_node_after_check, sema_conflict_with_sub_package 等通用语义诊断。
     */
    val GENERAL by object : DiagnosticGroup("General") {
        // 语义检查后节点无效
        val INVALID_NODE_AFTER_CHECK by error<PsiElement>()

        // 无法推断声明类型
        val UNABLE_TO_INFER_DECL by error<PsiElement>()

        // 多重赋值类型不匹配
        val MISMATCHED_TYPES_MULTIPLE_ASSIGN by error<PsiElement> {
            parameter<ConeCangJieType>("actualType")
        }

        // 类型不匹配（带原因说明）
        val MISMATCHED_TYPES_BECAUSE by error<PsiElement> {
            parameter<ConeCangJieType>("expectedType")
            parameter<ConeCangJieType>("actualType")
            parameter<String>("reason")
        }

        // 歧义使用
        val AMBIGUOUS_USE by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<Name>("name")
        }

        // 顶层声明与子包名冲突
        val CONFLICT_WITH_SUB_PACKAGE by error<CjNamedDeclaration>(PositioningStrategy.ACTUAL_DECLARATION_NAME) {
            parameter<Name>("declarationName")
            parameter<Name>("subPackageName")
        }

        // 使用 --no-prelude 时找不到 std/core 中的 Object 类
        val CORE_OBJECT_NOT_FOUND_WHEN_NO_PRELUDE by error<PsiElement>()

        // 可访问性检查（含 main 提示）
        val ACCESSIBILITY_WITH_MAIN_HINT by error<PsiElement> {
            parameter<String>("declarationKind")
            parameter<Name>("memberName")
            parameter<Visibility>("visibility")
        }

        // 可访问性检查
        val ACCESSIBILITY_ERROR by error<PsiElement> {
            parameter<String>("declarationKind")
            parameter<Visibility>("visibility")
        }

        // 参数个数不匹配（通用）
        val PARAM_COUNT_MISMATCH by error<PsiElement> {
            parameter<Int>("expected")
            parameter<Int>("actual")
        }
    }

    /**
     * 函数语义（Function）
     *
     * 对齐 C++ sema_invalid_return, sema_invalid_subscript_* 等函数相关的语义诊断。
     */
    val FUNCTION by object : DiagnosticGroup("Function") {
        // 无法推断返回类型
        val UNABLE_TO_INFER_RETURN_TYPE by error<PsiElement>()

        // 无法推断泛型函数的类型参数
        val UNABLE_TO_INFER_GENERIC_FUNC by error<PsiElement>()

        // 被调用对象不是函数或构造器
        val INVALID_CALLED_OBJECT by error<PsiElement>()

        // return 必须在函数体内使用
        val INVALID_RETURN by error<PsiElement>()

        // return 不能在 static init 中使用
        val INVALID_RETURN_IN_STATIC_INIT by error<PsiElement>()

        // subscript operator '[]' 只能有一个名为 'value' 的命名参数
        val INVALID_SUBSCRIPT_ASSIGN_PARAMETER by error<CjDeclaration>()

        // subscript operator '[]' 至少需要一个位置参数作为下标
        val INVALID_SUBSCRIPT_ASSIGN_PARAMETER_NUM by error<CjDeclaration>()

        // subscript 赋值操作的返回类型必须是 Unit
        val INVALID_SUBSCRIPT_ASSIGN_RETURN by error<CjDeclaration>()

        // 重载函数不能混合 static 和 non-static
        val STATIC_FUNCTION_OVERLOAD_CONFLICTS by error<CjNamedDeclaration>(PositioningStrategy.ACTUAL_DECLARATION_NAME) {
            parameter<Name>("functionName")
        }

        // mut 函数不能单独作为引用使用
        val USE_MUTABLE_FUNC_ALONE by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<Name>("functionName")
        }

        // unsafe 函数只能被调用，不能作为名称引用
        val UNSAFE_FUNC_CAN_ONLY_BE_CALLED by error<PsiElement>()

        // 基本类型扩展调用歧义
        val AMBIGUOUS_MATCH_PRIMITIVE_EXTEND by error<PsiElement> {
            parameter<Name>("functionName")
            parameter<Collection<Name>>("extendedTypes")
        }

        // optional 参数不能用于某类函数
        val CANNOT_HAVE_DEFAULT_PARAM by error<PsiElement> {
            parameter<String>("functionKind")
        }

        // trailing lambda 不能用于非函数类型参数
        val TRAILING_LAMBDA_CANNOT_USED_FOR_NON_FUNCTION by error<PsiElement> {
            parameter<ConeCangJieType>("paramType")
        }

        // lambda 表达式的参数必须有类型注解
        val LAMBDA_MUST_HAVE_TYPE_ANNOTATION by error<PsiElement>()

        // 捕获可变变量的闭包必须直接调用
        val USE_FUNC_CAPTURE_VAR_ALONE by error<PsiElement> {
            parameter<String>("description")
        }
    }

    /**
     * 表达式语义（Expression）
     *
     * 对齐 C++ sema_invalid_unary_expr, sema_optional_chain_non_optional 等表达式相关的语义诊断。
     */
    val EXPRESSION by object : DiagnosticGroup("Expression") {
        // 无法推断表达式类型
        val UNABLE_TO_INFER_EXPR by error<PsiElement>()

        // 浮点字面量超出范围
        val EXCEED_FLOAT_LITERAL_RANGE by error<PsiElement> {
            parameter<String>("literalText")
        }

        // 浮点字面量过大（警告）
        val FLOAT_LITERAL_TOO_LARGE by warning<PsiElement> {
            parameter<ConeCangJieType>("type")
            parameter<String>("maximum")
        }

        // 浮点字面量过小（警告）
        val FLOAT_LITERAL_TOO_SMALL by warning<PsiElement> {
            parameter<ConeCangJieType>("type")
            parameter<String>("minimum")
        }

        // 无效一元运算符
        val INVALID_UNARY_EXPR by error<PsiElement> {
            parameter<String>("operator")
            parameter<ConeCangJieType>("type")
        }

        // 无效一元运算符（含目标返回类型）
        val INVALID_UNARY_EXPR_WITH_TARGET by error<PsiElement> {
            parameter<String>("operator")
            parameter<ConeCangJieType>("type")
            parameter<ConeCangJieType>("returnType")
        }

        // 无效下标运算符
        val INVALID_SUBSCRIPT_EXPR by error<PsiElement> {
            parameter<ConeCangJieType>("receiverType")
            parameter<String>("indexDescription")
        }

        // 内建下标越界
        val BUILTIN_INDEX_IN_BOUND by error<PsiElement>()

        // 不能赋值给 subscript 表达式
        val CANNOT_ASSIGN_TO_SUBSCRIPT by error<PsiElement>()

        // 不是某类型的成员
        val NOT_MEMBER_OF by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<Name>("memberName")
            parameter<String>("kind")
            parameter<Name>("typeName")
        }

        // 成员未导入
        val MEMBER_NOT_IMPORTED by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<Name>("memberName")
        }

        // 不可赋值给不可变值
        val CANNOT_ASSIGN_TO_IMMUTABLE by error<PsiElement>()

        // 左值不可被赋值
        val UNQUALIFIED_LEFT_VALUE_ASSIGNED by error<PsiElement> {
            parameter<Name>("name")
        }

        // or pattern 中的分支种类不一致
        val DIFFERENT_OR_PATTERN by error<PsiElement> {
            parameter<String>("description")
        }

        // or pattern 中不能引入变量
        val VAR_IN_OR_PATTERN by error<PsiElement>()

        // or condition 中不能引入变量
        val VAR_IN_OR_CONDITION by error<PsiElement>()

        // 不可达模式（警告）
        val UNREACHABLE_PATTERN by warning<PsiElement>()

        // 带参数的 enum 构造器必须提供参数
        val ENUM_CONSTRUCTOR_WITH_PARAM_MUST_HAVE_ARGS by error<PsiElement> {
            parameter<Name>("constructorName")
        }

        // 不能对非 optional 类型使用 optional chaining
        val OPTIONAL_CHAIN_NON_OPTIONAL by error<PsiElement> {
            parameter<ConeCangJieType>("type")
        }

        // 变量初始化之前不能被捕获
        val CAPTURE_BEFORE_INITIALIZATION by error<PsiElement> {
            parameter<Name>("variableName")
        }

        // 捕获变量时，中间作用域存在同名遮蔽变量
        val CAPTURE_HAS_SHADOW_VARIABLE by warning<PsiElement> {
            parameter<Name>("variableName")
        }

        // 常量模式中不能使用字符串插值
        val INTERPOLATION_IN_CONST_PATTERN by error<PsiElement>()

        // 包名不能独立引用
        val CANNOT_REF_TO_PKG_NAME by error<PsiElement>()

        // 需要导入才能使用某表达式
        val USE_EXPR_WITHOUT_IMPORT by error<PsiElement> {
            parameter<FqName>("importPath")
            parameter<String>("exprKind")
        }
    }

    /**
     * 泛型深层检查（GenericDeep）
     *
     * 对齐 C++ sema_generic_type_inconsistent, sema_generic_argument_no_match 等。
     */
    val GENERIC_DEEP by object : DiagnosticGroup("GenericDeep") {
        // 泛型类型替换不一致
        val GENERIC_TYPE_INCONSISTENT by error<PsiElement> {
            parameter<Name>("typeParameterName")
        }

        // 类型参数个数不匹配
        val GENERIC_ARGUMENT_NO_MATCH by error<PsiElement>()

        // 泛型类型实参不满足声明侧约束
        val GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT by error<PsiElement> {
            parameter<ConeCangJieType>("actualType")
            parameter<ConeCangJieType>("upperBound")
            parameter<ConeCangJieType>("genericType")
        }

        // 子类型约束不能比父类宽松
        val GENERIC_CONSTRAINT_NOT_LOOSER by error<PsiElement>()

        // 泛型实例化沿声明链无限展开
        val GENERIC_INFINITE_INSTANTIATION by error<PsiElement>()

        // 泛型实例化导致函数歧义
        val GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS by error<PsiElement> {
            parameter<Name>("instantiation")
            parameter<Name>("functionName")
        }

        // 泛型参数存在与类无关的上界递归引用
        val GENERIC_PARAM_EXIST_IN_CLASS_IRRELEVANT_UPPERBOUND_RECURSIVELY by error<PsiElement> {
            parameter<Name>("typeParameterName")
            parameter<ConeCangJieType>("upperBound")
        }

        // 泛型参数直接递归绑定
        val GENERIC_PARAM_DIRECTLY_RECURSIVE by error<PsiElement> {
            parameter<Name>("typeParameterName")
            parameter<Name>("boundName")
        }

        // 上界必须是 class 或 interface
        val UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE by error<PsiElement> {
            parameter<ConeCangJieType>("upperBound")
            parameter<Name>("typeParameterName")
        }

        // static 成员不能依赖泛型参数（Java 类型中）
        val GENERIC_STATIC_ACCESS by error<PsiElement>()

        // 基本类型不能作为 @Java 泛型参数
        val PRIMITIVE_TYPE_AS_GENERICS_ARG by error<PsiElement>()

        // 通过 extend 满足约束的类型不能用于 @Java 泛型
        val MEET_CONSTRAINT_INDIRECTLY by error<PsiElement>()

        // @Java 类型中泛型上界也必须是 @Java 类型
        val GENERIC_UPPER_BOUNDS_MUST_BE_JAVA_IN_JAVA by error<PsiElement>()
    }

    /**
     * 继承深层检查（InheritanceDeep）
     *
     * 对齐 C++ sema_inherit_member_kind_inconsistent, sema_cannot_inherit_sealed 等。
     */
    val INHERITANCE_DEEP by object : DiagnosticGroup("InheritanceDeep") {
        // 成员类型（函数/属性）与同名父成员不一致
        val INHERIT_MEMBER_KIND_INCONSISTENT by error<PsiElement> {
            parameter<String>("memberKind")
            parameter<Name>("memberName")
            parameter<String>("superMemberKind")
            parameter<Name>("containerName")
        }

        // extend 成员与被扩展类型成员 static 状态不一致。
        val STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME by error<PsiElement> {
            parameter<String>("memberStaticKind")
            parameter<Name>("memberName")
            parameter<String>("superMemberStaticKind")
            parameter<String>("containerKind")
        }

        // 成员变量不能遮蔽父类型中的成员变量
        val MEMBER_VARIABLE_CAN_NOT_SHADOW by error<PsiElement> {
            parameter<Name>("memberName")
        }

        // 父成员不是 open/abstract/interface 成员，子类不能覆盖
        val CANNOT_OVERRIDE by error<PsiElement> {
            parameter<String>("memberKind")
            parameter<Name>("memberName")
        }

        // extend 实现接口时缺少必须实现的抽象函数/属性。
        val NEED_MEMBER_IMPLEMENTATION by error<PsiElement> {
            parameter<String>("extendName")
        }

        // extend 继承接口 default 成员时，必须在 extend 声明中显式实现。
        val INTERFACE_MEMBER_MUST_BE_IMPLEMENTED by error<PsiElement> {
            parameter<String>("memberKind")
            parameter<Name>("memberName")
            parameter<String>("extendName")
        }

        // struct/extend 实现 interface 时函数 mut 修饰不一致。
        val INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE by error<PsiElement>()

        // 从多个父类型继承的同名成员声明类型不一致
        val INHERIT_SUPER_MEMBER_KIND_INCONSISTENT by error<PsiElement> {
            parameter<Name>("memberName")
        }

        // 从多个父类型继承的同名成员类型不一致且无子类型关系
        val INHERIT_MEMBER_TYPE_INCONSISTENT by error<PsiElement> {
            parameter<String>("aspect")
            parameter<String>("memberKind")
            parameter<Name>("memberName")
        }

        // 抽象类不能包含未实现的 static 函数/属性
        val INHERIT_ABSTRACT_CLASS_STATIC_UNIMPLEMENT_FUNC by error<PsiElement> {
            parameter<Name>("className")
            parameter<String>("memberKind")
            parameter<Name>("memberName")
        }

        // open/abstract 成员的可见性必须是 public 或 protected
        val INVALID_MEMBER_VISIBILITY_IN_CLASS by error<PsiElement> {
            parameter<String>("modifier")
            parameter<String>("memberKind")
        }

        // 不能继承 sealed 类
        val CANNOT_INHERIT_SEALED by error<CjTypeReference> {
            parameter<String>("verb")
            parameter<String>("kind")
            parameter<String>("sealedKind")
            parameter<Name>("sealedName")
        }

        // 用户定义的声明不支持继承/实现/扩展 ThreadContext
        val INHERIT_THREAD_CONTEXT_INVALID by error<PsiElement> {
            parameter<Name>("declarationName")
        }

        // 继承/实现/扩展 ThreadContext 的声明不能标记为 open
        val INHERIT_THREAD_CONTEXT_NOT_OPEN by error<PsiElement> {
            parameter<Name>("declarationName")
        }

        // open 函数返回 This 类型时，override 也必须保持返回 This
        val INHERIT_NOT_RETURN_THIS by error<CjNamedDeclaration>(PositioningStrategy.ACTUAL_DECLARATION_NAME)
    }

    /**
     * Spawn 语义
     *
     * 对齐 C++ sema_spawn_arg_invalid, sema_spawn_arg_no_effect。
     */
    val SPAWN by object : DiagnosticGroup("Spawn") {
        // spawn 的参数无效，不允许自定义 ThreadContext 类型
        val SPAWN_ARG_INVALID by error<PsiElement>()

        // spawn 参数在当前后端不生效（警告）
        val SPAWN_ARG_NO_EFFECT by warning<PsiElement>()
    }

    /**
     * 接口语义（Interface）
     *
     * 对齐 C++ sema_interface_call_with_unimplemented_call。
     */
    val INTERFACE by object : DiagnosticGroup("Interface") {
        // 静态调用包含未实现的静态成员
        val INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL by error<PsiElement> {
            parameter<String>("memberKind")
            parameter<Name>("memberName")
        }
    }

    /**
     * 类/接口/结构体语义（ClassStructSemantics）
     *
     * 对齐 C++ sema_type_uninitialized_static_field, sema_non_abstract_class_cannot_be_sealed 等。
     */
    val CLASS_STRUCT by object : DiagnosticGroup("ClassStructSemantics") {
        // static 成员变量未初始化
        val TYPE_UNINITIALIZED_STATIC_FIELD by error<PsiElement> {
            parameter<Name>("fieldName")
        }

        // 实例成员不能在 finalizer 中使用
        val INSTANCE_FUNC_CANNOT_BE_USED_IN_FINALIZER by error<PsiElement> {
            parameter<String>("memberKind")
        }

        // finalizer 不能出现在 open/abstract class 中
        val FINALIZER_FORBIDDEN_IN_CLASS by error<PsiElement> {
            parameter<Name>("className")
            parameter<String>("classKind")
        }

        // finalizer/constructor/getter/setter 不支持柯里化参数列表
        val CANNOT_CURRYING by error<PsiElement> {
            parameter<String>("declarationKind")
        }

        // getter 不能声明参数
        val CANNOT_HAVE_PARAMETER by error<PsiElement> {
            parameter<String>("declarationKind")
        }

        // finalizer 不能声明泛型参数
        val FORBID_GENERIC_FINALIZER by error<PsiElement> {
            parameter<Name>("finalizerName")
        }

        // 非抽象类不能使用 sealed 修饰
        val NON_ABSTRACT_CLASS_CANNOT_BE_SEALED by error<CjNamedDeclaration>(PositioningStrategy.ACTUAL_DECLARATION_NAME)

        // static 成员不能依赖泛型参数
        val STATIC_VARIABLE_USE_GENERIC_PARAMETER by error<PsiElement> {
            parameter<Name>("typeParameterName")
        }

        // @C struct 不能实现接口
        val CSTRUCT_CANNOT_IMPL_INTERFACES by error<PsiElement>()

        // 不能同时导出两个同名的 private 声明
        val EXPORT_SAME_PRIVATE_DECL by error<PsiElement>()
    }

    /**
     * Extend 补充诊断
     *
     * 对齐 C++ sema_extend_function_cannot_overridden, sema_extend_member_cannot_shadow 等
     * 原有 EXTEND group 未覆盖的条目。
     */
    val EXTEND_EXTRA by object : DiagnosticGroup("ExtendExtra") {
        // extend 中不能 override 超类型的函数
        val EXTEND_FUNCTION_CANNOT_OVERRIDDEN by error<PsiElement> {
            parameter<String>("memberKind")
            parameter<Name>("memberName")
        }

        // extend 成员不能遮蔽被扩展类型的成员
        val EXTEND_MEMBER_CANNOT_SHADOW by error<PsiElement> {
            parameter<Name>("memberName")
            parameter<Name>("targetTypeName")
        }

        // extend 中出现非法成员（只允许函数、属性、关联类型）
        val EXTEND_ILLEGAL_MEMBER by error<PsiElement>()

        // 无法确定 extend 的检查顺序
        val EXTEND_CHECK_SEQUENCE_CANNOT_DECIDE by error<PsiElement>()

        // 导出的 extend 不能间接导出非导出 extend 的函数
        val EXPORT_EXTEND_DEPEND_NON_EXPORT_EXTEND by error<PsiElement> {
            parameter<Collection<Name>>("functionNames")
        }

        // @Java 类型不能被 extend
        val EXTEND_A_JAVA_TYPE by error<CjTypeReference>()

        // extend 引用目标不能是 @JavaImpl
        val EXTEND_REF_TARGET_CANNOT_BE_JAVA_IMPL by error<PsiElement>()

        // 类型不能 extend 导入的接口
        val TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE by error<PsiElement> {
            parameter<String>("kind")
            parameter<Name>("typeName")
        }
    }

    /**
     * 属性语义（Property）
     *
     * 对齐 C++ sema_property_must_have_accessors 等。
     */
    val PROPERTY by object : DiagnosticGroup("Property") {
        // 属性必须有访问器
        val PROPERTY_MUST_HAVE_ACCESSORS by error<CjDeclaration>()

        // 不可变属性不能有 setter
        val IMMUTABLE_PROPERTY_WITH_SETTER by error<CjDeclaration>()

        // 属性应该有 mut 修饰符（继承语义）
        val PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_MUT by error<PsiElement> {
            parameter<Name>("propertyName")
        }

        // 属性应该是不可变的（继承语义）
        val PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_IMMUT by error<PsiElement> {
            parameter<Name>("propertyName")
        }

        // 属性必须同时实现接口属性的 getter/setter
        val PROPERTY_MUST_IMPLEMENT_BOTH by error<PsiElement> {
            parameter<Name>("propertyName")
        }
    }

    /**
     * Const 声明语义
     *
     * 对齐 C++ sema_expect_const 等。
     */
    val CONST_DECLARATION by object : DiagnosticGroup("ConstDeclaration") {
        // 期望 const 修饰
        val EXPECT_CONST by error<PsiElement> {
            parameter<String>("kind")
        }

        // const 函数内不能定义 var 变量
        val CANNOT_DEFINE_VAR_IN_CONST_FUNCTION by error<PsiElement>()

        // 没有 const 构造器就不能定义 const 成员函数
        val NO_CONST_INIT by error<PsiElement>()

        // 类包含 var 成员时不能定义 const 构造器
        val CLASS_CONST_INIT_WITH_VAR by error<PsiElement>()
    }

    /**
     * Annotation 补充诊断
     *
     * 对齐 C++ sema_annotation_arg_target 等原有 ANNOTATION group 未覆盖的条目。
     */
    val ANNOTATION_EXTRA by object : DiagnosticGroup("AnnotationExtra") {
        // @Annotation 只能有一个名为 target 的命名参数
        val ANNOTATION_ARG_TARGET by error<PsiElement>()

        // @Annotation 的参数应该是数组字面量
        val ANNOTATION_ARG_TARGET_ARRAY_LIT by error<PsiElement>()

        // @Annotation 修饰的非 public 类在运行时不可见（警告）
        val ANNOTATION_NON_PUBLIC by warning<PsiElement>()

        // 不能使用自定义注解
        val ANNOTATION_CUSTOM_PLACE by error<PsiElement>()

        // 注解参数个数错误
        val ANNOTATION_ERROR_ARG_NUM by error<PsiElement> {
            parameter<String>("annotationName")
            parameter<String>("expectedArgs")
        }

        // 注解参数范围错误
        val ANNOTATION_ERROR_ARG_RANGE by error<PsiElement> {
            parameter<String>("annotationName")
            parameter<String>("supportedArgs")
        }

        // 注解只能修饰特定对象
        val ANNOTATION_ERROR_OBJECT by error<PsiElement> {
            parameter<String>("annotationName")
            parameter<String>("validTargets")
        }

        // 不能在此处使用注解（Java 互操作）
        val CANNOT_USE_ANNOTATION_JFFI by error<PsiElement>()

        // 注解不适用于某目标（Java 互操作）
        val ANNOTATION_NOT_APPLICABLE_JFFI by error<PsiElement> {
            parameter<String>("annotationName")
            parameter<String>("target")
        }
    }

    /**
     * inout 语义
     *
     * 对齐 C++ sema_inout_* 系列。
     */
    val INOUT by object : DiagnosticGroup("Inout") {
        // inout 表达式不能是 CString 或零大小类型
        val INOUT_MODIFY_CSTRING_OR_ZEROSIZED by error<PsiElement> {
            parameter<ConeCangJieType>("type")
        }

        // inout 表达式的类型必须满足 CType 约束
        val INOUT_MODIFY_NON_CTYPE by error<PsiElement>()

        // inout 只能修饰 var 变量
        val INOUT_MUST_BE_VAR_VARIABLE by error<PsiElement>()

        // inout 变量不能直接或间接来自 class 实例
        val INOUT_MODIFY_HEAP_VARIABLE by error<PsiElement>()

        // inout 只能在 CFunc 调用中使用
        val INOUT_CAN_ONLY_USED_IN_CFUNC_CALLING by error<PsiElement>()

        // inout 参数类型不匹配
        val INOUT_MISMATCH by error<PsiElement> {
            parameter<ConeCangJieType>("type")
        }

        // inout 参数必须是可变左值
        val INVALID_INOUT_ARGUMENT by error<PsiElement>()

        // 同一个参数不能重复标记 inout
        val DUPLICATE_INOUT_ARGUMENT by error<PsiElement>()
    }

    /**
     * VArray 深层语义
     *
     * 对齐 C++ sema_varray_args_number_mismatch 等原有 TYPE_CHECK 未覆盖的条目。
     */
    val VARRAY_EXTRA by object : DiagnosticGroup("VArrayExtra") {
        // VArray 构造器只接受一个参数
        val VARRAY_ARGS_NUMBER_MISMATCH by error<PsiElement>()

        // VArray 只接受一个 Int64 类型的下标索引
        val VARRAY_SUBSCRIPT_NUM by error<PsiElement>()

        // CFunc 的返回类型不能是 VArray
        val VARRAY_IN_CFUNC by error<PsiElement>()

        // VArray 直接或间接包含不支持的类型
        val VARRAY_ARG_TYPE_WITH_REFTYPE by error<PsiElement> {
            parameter<ConeCangJieType>("type")
        }
    }

    /**
     * Effects 补充诊断
     *
     * 对齐 C++ sema_resumption_handle_type_error 等原有 EFFECTS group 未覆盖的条目。
     */
    val EFFECTS_EXTRA by object : DiagnosticGroup("EffectsExtra") {
        // resumption 类型必须扩展 effect.Resumption
        val RESUMPTION_HANDLE_TYPE_ERROR by error<PsiElement>()

        // resumption 的返回类型与 try block 不匹配
        val RESUMPTION_INCORRECT_RETURN_TYPE by error<PsiElement> {
            parameter<ConeCangJieType>("resumptionType")
            parameter<ConeCangJieType>("tryBlockType")
        }

        // resumption 参数类型与 command 结果类型不匹配
        val COMMAND_RESUMPTION_MISMATCH by error<PsiElement> {
            parameter<ConeCangJieType>("resumptionParamType")
            parameter<ConeCangJieType>("commandResultType")
        }

        // resume 的 resumption 类型必须是 core.Resumption<T>
        val RESUME_WRONG_RESUMPTION_TYPE by error<PsiElement> {
            parameter<ConeCangJieType>("actualType")
        }

        // try/handle block 中不允许 return 语句
        val RETURN_IN_TRY_HANDLE_BLOCK by error<PsiElement>()

        // 无用的 command 类型（警告）
        val USELESS_COMMAND_TYPE by warning<PsiElement>()
    }

    /**
     * @Deprecated 语义
     *
     * 对齐 C++ sema_deprecated_error, sema_deprecation_weakening 等。
     */
    val DEPRECATED by object : DiagnosticGroup("Deprecated") {
        // 调用了被 @Deprecated 标记的声明（错误级别）
        val DEPRECATED_ERROR by error<PsiElement> {
            parameter<String>("kind")
            parameter<Name>("name")
            parameter<String>("message")
            parameter<String>("replacement")
        }

        // 调用了被 @Deprecated 标记的声明（警告级别）
        val DEPRECATED_WARNING by warning<PsiElement> {
            parameter<String>("kind")
            parameter<Name>("name")
            parameter<String>("message")
            parameter<String>("replacement")
        }

        // @Deprecated 的严格度不能被继承者减弱
        val DEPRECATION_WEAKENING by error<PsiElement>()

        // override 的成员应标记 @Deprecated（错误级别）
        val DEPRECATION_OVERRIDE_ERROR by error<PsiElement> {
            parameter<String>("kind")
            parameter<Name>("name")
        }

        // override 的成员应标记 @Deprecated（警告级别）
        val DEPRECATION_OVERRIDE_WARNING by warning<PsiElement> {
            parameter<String>("kind")
            parameter<Name>("name")
        }

        // redef 的成员应标记 @Deprecated（错误级别）
        val DEPRECATION_REDEF_ERROR by error<PsiElement> {
            parameter<String>("kind")
            parameter<Name>("name")
        }

        // redef 的成员应标记 @Deprecated（警告级别）
        val DEPRECATION_REDEF_WARNING by warning<PsiElement> {
            parameter<String>("kind")
            parameter<Name>("name")
        }
    }

    /**
     * common/specific 跨平台匹配语义
     *
     * 对齐 C++ sema_common_open_class_no_init, sema_multiple_common_implementations 等。
     */
    val COMMON_SPECIFIC by object : DiagnosticGroup("CommonSpecific") {
        // common open class 必须显式实现构造器
        val COMMON_OPEN_CLASS_NO_INIT by error<PsiElement> {
            parameter<Name>("className")
        }

        // common 声明有多个 specific 实现
        val MULTIPLE_COMMON_IMPLEMENTATIONS by error<PsiElement> {
            parameter<String>("kind")
        }

        // common extend 的 private 成员冲突
        val COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS by error<PsiElement> {
            parameter<Name>("extendName")
            parameter<String>("memberKind")
            parameter<Name>("memberName")
        }

        // common 和 private 修饰符冲突
        val COMMON_DIRECT_EXTENSION_HAS_COMMON_PRIVATE_MEMBERS by error<PsiElement> {
            parameter<String>("memberKind")
            parameter<Name>("memberName")
        }

        // specific 找不到匹配的 common 声明
        val NOT_MATCHED by error<PsiElement> {
            parameter<Name>("declarationName")
            parameter<String>("kind")
            parameter<String>("matchKind")
        }

        // specific var 不能匹配 common let
        val SPECIFIC_VAR_NOT_MATCH_LET by error<PsiElement> {
            parameter<Name>("specificName")
            parameter<Name>("commonName")
        }

        // specific init 不能实现 primary common constructor
        val SPECIFIC_INIT_COMMON_PRIMARY_CONSTRUCTOR by error<PsiElement>()

        // specific 声明类型与 common 不一致
        val SPECIFIC_HAS_DIFFERENT_KIND by error<PsiElement> {
            parameter<String>("specificKind")
            parameter<String>("commonKind")
        }

        // specific primary constructor 参数必须也是成员变量声明
        val SPECIFIC_PRIMARY_UNMATCHED_VAR_DECL by error<PsiElement>()

        // exhaustive common 不能匹配 non-exhaustive specific
        val COMMON_NON_EXHAUSTIVE_PLATFORM_EXHAUSTIVE_MISMATCH by error<PsiElement> {
            parameter<String>("commonKind")
            parameter<String>("specificKind")
        }

        // specific 类型与 common 不相等
        val SPECIFIC_HAS_DIFFERENT_TYPE by error<PsiElement> {
            parameter<String>("kind")
        }

        // specific 成员必须有函数体
        val SPECIFIC_MEMBER_MUST_HAVE_IMPLEMENTATION by error<PsiElement> {
            parameter<String>("memberKind")
            parameter<String>("containerKind")
        }

        // specific 修饰符与 common 不匹配
        val SPECIFIC_HAS_DIFFERENT_MODIFIER by error<PsiElement> {
            parameter<String>("kind")
        }

        // specific 注解与 common 不匹配
        val SPECIFIC_HAS_DIFFERENT_ANNOTATION by error<PsiElement> {
            parameter<String>("kind")
        }

        // 某些注解不允许出现在 specific 声明上
        val SPECIFIC_HAS_DEPRECATED_ANNOTATION by error<PsiElement> {
            parameter<Name>("annotationName")
            parameter<String>("kind")
            parameter<Name>("name")
        }

        // common 和 specific 两侧不能同时有默认参数值
        val CJMP_PARAMETER_DEFAULT_VALUE_BOTH_SIDES by error<PsiElement>()

        // specific 函数参数与 common 不匹配
        val SPECIFIC_HAS_DIFFERENT_PARAMETER by error<PsiElement>()

        // specific 超类型与 common 不匹配
        val SPECIFIC_HAS_DIFFERENT_SUPER_TYPE by error<PsiElement> {
            parameter<String>("kind")
        }

        // specific extend 冲突
        val SPECIFIC_HAS_DUPLICATE_EXTENSIONS by error<PsiElement> {
            parameter<Name>("extendName")
        }

        // main 函数不能在 common 包中使用
        val COMMON_PACKAGE_HAS_MAIN by error<PsiElement>()

        // common static let 不能在 static init 中初始化
        val COMMON_STATIC_LET_CANT_BE_INITIALIZED_IN_STATIC_INIT by error<PsiElement> {
            parameter<Name>("variableName")
        }

        // 不能给 common 不可变变量赋值
        val COMMON_ASSIGN_TO_COMMON_IMMUTABLE_IN_CTOR by error<PsiElement> {
            parameter<Name>("variableName")
        }

        // common/specific 抽象类成员必须有明确修饰符
        val CJMP_ABSTRACT_CLASS_MEMBER_HAS_NO_EXPLICIT_MODIFIER by error<PsiElement> {
            parameter<Name>("className")
            parameter<String>("memberKind")
            parameter<String>("modifier")
        }

        // abstract 成员不能有函数体
        val EXPLICITLY_ABSTRACT_CAN_NOT_HAVE_BODY by error<PsiElement> {
            parameter<String>("memberKind")
        }

        // 只有 common/specific 类才能有显式 abstract 成员
        val EXPLICITLY_ABSTRACT_ONLY_FOR_CJMP_ABSTRACT_CLASS by error<PsiElement> {
            parameter<String>("memberKind")
        }

        // open common 不能用 abstract specific 覆盖
        val OPEN_ABSTRACT_SPECIFIC_CAN_NOT_REPLACE_OPEN_COMMON by error<PsiElement> {
            parameter<String>("commonKind")
            parameter<String>("specificKind")
        }

        // specific 抽象类不能有非 specific 的抽象成员
        val CJMP_NON_SPECIFIC_ABSTRACT_MEMBER_IN_SPECIFIC_CLASS by error<PsiElement> {
            parameter<Name>("className")
            parameter<String>("memberKind")
        }

        // common/specific 带泛型的声明不能使用 @Frozen
        val COMMON_GENERIC_FROZEN_NOT_SUPPORTED by error<PsiElement> {
            parameter<String>("kind")
        }

        // common/specific 泛型重命名暂不支持
        val COMMON_GENERIC_RENAME_NOT_SUPPORTED by error<PsiElement>()

        // 某些注解不允许在 common/specific 声明上使用
        val COMMON_SPECIFIC_ANNOTATION_NOT_ALLOWED by error<PsiElement> {
            parameter<Name>("annotationName")
        }
    }

    /**
     * Java 互操作语义（@Java 注解相关）
     *
     * 对齐 C++ sema_java_incorrect_use_between_types 等。
     */
    val JAVA_INTEROP by object : DiagnosticGroup("JavaInterop") {
        // @Java["ext"] 类型只能在同样有 @Java["ext"] 的声明内使用
        val JAVA_INCORRECT_USE_BETWEEN_TYPES by error<PsiElement>()

        // @Java 声明中的类型必须满足 JType 约束
        val JAVA_NON_JTYPE by error<PsiElement> {
            parameter<String>("typePosition")
            parameter<String>("memberKind")
            parameter<Name>("memberName")
        }

        // @Java 声明中的类型不能是 Unit
        val JAVA_INVALID_UNIT by error<PsiElement> {
            parameter<String>("typePosition")
            parameter<String>("memberKind")
            parameter<Name>("memberName")
        }

        // 只有 @Java["ext"] 类型才能从 @Java["ext"] 类型继承
        val JAVA_APP_INHERIT_EXT by error<PsiElement> {
            parameter<String>("verb")
        }

        // @Java 注解类型中不支持某些声明
        val JAVA_UNSUPPORTED_DECL by error<PsiElement> {
            parameter<String>("declKind")
            parameter<String>("memberKind")
            parameter<Name>("memberName")
        }

        // 声明应该有 @Java 注解
        val MISSING_JAVA_INTEROP_ANNOTATION by error<PsiElement> {
            parameter<String>("kind")
            parameter<Name>("name")
        }

        // @Java 类型中不能使用带类型参数的 shadow
        val SHADOW_CANNOT_IN_TYPE_ARGS by error<PsiElement> {
            parameter<Name>("name")
            parameter<Name>("fieldName")
            parameter<ConeCangJieType>("superType")
        }

        // Java 互操作中的类型参数必须满足 JType 约束
        val UNSUPPORTED_TYPE_ARGUMENT_IN_JAVA_INTEROP by error<PsiElement>()

        // @Java 注解的接口中 static 函数必须有函数体
        val STATIC_MEMBER_IN_INTERFACE_MUST_HAS_BODY by error<PsiElement>()

        // @Java 类型不能同时标注 @Annotation
        val DEFINE_JAVA_ANNOTATION by error<PsiElement>()

        // 导入的 Java 注解只能用于 @Java 类型
        val INVALID_USE_OF_JAVA_ANNOTATION by error<PsiElement>()

        // 只有导入的 Java 注解才能用于 @Java 类型
        val INVALID_USE_OF_ANNOTATION_JFFI by error<PsiElement>()

        // 不能存储 Java 互操作类型的对象
        val VARIABLE_OF_JAVA_TYPE by error<PsiElement> {
            parameter<String>("kind")
            parameter<ConeCangJieType>("type")
        }

        // 不能用 Java 互操作类型实例化泛型
        val GENERIC_PARAMETER_OF_JAVA_TYPE by error<PsiElement> {
            parameter<Name>("genericName")
            parameter<ConeCangJieType>("type")
        }

        // Java 互操作特性暂不支持
        val JAVA_INTEROP_NOT_SUPPORTED by error<PsiElement> {
            parameter<String>("featureName")
        }
    }

    /**
     * Java mirror 语义
     *
     * 对齐 C++ sema_java_mirror_* 系列。
     */
    val JAVA_MIRROR by object : DiagnosticGroup("JavaMirror") {
        // java-mirrored 构造器参数类型必须是 @JavaMirror 类型
        val JAVA_MIRROR_CTOR_ARG_MUST_BE_JAVA_MIRROR by error<PsiElement>()

        // java-mirrored 函数参数类型必须是 @JavaMirror 类型
        val JAVA_MIRROR_METHOD_ARG_MUST_BE_JAVA_MIRROR by error<PsiElement>()

        // java-mirrored 函数返回类型不支持
        val JAVA_MIRROR_METHOD_RET_UNSUPPORTED by error<PsiElement> {
            parameter<ConeCangJieType>("returnType")
            parameter<String>("classKind")
        }

        // java-mirrored 声明的属性必须是 @JavaMirror 类型
        val JAVA_MIRROR_PROP_MUST_BE_JAVA_MIRROR by error<PsiElement>()

        // 父声明只能被 @JavaMirror 或 @JavaImpl 注解的声明继承
        val JAVA_MIRROR_SUBTYPE_MUST_BE_ANNOTATED by error<PsiElement> {
            parameter<Name>("superName")
        }

        // @JavaMirror 声明不能继承纯仓颉类型
        val JAVA_MIRROR_CANNOT_INHERIT_PURE_CANGJIE_TYPE by error<PsiElement>()

        // @JavaImpl 声明不能继承纯仓颉类型
        val JAVA_IMPL_CANNOT_INHERIT_PURE_CANGJIE_TYPE by error<PsiElement>()

        // @JavaImpl 声明必须继承 @JavaMirror 声明
        val JAVA_MIRROR_SUBTYPE_ANNO_MUST_INHERIT_MIRROR by error<PsiElement>()

        // @JavaMirror 类不能被 extend 添加接口
        val JAVA_MIRROR_CANNOT_BE_EXTENDED_WITH_INTERFACE by error<PsiElement>()

        // @JavaImpl 类不能被 extend 添加接口
        val JAVA_IMPL_CANNOT_BE_EXTENDED_WITH_INTERFACE by error<PsiElement>()

        // Java 声明重定义
        val JAVA_IMPL_REDEFINITION by error<PsiElement> {
            parameter<Name>("declarationName")
        }

        // 使用 Java 互操作必须导入 interoplib.interop
        val JAVA_MIRROR_INTEROPLIB_MUST_BE_IMPORTED by error<PsiElement>()

        // @JavaHasDefault 不能有参数
        val JAVA_HAS_DEFAULT_ANNOTATION_ARGS by error<PsiElement>()

        // @JavaHasDefault 只能用于 @JavaMirror 接口方法
        val JAVA_HAS_DEFAULT_ANNOTATION_IS_IN_WRONG_PLACE by error<PsiElement>()

        // @JavaHasDefault 和 static 不能同时使用
        val JAVA_HAS_DEFAULT_CONFLICT_WITH_STATIC by error<PsiElement>()
    }

    /**
     * CJMapping（Java）语义
     *
     * 对齐 C++ sema_cjmapping_* 系列。
     */
    val CJMAPPING by object : DiagnosticGroup("CJMapping") {
        // cangjie mirror struct 类型的泛型不支持
        val CJMAPPING_STRUCT_GENERIC_NOT_SUPPORTED by error<PsiElement> {
            parameter<String>("genericDescription")
        }

        // cangjie mirror struct 继承接口不支持
        val CJMAPPING_STRUCT_INHERITANCE_INTERFACE_NOT_SUPPORTED by error<PsiElement>()

        // cangjie mirror 声明类型不支持
        val CJMAPPING_DECL_NOT_SUPPORTED by error<PsiElement> {
            parameter<String>("kind")
        }

        // cangjie mirror 成员函数参数类型不支持
        val CJMAPPING_METHOD_ARG_NOT_SUPPORTED by error<PsiElement>()

        // cangjie mirror 函数返回类型不支持
        val CJMAPPING_METHOD_RET_UNSUPPORTED by error<PsiElement> {
            parameter<ConeCangJieType>("returnType")
            parameter<String>("containerKind")
        }

        // 实例配置格式不正确
        val CJ_MAPPING_GENERIC_METHOD_NOT_GET_INSTANCE_CONFIG by error<PsiElement> {
            parameter<String>("configName")
        }
    }

    /**
     * Objective-C 互操作语义
     *
     * 对齐 C++ sema_objc_* 系列。
     */
    val OBJC_INTEROP by object : DiagnosticGroup("ObjCInterop") {
        // @ObjCMirror/@ObjCImpl 构造器参数类型必须兼容 Objective-C
        val OBJC_INTEROP_CTOR_PARAM_MUST_BE_OBJC_COMPATIBLE by error<PsiElement> {
            parameter<String>("declarationKind")
        }

        // 方法参数类型必须兼容 Objective-C
        val OBJC_INTEROP_METHOD_PARAM_MUST_BE_OBJC_COMPATIBLE by error<PsiElement> {
            parameter<String>("declarationKind")
        }

        // 方法返回类型必须兼容 Objective-C
        val OBJC_INTEROP_METHOD_RET_MUST_BE_OBJC_COMPATIBLE by error<PsiElement> {
            parameter<String>("declarationKind")
        }

        // 属性类型必须兼容 Objective-C
        val OBJC_INTEROP_PROP_MUST_BE_OBJC_COMPATIBLE by error<PsiElement> {
            parameter<String>("declarationKind")
        }

        // 字段类型必须兼容 Objective-C
        val OBJC_INTEROP_FIELD_MUST_BE_OBJC_COMPATIBLE by error<PsiElement> {
            parameter<String>("declarationKind")
        }

        // ObjC mirror 不能继承其他超类型
        val OBJC_MIRROR_DECL_CANNOT_INHERIT by error<PsiElement>()

        // ObjC mirror 子类型不能多重继承
        val OBJC_MIRROR_SUBTYPE_CANNOT_MULTIPLE_INHERIT by error<PsiElement>()

        // ObjC mirror 子类型必须有 @ObjCMirror 或 @ObjCImpl 注解
        val OBJC_MIRROR_SUBTYPE_MUST_BE_ANNOTATED by error<PsiElement>()

        // @ObjCImpl 声明必须继承 @ObjCMirror
        val OBJC_MIRROR_SUBTYPE_MUST_INHERIT_MIRROR by error<PsiElement>()

        // @ObjCMirror 声明不能继承非 @ObjCMirror 声明
        val OBJC_MIRROR_MUST_INHERIT_MIRROR by error<PsiElement>()

        // 使用 ObjC 互操作必须导入 interoplib.objc
        val OBJC_MIRROR_INTEROPLIB_MUST_BE_IMPORTED by error<PsiElement>()

        // Objective-C 互操作特性暂不支持
        val OBJC_INTEROP_NOT_SUPPORTED by error<PsiElement> {
            parameter<String>("featureName")
        }

        // ObjCPointer 只能用于 ObjC 兼容类型
        val OBJC_POINTER_ARGUMENT_MUST_BE_OBJC_COMPATIBLE by error<PsiElement>()

        // ObjC mirror 顶层函数参数类型必须兼容 ObjC
        val OBJC_INTEROP_TOPLEVEL_PARAM_MUST_BE_OBJC_COMPATIBLE by error<PsiElement> {
            parameter<String>("functionName")
        }

        // ObjC mirror 顶层函数返回类型必须兼容 ObjC
        val OBJC_INTEROP_TOPLEVEL_RET_MUST_BE_OBJC_COMPATIBLE by error<PsiElement> {
            parameter<String>("functionName")
        }

        // 多参数方法必须有 @ForeignName 注解
        val OBJC_METHOD_MUST_HAVE_FOREIGN_NAME by error<PsiElement> {
            parameter<String>("declarationKind")
            parameter<Name>("methodName")
        }

        // 多参数构造器必须有 @ForeignName 注解
        val OBJC_CTOR_MUST_HAVE_FOREIGN_NAME by error<PsiElement> {
            parameter<String>("declarationKind")
        }

        // 只能在 ObjC 兼容的函数类型上使用
        val OBJC_FUNC_ARGUMENT_MUST_BE_OBJC_COMPATIBLE by error<PsiElement> {
            parameter<String>("description")
        }

        // ObjC 函数类型的 call 属性只能直接调用
        val OBJC_FUNC_CALL_PROPERTY_CAN_ONLY_BE_CALLED by error<PsiElement> {
            parameter<String>("description")
        }

        // @ObjCImpl class 必须有 @ObjCMirror super class
        val OBJC_IMPL_MUST_HAVE_OBJC_MIRROR_SUPER_CLASS by error<PsiElement>()

        // @ForeignSetterName 不能用在不可变属性上
        val OBJC_SETTER_NAME_ON_IMMUTABLE_PROP by error<PsiElement>()
    }

    /**
     * CJMapping（Objective-C）语义
     *
     * 对齐 C++ sema_objc_cjmapping_* 系列。
     */
    val OBJC_CJMAPPING by object : DiagnosticGroup("ObjCCJMapping") {
        // cangjie mirror 声明继承接口不支持
        val OBJC_CJMAPPING_INHERITANCE_INTERFACE_NOT_SUPPORTED by error<PsiElement>()

        // cangjie mirror 声明泛型不支持
        val OBJC_CJMAPPING_GENERIC_NOT_SUPPORTED by error<PsiElement> {
            parameter<String>("genericDescription")
        }
    }

    /**
     * @ForeignName 语义
     *
     * 对齐 C++ sema_foreign_name_* 系列。
     */
    val FOREIGN_NAME by object : DiagnosticGroup("ForeignName") {
        // @ForeignName 不能出现在被 override 的声明上
        val FOREIGN_NAME_APPEARED_IN_CHILD by error<PsiElement> {
            parameter<Name>("annotationName")
        }

        // @ForeignName 注解冲突
        val FOREIGN_NAME_CONFLICTING_ANNOTATION by error<PsiElement> {
            parameter<Name>("declarationName")
            parameter<Name>("annotationName")
        }

        // @ForeignName 派生注解冲突
        val FOREIGN_NAME_CONFLICTING_DERIVED_ANNOTATION by error<PsiElement> {
            parameter<Name>("declarationName")
            parameter<Name>("annotationName")
            parameter<Name>("derivedName")
        }
    }

    /**
     * @IfAvailable 语义
     *
     * 对齐 C++ sema_ifavailable_* 系列。
     */
    val IF_AVAILABLE by object : DiagnosticGroup("IfAvailable") {
        // @IfAvailable 的第一个参数必须有名字
        val IFAVAILABLE_ARG_NO_NAME by error<PsiElement>()

        // @IfAvailable 的第一个参数必须是字面量表达式
        val IFAVAILABLE_ARG_NOT_LITERAL by error<PsiElement>()

        // 未知的参数名
        val IFAVAILABLE_UNKNOWN_ARG_NAME by error<PsiElement> {
            parameter<String>("paramName")
        }

        // APILevel 低于 19 时 @IfAvailable 不可用
        val IFAVAILABLE_LEVEL_LIMIT by error<PsiElement>()
    }

    /**
     * @APILevel 语义
     *
     * 对齐 C++ sema_apilevel_* 系列。
     */
    val API_LEVEL by object : DiagnosticGroup("APILevel") {
        // 不能标注多个 @!APILevel
        val APILEVEL_MULTI_ANNO by error<PsiElement>()

        // 缺少命名参数或无法读取数值（警告）
        val APILEVEL_MISSING_ARG by warning<PsiElement> {
            parameter<Name>("argName")
        }

        // 仅支持字面量值
        val ONLY_LITERAL_SUPPORT by error<PsiElement> {
            parameter<String>("kind")
        }

        // 不能引用比当前作用域更高 level 的声明
        val APILEVEL_REF_HIGHER by error<PsiElement> {
            parameter<Name>("name")
            parameter<Int>("refLevel")
            parameter<Int>("currentLevel")
        }

        // 不合适的 syscap（警告）
        val APILEVEL_SYSCAP_WARNING by warning<PsiElement> {
            parameter<Name>("syscap")
        }

        // 不合适的 syscap（错误）
        val APILEVEL_SYSCAP_ERROR by error<PsiElement> {
            parameter<Name>("syscap")
        }

        // 声明标记了不同的 syscap
        val APILEVEL_MULTI_DIFF_SYSCAP by error<PsiElement>()
    }

    /**
     * @Hide 语义
     *
     * 对齐 C++ sema_hide_* 系列。
     */
    val HIDE by object : DiagnosticGroup("Hide") {
        // 不能标注多个 @!Hide
        val HIDE_MULTI_ANNOTATION by error<PsiElement>()

        // 函数参数不能标注 @!Hide
        val HIDE_AT_FUNC_PARAM by error<PsiElement>()

        // 要隐藏的声明应标记 @!Hide
        val HIDE_MISSING_HIDE by error<PsiElement>()

        // @!Hide 注解必须在编译期可见
        val HIDE_COMPILE_TIME_INVISIBLE by error<PsiElement>()

        // @!Hide 的 isChecked 参数错误
        val HIDE_DIFF_PARAM by error<PsiElement> {
            parameter<String>("paramValue")
        }

        // @!Hide 注解必须放在所有宏和注解之后（警告）
        val HIDE_MUST_AT_END by warning<PsiElement> {
            parameter<String>("annotationName")
        }
    }

    /**
     * 未使用导入（警告）
     *
     * 对齐 C++ sema_unused_import。
     */
    val UNUSED by object : DiagnosticGroup("Unused") {
        // 未使用的导入
        val UNUSED_IMPORT by warning<CjImportItem> {
            parameter<FqName>("importPath")
        }

        // 未使用的表达式
        val UNUSED_EXPRESSION by warning<CjExpression>()

        // 未使用的局部变量
        val UNUSED_VARIABLE by warning<PsiElement>()

        // 未使用的函数
        val UNUSED_FUNCTION by warning<PsiElement>()

        // type alias 声明了但展开类型未使用的类型参数
        val TYPEALIAS_UNUSED_TYPE_PARAMETERS by warning<PsiElement> {
            parameter<String>("typeParameters")
        }

        // type alias 展开链中出现环
        val TYPEALIAS_CYCLE by error<PsiElement> {
            parameter<String>("typeAlias")
        }
    }

    /**
     * 单元测试 / Mock 语义
     *
     * 对齐 C++ sema_mock_* 系列。
     */
    val MOCK by object : DiagnosticGroup("Mock") {
        // Mock 功能已禁用
        val MOCK_DISABLED by error<PsiElement> {
            parameter<String>("option")
        }

        // Mock 功能只能在测试模式下使用
        val MOCK_NOT_IN_TEST_MODE by error<PsiElement> {
            parameter<String>("option")
        }

        // 只支持对 class 或 interface 进行 mock
        val MOCK_UNSUPPORTED_TYPE by error<PsiElement>()

        // 要 mock 的 static/top-level 声明不能是 private/local/constant/constructor
        val MOCK_WRONG_STATIC_DECL by error<PsiElement>()

        // 目标不支持 mock（包未以 mock-compatible 方式编译）
        val MOCK_DOESNT_SUPPORT_MOCKING by error<PsiElement> {
            parameter<Name>("name")
            parameter<FqName>("packageName")
            parameter<String>("option")
        }

        // 不支持 mock @Frozen 标记的声明
        val MOCK_FROZEN_UNSUPPORTED by error<PsiElement>()

        // createMock/createSpy 的泛型包装函数需要 @Frozen 注解
        val MOCK_FROZEN_REQUIRED by error<PsiElement> {
            parameter<Name>("functionName")
        }
    }

    /**
     * Macro construction step 相关诊断（baseline 第 9-10 节）。
     *
     * 这些 factory 都对应 `MacroExpansionRegistry` 的
     * `MacroConstructionDiagnostic.Kind` 枚举值；
     * ordinary checker 只看 final CFIR，渲染时通过
     * `originSurfaceId` 反查 registry 找到原 macro 位点。
     */
    val MACRO by object : DiagnosticGroup("Macro") {
        // baseline 第 9 节 "MACRO_NOT_EXPANDED": IDE degraded 模式产物
        // —— typed error placeholder 替代了 macro call，原 macro 调用未展开。
        val MACRO_NOT_EXPANDED by error<PsiElement> {
            parameter<String>("macroName")
        }

        // baseline 第 9 节 "MACRO_EXPANSION_FAILED": construction step 中
        // executor 调用或 fragment parse 失败、且未降级。
        val MACRO_EXPANSION_FAILED by error<PsiElement> {
            parameter<String>("macroName")
            parameter<String>("reason")
        }

        val MACRO_DIAG_REPORT_ERROR by error<PsiElement> {
            parameter<String>("message")
            parameter<String>("hint")
        }

        val MACRO_DIAG_REPORT_WARNING by warning<PsiElement> {
            parameter<String>("message")
            parameter<String>("hint")
        }

        val MACRO_UNDEFINED_PACKAGE by error<PsiElement> {
            parameter<String>("packageName")
            parameter<String>("reason")
        }

        val MACRO_UNDECLARED_IDENTIFIER by error<PsiElement> {
            parameter<Name>("name")
            parameter<String>("reason")
        }

        val MACRO_EXPECT_MACRO_DEFINITION by error<PsiElement> {
            parameter<String>("target")
            parameter<String>("reason")
        }

        val MACRO_DEPENDENCY_COMPILE_FAILED by error<PsiElement> {
            parameter<String>("packageName")
            parameter<String>("reason")
            parameter<String>("diagnosticsRef")
        }

        val MACRO_AMBIGUOUS_MATCH by error<PsiElement> {
            parameter<String>("macroName")
            parameter<Collection<FqName>>("targets")
        }

        val MACRO_CANNOT_FIND_DEPENDENCY_BCHIR by error<PsiElement> {
            parameter<String>("packageName")
            parameter<String>("path")
        }

        val MACRO_EXPECT_PLAIN_MACRO by error<PsiElement> {
            parameter<String>("macroName")
            parameter<String>("reason")
        }

        val MACRO_EXPECT_ATTRIBUTED_MACRO by error<PsiElement> {
            parameter<String>("macroName")
            parameter<String>("reason")
        }

        val MACRO_EXPAND_ATEXCL by error<PsiElement> {
            parameter<String>("macroName")
            parameter<String>("reason")
        }

        val MACRO_INVALID_ATTR_TOKENS by error<PsiElement> {
            parameter<String>("macroName")
            parameter<String>("reason")
        }

        val MACRO_INVALID_INPUT_TOKENS by error<PsiElement> {
            parameter<String>("macroName")
            parameter<String>("reason")
        }

        val MACRO_INVALID_ESCAPE by error<PsiElement> {
            parameter<String>("macroName")
            parameter<String>("reason")
        }

        // baseline 第 4 节 "同包 macro def/call": 源包内同时存在 macro 定义和调用。
        val MACRO_SAME_PACKAGE_DEF_CALL by error<PsiElement> {
            parameter<String>("macroName")
            parameter<FqName>("packageName")
        }

        // baseline Batch 5 "alias conflict": 同一 alias 短名绑到多个 fqn。
        val MACRO_ALIAS_CONFLICT by error<PsiElement> {
            parameter<Name>("alias")
            parameter<Collection<FqName>>("targets")
        }

        // baseline 第 4 节 "no executor / unresolved / cannot-open-lib /
        // REEVALFAILED" 诊断 typed factory。
        val MACRO_EXECUTOR_UNAVAILABLE by error<PsiElement> {
            parameter<String>("hint")
        }

        val MACRO_CANNOT_OPEN_LIB by error<PsiElement> {
            parameter<String>("libPath")
            parameter<String>("reason")
        }

        val MACRO_CANNOT_FIND_METHOD by error<PsiElement> {
            parameter<String>("macroName")
            parameter<String>("reason")
        }

        val MACRO_EVALUATE_FAILED by error<PsiElement> {
            parameter<String>("macroName")
            parameter<String>("reason")
        }

        val MACRO_EXPAND_FAILED by error<PsiElement> {
            parameter<String>("macroName")
            parameter<String>("reason")
        }

        val MACRO_EXPAND_CODE_SHOULD_NOT_HAVE_MACROCALL by error<PsiElement> {
            parameter<String>("macroName")
            parameter<String>("reason")
        }

        val MACRO_CALL_SAVE_FILE_FAILED by error<PsiElement> {
            parameter<String>("macroName")
            parameter<String>("reason")
        }

        val MACRO_EXECUTOR_PROTOCOL_ERROR by error<PsiElement> {
            parameter<String>("reason")
        }

        val MACRO_EXECUTOR_SERVER_DISCONNECTED by error<PsiElement> {
            parameter<String>("reason")
        }

        val MACRO_EXECUTOR_TIMEOUT by error<PsiElement> {
            parameter<String>("reason")
        }

        val MACRO_EXECUTOR_SERVER_CRASH by error<PsiElement> {
            parameter<String>("reason")
        }

        val MACRO_REEVALUATION_FAILED by error<PsiElement> {
            parameter<String>("macroName")
            parameter<String>("reason")
        }

        val MACRO_UNRESOLVED by error<PsiElement> {
            parameter<Name>("macroName")
        }

        // baseline Batch 7 cycle detection: 同 fingerprint 在 forest evaluator
        // 多次出现，超出 iteration limit。
        val MACRO_CYCLE by error<PsiElement> {
            parameter<String>("macroName")
            parameter<Collection<String>>("cycleChain")
        }
    }
}

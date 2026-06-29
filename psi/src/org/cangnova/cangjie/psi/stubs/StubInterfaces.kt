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

package org.cangnova.cangjie.psi.stubs

import com.intellij.psi.PsiNamedElement
import com.intellij.psi.stubs.NamedStub
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.*

/**
 * 表示 `ConstantValueKind`，承载PSI Stub中的语法节点、索引桩或辅助模型。
 */
enum class ConstantValueKind {

    BOOLEAN_CONSTANT,
    FLOAT_CONSTANT,
    RUNE_CONSTANT,

    CHARACTER_BYTE_CONSTANT,
    INTEGER_CONSTANT,
    UNIT_CONSTANT,
}

/**
 * 定义 `CangJiePropertyAccessorStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJiePropertyAccessorStub : StubElement<CjPropertyAccessor> {
    /**
     * 提供 `isGetter` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun isGetter(): Boolean
    /**
     * 提供 `hasBody` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun hasBody(): Boolean
    /**
     * 提供 `hasBlockBody` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun hasBlockBody(): Boolean
}

/**
 * 定义 `CangJieCollectionLiteralExpressionStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieCollectionLiteralExpressionStub : StubElement<CjCollectionLiteralExpression>

/**
 * 定义 `CangJieConstantExpressionStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieConstantExpressionStub : StubElement<CjConstantExpression> {
    /**
     * 提供 `kind` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun kind(): ConstantValueKind
    /**
     * 提供 `value` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun value(): String
}

/**
 * 定义 `CangJieStubElement` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieStubElement<T : CjElement> : StubElement<T> {
    /** Returns a copy of this stub with the parent set to [newParent] */
    fun copyInto(newParent: StubElement<*>?): CangJieStubElement<T>
}


/**
 * 定义 `CangJieFileStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieFileStub : PsiFileStub<CjFile>, CangJieStubElement<CjFile> {
    /**
     * 提供 `getPackageFqName` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun getPackageFqName(): FqName

    /**
     * 保存 `kind`，供PSI Stub流程读取节点结构或语义信息。
     */
    val kind: CangJieFileStubKind
}

/**
 * CangJiePlaceHolderStub接口定义了一个通用的占位符 Stub 元素
 * 它继承自StubElement，用于表示CangJie解析树中的占位符节点
 * 这个接口是泛型的，允许它用于任何CjElement的子类
 *
 * @param T 表示泛型参数，限定了T必须是CjElement的子类
 */
interface CangJiePlaceHolderStub<T : CjElement> : StubElement<T>

/**
 * 定义 `CangJieAnnotationStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieAnnotationStub : StubElement<CjAnnotation> {
    /**
     * 提供 `getShortName` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun getShortName(): String?
    /**
     * 提供 `hasValueArguments` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun hasValueArguments(): Boolean
}

/**
 * 定义 `CangJieMacroExpressionStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieMacroExpressionStub : StubElement<CjMacroExpression> {
    /**
     * 提供 `getShortName` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun getShortName(): String?
    /**
     * 提供 `hasValueArguments` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun hasValueArguments(): Boolean
}

/**
 * 定义 `CangJieModifierListStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieModifierListStub : StubElement<CjDeclarationModifierList> {
    /**
     * 提供 `hasModifier` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun hasModifier(modifierToken: CjKeywordToken): Boolean
}

/**
 * 定义 `CangJieContextReceiverStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieContextReceiverStub : StubElement<CjContextReceiver> {
    /**
     * 提供 `getLabel` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun getLabel(): String?
}

/**
 * 定义 `CangJieValueArgumentStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieValueArgumentStub<T : CjValueArgument> : CangJiePlaceHolderStub<T> {
    /**
     * 提供 `isSpread` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun isSpread(): Boolean
}

/**
 * 定义 `CangJieBasicTypeStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieBasicTypeStub : StubElement<CjBasicType> {
    /**
     * 保存 `basicType`，供PSI Stub流程读取节点结构或语义信息。
     */
    val basicType: String
}

/**
 * 定义 `CangJieUserTypeStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieUserTypeStub : StubElement<CjUserType>
/**
 * 定义 `CangJieTupleTypeStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieTupleTypeStub : StubElement<CjTupleType>

/**
 * 定义 `CangJieClassifierStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieClassifierStub {
    /**
     * 提供 `getClassId` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun getClassId(): ClassId?
}

/**
 * 定义 `CangJieTypeAliasStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieTypeAliasStub : CangJieClassifierStub, CangJieStubWithFqName<CjTypeAlias> {

}

/**
 * 变量声明模式类型
 *
 * 变量声明支持的模式匹配类型（不包含类型模式）
 */
enum class PatternKind {
    /** 通配符模式: `_` */
    WILDCARD,

    /** 绑定模式: `identifier` */
    BINDING,

    /** 元组模式: `(a, b)` */
    TUPLE,

    /** 枚举模式: `Some(x)` */
    ENUM;

    companion object {
        fun fromOrdinal(ordinal: Int): PatternKind = entries[ordinal]
    }
}

/**
 * 变量声明的 Stub 接口
 *
 * 变量声明是模式匹配的声明方式，用于顶层变量和局部变量。
 * 与 CangJieFieldStub 不同，Variable 支持模式匹配解构。
 *
 * 变量本身没有 fqName，因为一个变量声明可能包含多个绑定（如元组解构）。
 * fqName 存储在子模式（CangJieBindingPatternStub）中。
 */
interface CangJieVariableStub : StubElement<CjPatternVariable> {
    /** 获取模式类型 */
    fun getPatternKind(): PatternKind

    /** 是否为 var 声明（可变） */
    fun isVar(): Boolean

    /** 是否为 const 声明（编译期常量） */
    fun isConst(): Boolean

    /** 是否为顶层变量 */
    fun isTopLevel(): Boolean

    /** 是否有初始化器 */
    fun hasInitializer(): Boolean

    /** 是否有类型声明 */
    fun hasReturnTypeRef(): Boolean
}

// ==================== 模式 Stub 接口 ====================

/**
 * 模式 Stub 基础接口
 */
interface CangJiePatternStub<T : CjCasePatternElement> : StubElement<T>

/**
 * 绑定模式 Stub
 *
 * 存储绑定变量的名称和 fqName。
 * 如 `let a = 1` 中的 `a`，fqName 为 `package.a`。
 *
 * 对于 `let (a, b) = tuple`，会有两个绑定模式 Stub，
 * 分别存储 `a` 和 `b` 的 fqName。
 */
interface CangJieBindingPatternStub : CangJiePatternStub<CjBindingPattern>, NamedStub<CjBindingPattern> {
    /** 获取完全限定名（仅顶层变量的绑定有效） */
    val fqName: FqName?

}

/**
 * 元组模式 Stub
 *
 * 子 stub 包含元组中的各个模式
 * 如 `let (a, b) = tuple` 中的 `(a, b)`
 */
interface CangJieTuplePatternStub : CangJiePatternStub<CjTuplePattern>

/**
 * 类型模式 Stub
 *
 * ```cangjie
 * match(a){
 *   case b:Int => {}
 * }
 * ```
 */
interface CangJieTypePatternStub : CangJiePatternStub<CjTypePattern>, NamedStub<CjTypePattern>

/**
 * 枚举模式 Stub
 *
 * 存储枚举类型引用，子 stub 包含参数模式
 * 如 `let Some(x) = optional` 中的 `Some(x)`
 */
interface CangJieEnumPatternStub : CangJiePatternStub<CjEnumPattern>

/**
 * 裸名字歧义模式 Stub。
 *
 * 仅保存源码中的名字文本，语义阶段再决定它究竟是 binding 还是 enum constructor。
 */
interface CangJieVarOrEnumPatternStub : CangJiePatternStub<CjVarOrEnumPattern>, NamedStub<CjVarOrEnumPattern>

/**
 * 通配符模式 Stub
 *
 * 如 `let _ = ignored` 中的 `_`
 */
interface CangJieWildcardPatternStub : CangJiePatternStub<CjWildcardPattern>

/**
 * 常量模式 Stub
 *
 * 如 `case 1 => ...` 中的常量
 */
interface CangJieConstantPatternStub : CangJiePatternStub<CjConstantPattern>

/**
 * Match 条件表达式模式 Stub
 */
interface CangJieMatchConditionStub : CangJiePatternStub<CjMatchConditionWithExpression>

/**
 * 定义 `CangJiePropertyStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJiePropertyStub : CangJieCallableStubBase<CjProperty> {
    /**
     * 实现 `isTopLevel` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isTopLevel(): Boolean = false
}

/**
 * 类成员字段的 Stub 接口
 *
 * 与 CangJieVariableStub 不同，Field 是类/结构体/接口的成员变量声明，
 * 不支持模式匹配，只有简单的标识符名称。
 *
 * 示例:
 * ```cangjie
 * class Person {
 *     let name: String      // Field
 *     var age: Int64        // Field
 *     const MAX_AGE = 150   // Field
 * }
 * ```
 */
interface CangJieFieldStub : CangJieCallableStubBase<CjFieldVariable> {
    /**
     * 提供 `isVar` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun isVar(): Boolean
    /**
     * 提供 `isConst` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun isConst(): Boolean
    /**
     * 提供 `hasInitializer` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun hasInitializer(): Boolean
    /**
     * 提供 `hasReturnTypeRef` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun hasReturnTypeRef(): Boolean

    /**
     * 实现 `isTopLevel` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isTopLevel(): Boolean = false
}

/**
 * 定义 `CangJieCallableStubBase` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieCallableStubBase<TDeclaration : CjCallableDeclaration> : CangJieStubWithFqName<TDeclaration> {
    /**
     * 提供 `isTopLevel` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun isTopLevel(): Boolean
}

/**
 * 定义 `CangJieStubWithFqName` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieStubWithFqName<T : PsiNamedElement> : NamedStub<T> {
    /**
     * 提供 `getFqName` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun getFqName(): FqName?
}

/**
 * 定义 `CangJieTypeParameterStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieTypeParameterStub : CangJieStubWithFqName<CjTypeParameter> {
//    fun isInVariance(): Boolean
}

/**
 * 定义 `CangJieNameBasicReferenceExpressionStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieNameBasicReferenceExpressionStub : StubElement<CjNameBasicReferenceExpression> {
    /**
     * 提供 `getReferencedName` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun getReferencedName(): String
}

/**
 * 定义 `CangJieNameReferenceExpressionStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieNameReferenceExpressionStub : StubElement<CjNameReferenceExpression> {
    /**
     * 提供 `getReferencedName` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun getReferencedName(): String
}

/**
 * 定义 `CangJieParameterStubBase` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieParameterStubBase<T : PsiNamedElement> : CangJieStubWithFqName<T>
/**
 * 定义 `CangJieCatchParameterStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieCatchParameterStub : CangJieParameterStubBase<CjCatchParameter>

/**
 * 定义 `CangJieParameterStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieParameterStub : CangJieParameterStubBase<CjParameter> {
    /**
     * 提供 `isMutable` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun isMutable(): Boolean
    /**
     * 提供 `hasLetOrVar` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun hasLetOrVar(): Boolean
    /**
     * 提供 `hasDefaultValue` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun hasDefaultValue(): Boolean
    /**
     * 提供 `isNamed` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun isNamed(): Boolean
}

/**
 * 定义 `CangJieClassStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieClassStub : CangJieTypeStatementStub<CjClass> {

}

/**
 * 定义 `CangJieStructStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieStructStub : CangJieTypeStatementStub<CjStruct>

/**
 * 定义 `CangJieInterfaceStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieInterfaceStub : CangJieTypeStatementStub<CjInterface>

/**
 * 定义 `CangJieEnumStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieEnumStub : CangJieTypeStatementStub<CjEnum> {
    /**
     * 是否非穷尽枚举
     */
    fun isNonExhaustive(): Boolean


}

/**
 * 枚举构造器的 Stub 接口（全量重构版本）
 *
 * 根据仓颉语言规范，枚举条目是构造器（constructors），而非类型声明。
 * 因此此接口直接继承 CangJieStubWithFqName，不再继承 CangJieTypeStatementStub。
 *
 * 示例:
 * ```cangjie
 * enum RGBColor {
 *     | Red | Green | Blue              // 无参数构造器
 *     | Red(UInt8) | Green(UInt8) | Blue(UInt8)  // 有参数构造器
 * }
 * ```
 */
/**
 * 枚举构造器 Stub 接口
 *
 * 枚举构造器不是独立声明，只存储名称和参数类型数量（用于重载区分）。
 * 参数类型详情通过 TYPE_LIST 子树获取。
 */
interface CangJieEnumConstructorStub : NamedStub<CjEnumConstructor> {
    /**
     * 获取参数类型数量（用于区分重载的枚举构造器）
     */
    fun getTypeCount(): Int

    /**
     * 获取所属枚举的完全限定名（用于索引）
     */
    fun getEnumFqName(): FqName?
}



/**
 * 定义 `CangJieExtendStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieExtendStub : CangJieTypeStatementStub<CjExtend> {
    /**
     * 保存 `extendId`，供PSI Stub流程读取节点结构或语义信息。
     */
    val extendId: String;

    //被扩展类型名称
    /**
     * 保存 `receiverTypeName`，供PSI Stub流程读取节点结构或语义信息。
     */
    val receiverTypeName: String?;
}


/**
 * 定义 `CangJieTypeStatementStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieTypeStatementStub<T : CjTypeStatement> : CangJieClassifierStub, CangJieStubWithFqName<T> {
    /**
     * 提供 `getSuperNames` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun getSuperNames(): List<String>
}

/**
 * 定义 `CangJieConstructorStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieConstructorStub<T : CjConstructor<T>> :
    CangJieCallableStubBase<T> {
    /**
     * 提供 `hasBody` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun hasBody(): Boolean
    /**
     * 保存 `isPrimary`，供PSI Stub流程读取节点结构或语义信息。
     */
    val isPrimary: Boolean get() = false
}

/**
 * 定义 `CangJieFinalizerStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieFinalizerStub : CangJieCallableStubBase<CjFinalizer> {
    /**
     * 提供 `hasBody` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun hasBody(): Boolean
}

/**
 * 定义 `CangJieImportAliasStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieImportAliasStub : StubElement<CjImportAlias> {
    /**
     * 提供 `getName` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun getName(): String?
}


/**
 * 定义 `CangJieFunctionStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieFunctionStub<F : CjFunction> : CangJieCallableStubBase<F> {
    /**
     * 提供 `hasBlockBody` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun hasBlockBody(): Boolean
    /**
     * 提供 `hasBody` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun hasBody(): Boolean
    /**
     * 提供 `hasTypeParameterListBeforeFunctionName` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun hasTypeParameterListBeforeFunctionName(): Boolean
}

/**
 * 定义 `CangJieNamedFunctionStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieNamedFunctionStub : CangJieFunctionStub<CjNamedFunction>

/**
 * 定义 `CangJieMainFunctionStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieMainFunctionStub : CangJieFunctionStub<CjMainFunction> {
    /**
     * 实现 `hasBlockBody` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasBlockBody(): Boolean {
        return true
    }

    /**
     * 实现 `hasBody` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasBody(): Boolean {
        return true
    }

    /**
     * 实现 `hasTypeParameterListBeforeFunctionName` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasTypeParameterListBeforeFunctionName(): Boolean {
        return false
    }
}

/**
 * 定义 `CangJieMacroStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieMacroStub : CangJieFunctionStub<CjMacroDeclaration>


/**
 * 定义 `CangJieForeignDirectiveStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieForeignDirectiveStub : StubElement<CjForeignDirective>


/**
 * 定义 `CangJiePackageDirectiveStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJiePackageDirectiveStub : StubElement<CjPackageDirective> {
    /**
     * 当前 package directive 是否使用 macro package 形式。
     */
    val isMacroPackage: Boolean
}


/**
 * 定义 `CangJieImportDirectiveStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieImportDirectiveStub : StubElement<CjImportDirective> {
    /**
     * 获取包的完全限定名
     */
    fun getPackageFqName(): FqName?

    /**
     * 获取所有导入项的信息
     * 返回 List<ImportItemInfo>，每个元素包含：
     * - importedFqName: 导入的完全限定名
     * - isAllUnder: 是否是通配符导入
     * - aliasName: 别名(如果有)
     */
    fun getImportItems(): List<ImportItemInfo>

    /**
     * 导入项信息数据类
     */
    data class ImportItemInfo(
        /**
         * 保存 `importedFqName`，供PSI Stub流程读取节点结构或语义信息。
         */
        val importedFqName: FqName?,
        /**
         * 保存 `isAllUnder`，供PSI Stub流程读取节点结构或语义信息。
         */
        val isAllUnder: Boolean,
        /**
         * 保存 `aliasName`，供PSI Stub流程读取节点结构或语义信息。
         */
        val aliasName: String?
    )
}


/**
 * 定义 `CangJieTypeProjectionStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieTypeProjectionStub : StubElement<CjTypeProjection> {
    /**
     * 提供 `getProjectionKind` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun getProjectionKind(): CjProjectionKind
}

/**
 * 定义 `CangJiePlaceHolderWithTextStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJiePlaceHolderWithTextStub<T : CjElement> : CangJiePlaceHolderStub<T> {
    /**
     * 提供 `text` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    fun text(): String
}

/**
 * 定义 `CangJieFunctionTypeStub` 接口，约束PSI Stub节点或服务需要暴露的结构能力。
 */
interface CangJieFunctionTypeStub : StubElement<CjFunctionType>

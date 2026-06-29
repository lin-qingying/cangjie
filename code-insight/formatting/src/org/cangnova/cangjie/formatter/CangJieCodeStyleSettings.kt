/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.formatter

import com.intellij.configurationStore.Property
import com.intellij.ide.plugins.PluginManagerCore.isUnitTestMode
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CustomCodeStyleSettings
import org.jdom.Element

/**
 * 仓颉 formatter 的自定义代码风格设置。
 *
 * @property isTempForDeserialize 是否为读取 XML 时使用的临时设置对象。
 */
class CangJieCodeStyleSettings(
    container: CodeStyleSettings,
    /** 是否为读取 XML 时使用的临时设置对象。 */
    val isTempForDeserialize: Boolean = false
) : CustomCodeStyleSettings("CangJieCodeStyleSettings", container) {


    companion object {
        public val DEFAULT_NAME_COUNT_TO_USE_STAR_IMPORT = 5
        public val DEFAULT_NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS = 3

        private fun readExternalToTemp(parentElement: Element): CangJieCodeStyleSettings {
            val tempSettings = CangJieCodeStyleSettings(CodeStyleSettings.getDefaults(), true)
            tempSettings.readExternal(parentElement)
            return tempSettings
        }
    }

    /**
     * 当前 custom settings 对应的仓颉预定义代码风格标识。
     */
    @ReflectionUtil.SkipInEquals
    @JvmField
    var CODE_STYLE_DEFAULTS: String? = null

    /**
     * 允许使用 star import 的包列表。
     */
    @ReflectionUtil.SkipInEquals
    @Property(externalName = "packages_to_use_import_on_demand")
    @JvmField
    var PACKAGES_TO_USE_STAR_IMPORTS = CangJiePackageEntryTable()

    /**
     * import 排序和分组布局。
     */
    @ReflectionUtil.SkipInEquals
    @Property(externalName = "imports_layout")
    @JvmField
    var PACKAGES_IMPORT_LAYOUT = CangJiePackageEntryTable()

    /** range 运算符周围是否保留空格。 */
    @JvmField
    public var SPACE_AROUND_RANGE = true
    /** 一元运算符周围是否保留空格。 */
    @JvmField
    public var SPACE_AROUND_UNARY_OPERATOR = true

    /** 类型冒号前是否保留空格。 */
    @JvmField
    public var SPACE_BEFORE_TYPE_COLON = false

    /** 类型冒号后是否保留空格。 */
    @JvmField
    public var SPACE_AFTER_TYPE_COLON = true

    /** extend 冒号前是否保留空格。 */
    @JvmField
    public var SPACE_BEFORE_EXTEND_COLON = true

    /** extend 冒号后是否保留空格。 */
    @JvmField
    public var SPACE_AFTER_EXTEND_COLON = true

    /** 简单单行方法体内是否插入空格。 */
    @JvmField
    public var INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD = true

    /** match case 分支是否按列对齐。 */
    @JvmField
    public var ALIGN_IN_COLUMNS_CASE_BRANCH = false

    /** 多行 match entry 后是否强制换行。 */
    @JvmField
    public var LINE_BREAK_AFTER_MULTILINE_MATCH_ENTRY = true

    /** 函数类型箭头周围是否保留空格。 */
    @JvmField
    public var SPACE_AROUND_FUNCTION_TYPE_ARROW = true

    /** match 箭头周围是否保留空格。 */
    @JvmField
    public var SPACE_AROUND_MATCH_ARROW = true

    /** lambda 箭头前是否保留空格。 */
    @JvmField
    public var SPACE_BEFORE_LAMBDA_ARROW = true

    /** match 条件括号前是否保留空格。 */
    @JvmField
    public var SPACE_BEFORE_MATCH_PARENTHESES = true

    /** 左花括号是否放到下一行。 */
    @JvmField
    public var LBRACE_ON_NEXT_LINE = false

    /** 触发普通 star import 的名称数量阈值。 */
    @JvmField
    public var NAME_COUNT_TO_USE_STAR_IMPORT =
        if (isUnitTestMode) Integer.MAX_VALUE else DEFAULT_NAME_COUNT_TO_USE_STAR_IMPORT

    /** 触发成员 star import 的名称数量阈值。 */
    @JvmField
    public var NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS =
        if (isUnitTestMode) Integer.MAX_VALUE else DEFAULT_NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS

    /** import 时是否允许嵌套类参与导入。 */
    @JvmField
    public var IMPORT_NESTED_CLASSES = false

    /** 参数列表换行时是否使用 continuation indent。 */
    @JvmField
    public var CONTINUATION_INDENT_IN_PARAMETER_LISTS = true

    /** 实参列表换行时是否使用 continuation indent。 */
    @JvmField
    public var CONTINUATION_INDENT_IN_ARGUMENT_LISTS = true

    /** 表达式体函数换行时是否使用 continuation indent。 */
    @JvmField
    public var CONTINUATION_INDENT_FOR_EXPRESSION_BODIES = true

    /** 链式调用换行时是否使用 continuation indent。 */
    @JvmField
    var CONTINUATION_INDENT_FOR_CHAINED_CALLS = true

    /** 父类型列表换行时是否使用 continuation indent。 */
    @JvmField
    public var CONTINUATION_INDENT_IN_SUPERTYPE_LISTS = true

    /** if 条件换行时是否使用 continuation indent。 */
    @JvmField
    public var CONTINUATION_INDENT_IN_IF_CONDITIONS = true

    /** elvis 表达式换行时是否使用 continuation indent。 */
    @JvmField
    public var CONTINUATION_INDENT_IN_ELVIS = true

    /** 块状 match 分支周围的空行数量。 */
    @JvmField
    public val BLANK_LINES_AROUND_BLOCK_MATCH_BRANCHES = 0

    /** 表达式体函数的换行策略。 */
    @JvmField
    public var WRAP_EXPRESSION_BODY_FUNCTIONS = 0

    /** elvis 表达式的换行策略。 */
    @JvmField
    public val WRAP_ELVIS_EXPRESSIONS = 1

    /** if 右括号是否放到新行。 */
    @JvmField
    public var IF_RPAREN_ON_NEW_LINE = false

    /** 声明位置是否允许尾逗号。 */
    @JvmField
    public var ALLOW_TRAILING_COMMA = false

    /** 调用位置是否允许尾逗号。 */
    @JvmField
    public val ALLOW_TRAILING_COMMA_ON_CALL_SITE = false

    /** 注释或注解独占一行时，声明前保留的空行数量。 */
    @JvmField
    public val BLANK_LINES_BEFORE_DECLARATION_WITH_COMMENT_OR_ANNOTATION_ON_SEPARATE_LINE = 1

    /**
     * 克隆设置并深拷贝 import 相关表。
     */
    override fun clone(): Any {
        val clone = super.clone() as CangJieCodeStyleSettings

        clone.PACKAGES_TO_USE_STAR_IMPORTS = CangJiePackageEntryTable()
        clone.PACKAGES_TO_USE_STAR_IMPORTS.copyFrom(this.PACKAGES_TO_USE_STAR_IMPORTS)

        clone.PACKAGES_IMPORT_LAYOUT = CangJiePackageEntryTable()
        clone.PACKAGES_IMPORT_LAYOUT.copyFrom(this.PACKAGES_IMPORT_LAYOUT)

        return clone
    }

    /**
     * 比较 import 表和公开非 final 字段。
     */
    override fun equals(obj: Any?): Boolean {
        if (obj !is CangJieCodeStyleSettings) return false

        if (!Comparing.equal(PACKAGES_TO_USE_STAR_IMPORTS, obj.PACKAGES_TO_USE_STAR_IMPORTS)) return false
        if (!Comparing.equal(PACKAGES_IMPORT_LAYOUT, obj.PACKAGES_IMPORT_LAYOUT)) return false
        return ReflectionUtil.comparePublicNonFinalFieldsWithSkip(this, obj)
    }

    /**
     * 写出 XML 前根据预定义风格计算父设置差异。
     */
    @Throws(WriteExternalException::class)
    override fun writeExternal(parentElement: Element, parentSettings: CustomCodeStyleSettings) {
        var parentSettings = parentSettings
        if (CODE_STYLE_DEFAULTS != null) {
            val defaultCangJieCodeStyle = parentSettings.clone() as CangJieCodeStyleSettings

            applyCangJieCodeStyle(CODE_STYLE_DEFAULTS, defaultCangJieCodeStyle, false)

            parentSettings = defaultCangJieCodeStyle
        }

        super.writeExternal(parentElement, parentSettings)
    }

    /**
     * 从 XML 读取 custom settings，并优先应用预定义风格默认值。
     */
    @Throws(InvalidDataException::class)
    override fun readExternal(parentElement: Element) {
        if (isTempForDeserialize) {
            super.readExternal(parentElement)
            return
        }

        val tempSettings = readExternalToTemp(parentElement)
        val customDefaults = tempSettings.CODE_STYLE_DEFAULTS

        applyCangJieCodeStyle(customDefaults, this, true)

        super.readExternal(parentElement)
    }

    /**
     * 基于全部 formatter 选项生成 hash code。
     */
    override fun hashCode(): Int {
        var result = isTempForDeserialize.hashCode()
        result = 31 * result + (CODE_STYLE_DEFAULTS?.hashCode() ?: 0)
        result = 31 * result + PACKAGES_TO_USE_STAR_IMPORTS.hashCode()
        result = 31 * result + PACKAGES_IMPORT_LAYOUT.hashCode()
        result = 31 * result + SPACE_AROUND_RANGE.hashCode()
        result = 31 * result + SPACE_BEFORE_TYPE_COLON.hashCode()
        result = 31 * result + SPACE_AFTER_TYPE_COLON.hashCode()
        result = 31 * result + SPACE_BEFORE_EXTEND_COLON.hashCode()
        result = 31 * result + SPACE_AFTER_EXTEND_COLON.hashCode()
        result = 31 * result + INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD.hashCode()
        result = 31 * result + ALIGN_IN_COLUMNS_CASE_BRANCH.hashCode()
        result = 31 * result + LINE_BREAK_AFTER_MULTILINE_MATCH_ENTRY.hashCode()
        result = 31 * result + SPACE_AROUND_FUNCTION_TYPE_ARROW.hashCode()
        result = 31 * result + SPACE_AROUND_MATCH_ARROW.hashCode()
        result = 31 * result + SPACE_BEFORE_LAMBDA_ARROW.hashCode()
        result = 31 * result + SPACE_BEFORE_MATCH_PARENTHESES.hashCode()
        result = 31 * result + LBRACE_ON_NEXT_LINE.hashCode()
        result = 31 * result + NAME_COUNT_TO_USE_STAR_IMPORT
        result = 31 * result + NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS
        result = 31 * result + IMPORT_NESTED_CLASSES.hashCode()
        result = 31 * result + CONTINUATION_INDENT_IN_PARAMETER_LISTS.hashCode()
        result = 31 * result + CONTINUATION_INDENT_IN_ARGUMENT_LISTS.hashCode()
        result = 31 * result + CONTINUATION_INDENT_FOR_EXPRESSION_BODIES.hashCode()
        result = 31 * result + CONTINUATION_INDENT_FOR_CHAINED_CALLS.hashCode()
        result = 31 * result + CONTINUATION_INDENT_IN_SUPERTYPE_LISTS.hashCode()
        result = 31 * result + CONTINUATION_INDENT_IN_IF_CONDITIONS.hashCode()
        result = 31 * result + CONTINUATION_INDENT_IN_ELVIS.hashCode()
        result = 31 * result + BLANK_LINES_AROUND_BLOCK_MATCH_BRANCHES
        result = 31 * result + WRAP_EXPRESSION_BODY_FUNCTIONS
        result = 31 * result + WRAP_ELVIS_EXPRESSIONS
        result = 31 * result + IF_RPAREN_ON_NEW_LINE.hashCode()
        result = 31 * result + ALLOW_TRAILING_COMMA.hashCode()
        result = 31 * result + ALLOW_TRAILING_COMMA_ON_CALL_SITE.hashCode()
        result = 31 * result + BLANK_LINES_BEFORE_DECLARATION_WITH_COMMENT_OR_ANNOTATION_ON_SEPARATE_LINE
        return result
    }
}

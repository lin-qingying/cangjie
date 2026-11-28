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

/**
 * import 布局中的一个包匹配项。
 */
class CangJiePackageEntry(
    packageName: String,
    /** 是否匹配该包的子包。 */
    val withSubpackages: Boolean
) {
    /** 去掉通配后缀后的包名。 */
    val packageName = packageName.removeSuffix(".*")

    companion object {
        @JvmField
        val ALL_OTHER_IMPORTS_ENTRY = CangJiePackageEntry("<all other imports>", withSubpackages = true)

        @JvmField
        val ALL_OTHER_ALIAS_IMPORTS_ENTRY = CangJiePackageEntry("<all other alias imports>", withSubpackages = true)
    }

    /**
     * 判断给定包名是否匹配当前条目。
     */
    fun matchesPackageName(otherPackageName: String): Boolean {
        if (otherPackageName.startsWith(packageName)) {
            if (otherPackageName.length == packageName.length) return true
            if (withSubpackages) {
                if (otherPackageName[packageName.length] == '.') return true
            }
        }
        return false
    }

    /**
     * 是否为 import layout 中的特殊“其他 imports”占位项。
     */
    val isSpecial: Boolean get() = this == ALL_OTHER_IMPORTS_ENTRY || this == ALL_OTHER_ALIAS_IMPORTS_ENTRY

    /**
     * 返回用于 UI 和 XML 的包名文本。
     */
    override fun toString(): String {
        return packageName
    }

    /**
     * 比较包名和是否包含子包两个匹配维度。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CangJiePackageEntry) return false

        return withSubpackages == other.withSubpackages && packageName == other.packageName
    }

    /**
     * 使用包名作为条目的 hash code。
     */
    override fun hashCode(): Int = packageName.hashCode()
}

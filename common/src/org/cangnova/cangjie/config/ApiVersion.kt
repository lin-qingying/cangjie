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

package org.cangnova.cangjie.config

import org.cangnova.cangjie.LanguageOrApiVersion
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.util.DescriptionAware

class ApiVersion private constructor(
    val version: LanguageVersion,
    override val versionString: String
) : Comparable<ApiVersion>, DescriptionAware, LanguageOrApiVersion {

    override val isStable: Boolean
        get() = this <= LATEST_STABLE

    override val isDeprecated: Boolean
        get() = FIRST_SUPPORTED <= this && this < FIRST_NON_DEPRECATED

    override val isUnsupported: Boolean
        get() = this < FIRST_SUPPORTED

    override fun compareTo(other: ApiVersion): Int =
        version.compareTo(other.version)

    override fun equals(other: Any?) =
        (other as? ApiVersion)?.version == version

    override fun hashCode() =
        version.hashCode()

    override fun toString() = versionString

    companion object {
        @JvmField
        val CANGJIE_1_0_0 = createByLanguageVersion(LanguageVersion.CANGJIE_1_0_0)

        @JvmField
        val CANGJIE_1_0_5 = createByLanguageVersion(LanguageVersion.CANGJIE_1_0_5)

        @JvmField
        val CANGJIE_1_1_0 = createByLanguageVersion(LanguageVersion.CANGJIE_1_1_0)

        @JvmField
        val CANGJIE_1_1_3 = createByLanguageVersion(LanguageVersion.CANGJIE_1_1_3)

        @JvmField
        val LATEST: ApiVersion = createByLanguageVersion(LanguageVersion.entries.last())

        @JvmField
        val LATEST_STABLE: ApiVersion = createByLanguageVersion(LanguageVersion.LATEST_STABLE)

        @JvmField
        val FIRST_SUPPORTED: ApiVersion = createByLanguageVersion(LanguageVersion.FIRST_API_SUPPORTED)

        @JvmField
        val FIRST_NON_DEPRECATED: ApiVersion = createByLanguageVersion(LanguageVersion.FIRST_NON_DEPRECATED)

        @JvmStatic
        fun createByLanguageVersion(version: LanguageVersion): ApiVersion = parse(version.versionString)!!

        fun parse(versionString: String): ApiVersion? = try {
            ApiVersion(LanguageVersion.parse(versionString), versionString)
        } catch (e: Exception) {
            null
        }
    }
}

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

package org.cangnova.cangjie.psi

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.PsiElement
import kotlin.reflect.KProperty

/**
 * 表示 `UserDataProperty`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class UserDataProperty<in R : UserDataHolder, T : Any>(val key: Key<T>) {
    /**
     * 提供 `getValue` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    operator fun getValue(thisRef: R, desc: KProperty<*>) = thisRef.getUserData(key)

    /**
     * 提供 `setValue` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    operator fun setValue(thisRef: R, desc: KProperty<*>, value: T?) = thisRef.putUserData(key, value)
}
/**
 * 表示 `NotNullableUserDataProperty`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class NotNullableUserDataProperty<in R : UserDataHolder, T : Any>(val key: Key<T>, val defaultValue: T) {
    /**
     * 提供 `getValue` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    operator fun getValue(thisRef: R, desc: KProperty<*>) = thisRef.getUserData(key) ?: defaultValue

    /**
     * 提供 `setValue` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    operator fun setValue(thisRef: R, desc: KProperty<*>, value: T) {
        thisRef.putUserData(key, if (value != defaultValue) value else null)
    }
}
/**
 * 表示 `NotNullablePsiCopyableUserDataProperty`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class NotNullablePsiCopyableUserDataProperty<in R : PsiElement, T : Any>(val key: Key<T>, val defaultValue: T) {
    /**
     * 提供 `getValue` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    operator fun getValue(thisRef: R, property: KProperty<*>) = thisRef.getCopyableUserData(key) ?: defaultValue

    /**
     * 提供 `setValue` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    operator fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        thisRef.putCopyableUserData(key, if (value != defaultValue) value else null)
    }
}

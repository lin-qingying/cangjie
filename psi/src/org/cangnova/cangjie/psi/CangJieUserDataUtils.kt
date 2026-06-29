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
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * 表示 `UserDataCachedDelegate`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
private class UserDataCachedDelegate<in T : UserDataHolder, out V>(
    key: String,
    /**
     * 保存 `modificationStampFactory` 的内部状态，供仓颉 PSI实现维护节点缓存或解析上下文。
     */
    private val modificationStampFactory: (T) -> Long,
    /**
     * 保存 `valueFactory` 的内部状态，供仓颉 PSI实现维护节点缓存或解析上下文。
     */
    private val valueFactory: (T) -> V,
) : ReadOnlyProperty<T, V> {
    /**
     * 保存 `key` 的内部状态，供仓颉 PSI实现维护节点缓存或解析上下文。
     */
    private val key = Key<ValueHolder<V>>(key)

    /**
     * 表示 `ValueHolder`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
     */
    private class ValueHolder<V>(val value: V, val modificationStamp: Long)

    /**
     * 实现 `getValue` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getValue(thisRef: T, property: KProperty<*>): V {
        val cached = thisRef.getUserData(key)
        val modificationStamp = modificationStampFactory(thisRef)
        if (cached != null && modificationStamp == cached.modificationStamp) {
            return cached.value
        }

        val value = valueFactory(thisRef)
        thisRef.putUserData(key, ValueHolder(value, modificationStamp))
        return value
    }
}
/**
 * 提供 `userDataCached` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
 */
fun <T : UserDataHolder, V> userDataCached(key: String, modificationStampFactory: (T) -> Long, valueFactory: (T) -> V): ReadOnlyProperty<T, V> {
    return UserDataCachedDelegate(key, modificationStampFactory, valueFactory)
}

/**
 * 提供 `userDataCached` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
 */
fun <T : PsiElement, V> userDataCached(key: String, valueFactory: (T) -> V): ReadOnlyProperty<T, V> {
    return userDataCached(key, { it.containingFile.modificationStamp }, valueFactory)
}

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

package org.cangnova.cangjie.psi.stubs.impl

import org.cangnova.cangjie.psi.CjElementImplStub
import org.cangnova.cangjie.psi.stubs.CangJieCallableStubBase
import org.cangnova.cangjie.psi.stubs.CangJieClassifierStub
import org.cangnova.cangjie.psi.stubs.CangJiePlaceHolderWithTextStub
import org.cangnova.cangjie.psi.stubs.CangJieStubElement
import org.cangnova.cangjie.psi.stubs.CangJieStubWithFqName
import org.cangnova.cangjie.psi.stubs.CangJieTypeStatementStub
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.NamedStub
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import java.lang.reflect.Method

/**
 * 保存 `STUB_TO_STRING_PREFIX`，供PSI Stub流程读取节点结构或语义信息。
 */
const val STUB_TO_STRING_PREFIX = "CangJieStub$"

/**
 * 表示 `CangJieStubBaseImpl`，承载PSI Stub中的语法节点、索引桩或辅助模型。
 */
abstract class CangJieStubBaseImpl<T : CjElementImplStub<*>>(parent: StubElement<*>?, elementType: IStubElementType<*, *>) :
    StubBase<T>(parent, elementType), CangJieStubElement<T> {

    companion object {
        private val LOGGER: Logger = Logger.getInstance(CangJieStubBaseImpl::class.java)
        private val BASE_STUB_INTERFACES = listOf(
            CangJieStubWithFqName::class.java,
            CangJieClassifierStub::class.java,
            CangJieTypeStatementStub::class.java,
            NamedStub::class.java,
            CangJieCallableStubBase::class.java,
            CangJiePlaceHolderWithTextStub::class.java,
        )
    }

    /**
     * 提供 `copyInto` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    abstract override fun copyInto(newParent: StubElement<*>?): CangJieStubBaseImpl<T>

    /**
     * 实现 `getStubType` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    @Deprecated("Deprecated in Java")
    override fun getStubType(): IStubElementType<out StubElement<*>, *> =
        super.getStubType() as IStubElementType<out StubElement<*>, *>

    /**
     * 执行 `renderPropertyValues` 内部辅助逻辑，支撑PSI Stub节点的结构解析与访问。
     */
    private fun renderPropertyValues(stubInterface: Class<out Any?>): List<String> {
        return collectProperties(stubInterface).mapNotNull { property -> renderProperty(property) }.sorted()
    }

    /**
     * 执行 `getPropertyName` 内部辅助逻辑，支撑PSI Stub节点的结构解析与访问。
     */
    private fun getPropertyName(method: Method): String {
        val methodName = method.name
        if (methodName.startsWith("get")) {
            return methodName.substring(3).replaceFirstChar(Char::lowercaseChar)
        }
        return methodName
    }

    /**
     * 执行 `renderProperty` 内部辅助逻辑，支撑PSI Stub节点的结构解析与访问。
     */
    private fun renderProperty(property: Method): String? {
        return try {
            val value = property.invoke(this)
            val name = getPropertyName(property)
            "$name=$value"
        } catch (e: java.lang.reflect.InvocationTargetException) {
            // 打印真正的根因，而不是包装异常
            LOGGER.error("Failed to invoke ${property.name}: ${e.cause}", e.cause)
            null
        } catch (e: Exception) {
            LOGGER.error("Reflection error on ${property.name}", e)
            null
        }
    }

    /**
     * 执行 `collectProperties` 内部辅助逻辑，支撑PSI Stub节点的结构解析与访问。
     */
    private fun collectProperties(stubInterface: Class<*>): Collection<Method> {
        val result = ArrayList<Method>()
        result.addAll(stubInterface.declaredMethods.filter { it.parameterTypes.isEmpty() })
        for (baseInterface in stubInterface.interfaces) {
            if (baseInterface in BASE_STUB_INTERFACES) {
                result.addAll(collectProperties(baseInterface))
            }
        }
        return result
    }

    /**
     * 实现 `toString` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun toString(): String {
        val stubInterface = this::class.java.interfaces.single { it.name.contains("Stub") }
        val propertiesValues = renderPropertyValues(stubInterface)
        if (propertiesValues.isEmpty()) {
            return "$STUB_TO_STRING_PREFIX$stubType"
        }
        val properties = propertiesValues.joinToString(separator = ", ", prefix = "[", postfix = "]")
        return "$STUB_TO_STRING_PREFIX$stubType$properties"
    }
}

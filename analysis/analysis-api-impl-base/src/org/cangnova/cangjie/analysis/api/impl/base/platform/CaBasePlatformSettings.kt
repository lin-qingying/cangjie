package org.cangnova.cangjie.analysis.api.impl.base.platform

import org.cangnova.cangjie.analysis.api.platform.CaDeserializedDeclarationsOrigin
import org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings

/**
 * Analysis API 平台设置的基础实现。
 *
 * 该实现保留二进制 origin 语义，供 standalone / LSP / 测试宿主按需复用；
 * 产品插件不再通过 shared XML 直接装配它，而是由各宿主显式提供自己的 platform settings。
 */
internal class CaBasePlatformSettings : CaPlatformSettings {
    override val deserializedDeclarationsOrigin: CaDeserializedDeclarationsOrigin
        get() = CaDeserializedDeclarationsOrigin.BINARIES
}

package org.cangnova.cangjie.analysis.api.impl.base.platform

import org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings

/**
 * Analysis API 平台设置的基础实现。
 *
 * 当前项目尚未建立完整的库模块语义，因此先保持与现有模块模型一致，
 * 允许库模块作为 use-site 模块参与分析。后续在模块分层对齐 Kotlin 后，
 * 再由更具体的平台实现覆盖该策略。
 */
internal class CaBasePlatformSettings : CaPlatformSettings

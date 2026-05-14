package org.cangnova.cangjie.analysis.api.resolution

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.Name

// 调用实参映射的占位入口。
//
// 当前公开 API 中,实参映射由 CaFunctionCall.valueArgumentMapping /
// combinedArgumentMapping 直接暴露,本文件保留作为后续独立建模的演进锚点。

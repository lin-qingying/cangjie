package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.resolve.body.CfirBodyResolveContext
import org.cangnova.cangjie.cfir.resolve.inference.CfirInferenceComponents
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeSubtypeChecker
import org.cangnova.cangjie.name.FqName

/**
 * 调用解析上下文，为验证阶段提供所需环境信息。
 * 对齐 K2 `ResolutionContext`，但只暴露当前实现真正需要的服务。
 */
class CfirResolutionContext(
    /** 编译器会话。 */
    val session: CfirSession,
    /** Body resolve 上下文。 */
    val bodyResolveContext: CfirBodyResolveContext,
    /** 子类型检查器。 */
    val subtypeChecker: ConeSubtypeChecker,
    /** 泛型推断组件，主要供后续阶段使用。 */
    val inferenceComponents: CfirInferenceComponents? = null,
    /** 调用处的期望返回类型，用于泛型返回类型约束收集。 */
    val expectedType: ConeCangjieType? = null,
    /** 使用点所属文件路径，用于可见性检查。 */
    val containingFilePath: String? = null,
    /** 使用点所属包名，用于可见性检查。 */
    val containingPackageFqName: FqName? = null,
    /** 使用点所属类符号，用于可见性检查。 */
    val containingClassSymbol: CfirClassSymbol? = null,
)


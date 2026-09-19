package org.cangnova.cangjie.arguments.dsl.base

/**
 * 编译器参数 schema 可描述的仓颉发布版本。
 */
enum class CangJieReleaseVersion(
    /** 正式的 major.minor.patch 发布标识。 */
    val releaseName: String,
    /** 发布版本主号。 */
    val major: Int,
    /** 发布版本次号。 */
    val minor: Int,
    /** 发布版本修订号。 */
    val patch: Int,
) {
    /**
     * 仓颉 1.0.0 版本。
     *
     * 这是当前项目语言兼容矩阵的起点；不能用项目发布版本或最新稳定版本代替。
     */
    V_1_0_0("1.0.0", 1, 0, 0),

    /**
     * 仓颉 1.0.5 版本。
     */
    V_1_0_5("1.0.5", 1, 0, 5),

    /** 仓颉 1.1.0 版本。 */
    V_1_1_0("1.1.0", 1, 1, 0),

    /** 仓颉 1.1.3 版本。 */
    V_1_1_3("1.1.3", 1, 1, 3),

}

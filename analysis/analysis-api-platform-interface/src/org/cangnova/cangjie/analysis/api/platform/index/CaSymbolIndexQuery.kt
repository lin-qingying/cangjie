package org.cangnova.cangjie.analysis.api.platform.index

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CaPlatformComponent
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 符号索引条目的**身份**，不是索引键。
 *
 * 这里刻意不暴露任何具体索引的实现形态：没有 `ClassId`、没有 stub `fileKey`、
 * 没有 IntelliJ stub index 的 key 类型。索引是**可替换**的实现（IDE 侧跨会话持久、
 * 无平台侧会话内快照、`.cjo` 侧只读名字视图），一旦把某种索引的键写进契约，
 * 换实现就得改契约，调用方也会开始依赖某个索引的内部形状。
 *
 * 身份用仓颉自己的命名体系表达（`Name` + `FqName` + 种类），调用方拿到后自行
 * 经 `CaScopeProvider.getPackageScope` 解析成符号。
 */
@CaPlatformInterface
data class CaSymbolIndexEntry(
    /** 声明的短名。 */
    val name: Name,

    /** 声明的完整名；顶层声明用包名 + 短名，成员声明用所属类型完整名 + 短名。 */
    val fqName: FqName,

    /** 声明种类。 */
    val kind: CaSymbolIndexEntryKind,
) {
    /**
     * 声明所属的包名，等于 `fqName` 去掉最后一段短名。
     *
     * 索引条目一律是**顶层**语义（包索引查出来的就是包级声明），因此这段推导
     * 恒成立；把它做成属性而不是让每个调用方各写一次 `.dropLast(1)`。
     */
    val packageFqName: FqName
        get() = fqName.parent()
}

/**
 * 符号索引条目能表示的声明种类。
 *
 * 只区分**查询方需要区别对待**的几类：
 * - `CLASS_LIKE` 走类型候选通道，插入形态与顶层 callable 不同；
 * - `CALLABLE` 覆盖顶层函数/属性/宏，仓颉里三者插入形态一致；
 * - `ENUM_CONSTRUCTOR` 必须单列——仓颉允许同名不同 payload 个数，按名字去重
 *   会把两个合法枚举构造器合并成一个；
 * - `TYPE_ALIAS` 单列，因为补全插入后是别名引用而不是构造函数调用。
 *
 * 不区分可见性修饰符：可见性由 [CaSymbolIndexQuery] 的 scope 参数在查询期决定，
 * 放进条目会让同一条目在不同调用点有不同的身份。
 */
@CaPlatformInterface
enum class CaSymbolIndexEntryKind {
    /** 类、结构体、接口、枚举等类状声明。 */
    CLASS_LIKE,

    /** 顶层函数、属性与宏。 */
    CALLABLE,

    /** 枚举构造器（枚举值）。 */
    ENUM_CONSTRUCTOR,

    /** 类型别名。 */
    TYPE_ALIAS,
}

/**
 * 按名称反向查询声明的索引契约。
 *
 * 与已有的包枚举（`CangJiePackageProvider`）、按 id 正向查询
 * （`CangJieDeclarationProvider`）互补：那两个接口的入口是**已知**的名字或 id，
 * 本接口的入口是**只有前缀**，因此消费方是补全这类「用户正在敲名字」的场景。
 *
 * 实现方分三种，按宿主能力择一，**同一查询下三者必须返回同一符号集合**：
 * 1. IDE 侧：平台索引扩展，跨会话持久，随索引增量更新；
 * 2. 无平台侧：会话内 stub 快照，**不得宣称持久**；
 * 3. `.cjo` 侧：`cfir-serialization` 的只读名字视图，不另建索引。
 */
@CaPlatformInterface
interface CaSymbolIndexQuery : CaPlatformComponent {
    /**
     * 按**精确短名**查询声明。
     *
     * @param name 声明短名
     * @param analysisScope 分析范围；同时承担可见性限定——范围外的声明不返回
     * @return 命中的条目；无命中时返回空集合
     */
    fun getDeclarationsByName(name: Name, analysisScope: GlobalSearchScope): Collection<CaSymbolIndexEntry>

    /**
     * 按**名称前缀**查询声明。
     *
     * 前缀分级过滤（≤3 字符只做起始匹配，更长才叠加驼峰匹配）是**实现方**的义务：
     * 契约只承诺「返回的名字都以 `namePrefix` 开头」，不承诺用哪种匹配策略。
     * 这样消费方无法依赖某个索引的匹配细节，也不会因为索引换了策略而行为漂移。
     *
     * @param namePrefix 名称前缀
     * @param analysisScope 分析范围；同时承担可见性限定
     * @return 命中的条目；无命中时返回空集合
     */
    fun getDeclarationsByNamePrefix(namePrefix: String, analysisScope: GlobalSearchScope): Collection<CaSymbolIndexEntry>

    /**
     * 实现方的索引形态标识，供诊断与测试断言两侧等价性。
     *
     * 显式建模而不是靠 `::class` 判断：三种实现的持久性承诺不同
     * （IDE 侧持久、无平台侧仅会话内），调用方需要能**声明**自己拿到的是什么。
     */
    val indexPersistence: CaSymbolIndexPersistence
}

/**
 * 符号索引的持久性形态。
 */
@CaPlatformInterface
enum class CaSymbolIndexPersistence {
    /** 跨会话持久，随宿主索引增量更新。 */
    PERSISTENT,

    /** 仅当前会话内有效，不得当作持久索引使用。 */
    SESSION_ONLY,
}

/**
 * 为指定分析范围取得符号索引查询实例。
 *
 * 与 `createDeclarationProvider` 同形：查询实例绑定范围，调用方不必每次重复传 scope。
 */
@CaPlatformInterface
fun Project.createCaSymbolIndexQuery(analysisScope: GlobalSearchScope): CaSymbolIndexQuery =
    CaSymbolIndexQueryService.getInstance(this).createSymbolIndexQuery(analysisScope)

/**
 * 符号索引查询的平台服务。
 */
@CaPlatformInterface
interface CaSymbolIndexQueryService {
    /**
     * 为指定分析范围创建索引查询实例。
     *
     * 与 [org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProviderFactory]
     * 同形：实例是**范围绑定**的，因此调用方不必在每次查询时重复传 scope。
     */
    fun createSymbolIndexQuery(analysisScope: GlobalSearchScope): CaSymbolIndexQuery

    @CaPlatformInterface
    companion object {
        /**
         * 获取项目级符号索引查询工厂服务。
         */
        fun getInstance(project: Project): CaSymbolIndexQueryService = project.service()
    }
}
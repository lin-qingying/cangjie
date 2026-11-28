package org.cangnova.cangjie.analysis.api.platform.index

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CaPlatformComponent
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 名称发现条目：一条**声明身份**，不是索引键。
 *
 * 刻意不暴露任何具体索引的实现形态：没有 stub `fileKey`、没有 IntelliJ
 * stub index 的 key 类型、也没有虚拟文件。索引是可替换实现（IDE 侧跨会话持久、
 * 无平台侧会话内快照、`.cjo` 侧只读名字视图），一旦把某种索引的键写进契约，
 * 换实现就得改契约，调用方也会开始依赖某个索引的内部形状。
 *
 * 身份用仓颉自己的命名体系表达：所属包、短名、种类，外加枚举构造器所需的
 * 所属类型 ClassId。**符号恢复不属于索引职责**——那是 Analysis 层按包作用域
 * 查询符号的事，索引在这里返回符号就会迫使契约持有会话。
 */
@CaPlatformInterface
data class CaSymbolIndexEntry(
    /** 声明所属的包名。 */
    val packageFqName: FqName,

    /** 声明的短名。 */
    val name: Name,

    /** 声明种类。 */
    val kind: CaSymbolIndexEntryKind,

    /**
     * 成员声明所属类型的 ClassId；顶层声明为 `null`。
     *
     * 只有 [CaSymbolIndexEntryKind.ENUM_CONSTRUCTOR] 会带上它：枚举构造器是枚举的成员，
     * 恢复时必须先定位所属枚举。普通实例成员**不进入索引**——它们只在接收者已知时才有意义，
     * 由成员区段从类型作用域取，不是名称发现的输入。
     */
    val ownerClassId: ClassId? = null,
)

/**
 * 名称发现能表示的声明种类。
 *
 * 只区分**恢复时路径不同**的几类：class-like 与 typealias 走 ClassId 查询，
 * callable 走包级 callable 查询，枚举构造器要先定位所属类型再取其成员。
 * 不区分可见性修饰符：可见性由使用点的 Analysis 组件判断，
 * 放进条目会让同一条目在不同调用点有不同的身份。
 */
@CaPlatformInterface
enum class CaSymbolIndexEntryKind {
    /** 类、结构体、接口、枚举等类状声明。 */
    CLASS_LIKE,

    /** 类型别名。 */
    TYPE_ALIAS,

    /** 顶层函数、属性与宏。 */
    CALLABLE,

    /** 枚举构造器（枚举值）。 */
    ENUM_CONSTRUCTOR,
}

/**
 * 名称匹配规则。
 *
 * 规则本身属于**契约**而不是实现：三个实现必须返回同一符号集合，
 * 各自实现一套匹配就会漂移。前缀不超过三个字符且非重复触发时只做起始匹配，
 * 更长或重复触发时放宽到子串——短前缀下的子串匹配会召回过多候选。
 */
@CaPlatformInterface
data class CaSymbolIndexNameFilter(
    /** 名称前缀；空串表示不按前缀过滤。 */
    val prefix: String,

    /** 本次是否为重复触发（用户在上一次候选未变时再次请求）。 */
    val isRepeatedInvocation: Boolean = false,
) {
    /**
     * 判断名称是否命中。
     */
    fun matches(name: Name): Boolean {
        if (name.isSpecial) return false
        val text = name.asString()
        return if (prefix.length <= 3 && !isRepeatedInvocation) text.startsWith(prefix)
        else text.contains(prefix)
    }
}

/** 符号索引的持久性形态。 */
@CaPlatformInterface
enum class CaSymbolIndexPersistence {
    /** 跨会话持久，随宿主索引增量更新。 */
    PERSISTENT,

    /** 仅当前会话内有效，不得当作持久索引使用。 */
    SESSION_ONLY,
}

/**
 * 按名称发现声明。
 *
 * 与已有的包枚举（`CangJiePackageProvider`）和按 id 正向查询
 * （`CangJieDeclarationProvider`）互补：那两个接口的入口是**已知**的名字或 id，
 * 本接口的入口是**只有前缀**，因此消费方是补全这类「用户正在敲名字」的场景。
 *
 * 实现方分三种，按宿主能力择一，**同一查询下三者必须返回同一名称集合**：
 * 1. IDE 侧：平台索引扩展，跨会话持久，随索引增量更新；
 * 2. 无平台侧：会话内 stub 快照，**不得宣称持久**；
 * 3. `.cjo` 侧：`cfir-serialization` 的只读名字视图，不另建索引。
 *
 * 实现**不得**在查询期遍历工作区 PSI：候选发现若退化成全工作区扫描，
 * 每次键入都会付出与项目规模成正比的代价。
 */
@CaPlatformInterface
interface CaSymbolIndexQuery : CaPlatformComponent {
    /**
     * 按滤波条件发现声明，结果已按绑定的分析范围限定。
     *
     * @param filter 名称滤波条件
     * @return 命中的条目；无命中时返回空集合
     */
    fun findDeclarations(filter: CaSymbolIndexNameFilter): Collection<CaSymbolIndexEntry>

    /** 持久性是实现的承诺；会话内快照不能标为持久索引。 */
    val indexPersistence: CaSymbolIndexPersistence
}

/**
 * 为指定分析范围取得名称发现实例。
 *
 * 与 `createDeclarationProvider` 同形：实例是**范围绑定**的，
 * 调用方不必在每次查询时重复传 scope。
 */
@CaPlatformInterface
fun Project.createCaSymbolIndexQuery(analysisScope: GlobalSearchScope): CaSymbolIndexQuery =
    CaSymbolIndexQueryService.getInstance(this).createSymbolIndexQuery(analysisScope)

/**
 * 符号索引查询的平台服务。
 */
@CaPlatformInterface
interface CaSymbolIndexQueryService : CaPlatformComponent {
    /**
     * 为指定分析范围创建名称发现实例。
     */
    fun createSymbolIndexQuery(analysisScope: GlobalSearchScope): CaSymbolIndexQuery

    @CaPlatformInterface
    companion object {
        /**
         * 获取项目级名称发现工厂服务。
         */
        fun getInstance(project: Project): CaSymbolIndexQueryService = project.service()
    }
}

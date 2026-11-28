package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.name.Name

/**
 * nominal 直接声明的有效值成员，与普通名字查找和构造初始化共同使用。
 *
 * 官方 PreCheck 报告后声明的重定义，普通引用仍指向先声明成员。保留原声明树用于诊断，
 * 此视图只选取 lookup 可达的字段或属性；构造参数的隐式赋值也必须指向同一声明。
 */
class CfirDeclaredValueMemberIndex(declarations: List<CfirDeclaration>) {
    private data class Entry(val name: Name, val declaration: CfirCallableDeclaration, val ordinal: Int)

    private val byName: Map<Name, CfirCallableDeclaration> = declarations.mapIndexedNotNull { ordinal, declaration ->
        when (declaration) {
            is CfirProperty -> Entry(declaration.name, declaration, ordinal)
            is CfirFieldVariable -> Entry(declaration.name, declaration, ordinal)
            else -> null
        }
    }.groupBy { it.name }.mapValues { (_, entries) ->
        entries.minWith(
            compareBy<Entry> { it.declaration.source?.startOffset ?: Int.MAX_VALUE }
                .thenBy { if (it.declaration is CfirProperty) 0 else 1 }
                .thenBy { it.ordinal },
        ).declaration
    }

    /** 被名字查找选中的值成员，按名字首次出现的顺序返回。 */
    val members: Collection<CfirCallableDeclaration> get() = byName.values

    /** 初始化只跟踪真实字段；普通 prop 的访问器不作为字段存储。 */
    val fields: List<CfirFieldVariable> get() = byName.values.filterIsInstance<CfirFieldVariable>()

    operator fun get(name: Name): CfirCallableDeclaration? = byName[name]
}

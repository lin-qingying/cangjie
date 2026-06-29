

package org.cangnova.cangjie.generators.tree.imports

/**
 * 可被生成器加入 import 列表的类型或声明引用。
 */
interface Importable {

    /**
     * 要导入的实体名称。
     */
    val typeName: String

    /**
     * 要导入实体所在的包名。
     */
    val packageName: String
}

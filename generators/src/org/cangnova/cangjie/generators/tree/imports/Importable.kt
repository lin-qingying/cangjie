

package org.cangnova.cangjie.generators.tree.imports

interface Importable {

    /**
     * 要导入的实体名称。
     */
    val typeName: String

    val packageName: String
}

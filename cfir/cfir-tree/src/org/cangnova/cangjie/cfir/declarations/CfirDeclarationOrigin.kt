package org.cangnova.cangjie.cfir.declarations

/**
 * 声明来源，标记一个 CFIR 声明是怎样产生的。
 */
sealed class CfirDeclarationOrigin {
    /** 来源于用户编写的源码 */
    object Source : CfirDeclarationOrigin() {
        override fun toString(): String = "Source"
    }

    /** 来源于已编译的库 */
    object Library : CfirDeclarationOrigin() {
        override fun toString(): String = "Library"
    }

    /** 编译器合成的声明 */
    object Synthetic : CfirDeclarationOrigin() {
        override fun toString(): String = "Synthetic"
    }

    /** 默认构造函数等编译器隐式生成 */
    object ImplicitDefault : CfirDeclarationOrigin() {
        override fun toString(): String = "ImplicitDefault"
    }

    /** 泛型实例化生成的声明 */
    object GenericInstantiation : CfirDeclarationOrigin() {
        override fun toString(): String = "GenericInstantiation"
    }

    /** 来自 extend 声明的合成成员 */
    object Extension : CfirDeclarationOrigin() {
        override fun toString(): String = "Extension"
    }
}

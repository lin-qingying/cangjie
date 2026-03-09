package org.cangjie.cfir.visitors

import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.declarations.*
import org.cangjie.cfir.expressions.*
import org.cangjie.cfir.patterns.*
import org.cangjie.cfir.references.*
import org.cangjie.cfir.types.*

/**
 * CFIR 转换器基类。
 *
 * 与 CfirVisitor 类似，但每个 visit/transform 方法返回一个（可能被替换的）节点。
 * 用于分阶段解析中对 CFIR 树进行就地修改。
 */
abstract class CfirTransformer<in D> : CfirVisitor<CfirElement, D>() {

    /** 通用转换方法 */
    abstract fun <E : CfirElement> transformElement(element: E, data: D): E

    override fun visitElement(element: CfirElement, data: D): CfirElement =
        transformElement(element, data)

    // ---- 声明 ----

    open fun transformDeclaration(declaration: CfirDeclaration, data: D): CfirDeclaration =
        transformElement(declaration, data)

    override fun visitFile(file: CfirFile, data: D): CfirElement {
        file.transformChildren(this, data)
        return transformDeclaration(file, data)
    }

    override fun visitClass(klass: CfirClass, data: D): CfirElement {
        klass.transformChildren(this, data)
        return transformDeclaration(klass, data)
    }

    override fun visitEnumEntry(enumEntry: CfirEnumEntry, data: D): CfirElement {
        enumEntry.transformChildren(this, data)
        return transformDeclaration(enumEntry, data)
    }

    override fun visitExtend(extend: CfirExtend, data: D): CfirElement {
        extend.transformChildren(this, data)
        return transformDeclaration(extend, data)
    }

    override fun visitTypeAlias(typeAlias: CfirTypeAlias, data: D): CfirElement {
        typeAlias.transformChildren(this, data)
        return transformDeclaration(typeAlias, data)
    }

    override fun visitFunction(function: CfirFunction, data: D): CfirElement {
        function.transformChildren(this, data)
        return transformDeclaration(function, data)
    }

    override fun visitConstructor(constructor: CfirConstructor, data: D): CfirElement {
        constructor.transformChildren(this, data)
        return transformDeclaration(constructor, data)
    }

    override fun visitProperty(property: CfirProperty, data: D): CfirElement {
        property.transformChildren(this, data)
        return transformDeclaration(property, data)
    }

    override fun visitVariable(variable: CfirVariable, data: D): CfirElement {
        variable.transformChildren(this, data)
        return transformDeclaration(variable, data)
    }

    override fun visitValueParameter(valueParameter: CfirValueParameter, data: D): CfirElement {
        valueParameter.transformChildren(this, data)
        return transformDeclaration(valueParameter, data)
    }

    override fun visitTypeParameter(typeParameter: CfirTypeParameter, data: D): CfirElement {
        typeParameter.transformChildren(this, data)
        return transformDeclaration(typeParameter, data)
    }

    override fun visitAnonymousInitializer(anonymousInitializer: CfirAnonymousInitializer, data: D): CfirElement {
        anonymousInitializer.transformChildren(this, data)
        return transformDeclaration(anonymousInitializer, data)
    }

    // ---- 表达式 ----

    open fun transformExpression(expression: CfirExpression, data: D): CfirExpression =
        transformElement(expression, data)

    override fun visitBlock(block: CfirBlock, data: D): CfirElement {
        block.transformChildren(this, data)
        return transformExpression(block, data)
    }

    override fun visitIfExpression(ifExpression: CfirIfExpression, data: D): CfirElement {
        ifExpression.transformChildren(this, data)
        return transformExpression(ifExpression, data)
    }

    override fun visitMatchExpression(matchExpression: CfirMatchExpression, data: D): CfirElement {
        matchExpression.transformChildren(this, data)
        return transformExpression(matchExpression, data)
    }

    override fun visitFunctionCall(call: CfirFunctionCall, data: D): CfirElement {
        call.transformChildren(this, data)
        return transformExpression(call, data)
    }

    override fun visitAssignment(assignment: CfirAssignment, data: D): CfirElement {
        assignment.transformChildren(this, data)
        return transformExpression(assignment, data)
    }

    // 其余表达式类型默认走 visitExpression → transformExpression 路径
}

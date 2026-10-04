package sample.performance

// 逐组件耗时用例的测试数据：形状与 diagnosticsCollectionStagesAreRecorded.cj 一致，
// 目的是让声明 / 表达式 / 类型三个组件都在足够多的 CFIR 元素上被执行到。
class Shape {
    var width: Int64 = 0

    init(width: Int64) {
        this.width = width
    }

    func scaled(factor: Int64): Int64 {
        return width * factor
    }
}

func makeShape(width: Int64): Shape {
    return Shape(width)
}

func areaOf(shape: Shape, factor: Int64): Int64 {
    return shape.scaled(factor) + shape.width
}

main() {
    let shape = makeShape(3)
    areaOf(shape, 2)
}
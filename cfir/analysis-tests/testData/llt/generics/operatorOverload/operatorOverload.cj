// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class Point {
    public var x: Int64 = 0
    public var y: Int64 = 0
    public init(x: Int64, y: Int64) {
        this.x = x
        this.y = y
    }
    public operator func -(): Point {
        Point(-this.x, -this.y)
    }
    public operator func +(right: Point): Point {
        Point(this.x + right.x, this.y + right.y)
    }
}

struct TestRecord {
    public TestRecord(let x!: Int64 = 2, let y!: Int64 = 5) {}
    public func nested(): Int64 {
        let p1 = TestRecord(x: 8, y: 24)
        let p2 = -p1
        return p2.x
    }
    public operator func -(): TestRecord {
        TestRecord(x: -this.x, y: -this.y)
    }
    public operator func +(right: TestRecord): TestRecord {
        TestRecord(x: this.x + right.x, y: this.y + right.y)
    }
}

interface TestInterface {
    func Cons(x: Int64, y: Int64): TestInterface
    func GetX(): Int64
    func SetX(val: Int64): Unit
    func GetY(): Int64
    func SetY(val: Int64): Unit
    operator func -(): TestInterface {
        let a: Int64 = -this.GetX()
        let b: Int64 = -this.GetY()
        return this.Cons(a, b)
    }

    operator func +(right: TestInterface): TestInterface {
        this.Cons(this.GetX() + right.GetX(), this.GetY() + right.GetY())
    }
}

class BaseClass <: TestInterface {
    private var x: Int64 = 0
    private var y: Int64 = 0
    public init(x: Int64, y: Int64) {
        this.x = x
        this.y = y
    }
    public func GetX(): Int64 {
        return x
    }
    public func GetY(): Int64 {
        return y
    }
    public func SetX(val: Int64): Unit {
        this.x = val
    }
    public func SetY(val: Int64): Unit {
        this.y = val
    }
    public func Cons(x: Int64, y: Int64): TestInterface {
        return BaseClass(x, y)
    }
}

func ClassTest(): Int64 {
    let p1 = Point(8, 24)
    let p2 = -p1
    let p3 = p2 + p1
    return p3.x + p3.y
}

func RecordTest(): Int64 {
    let p1 = TestRecord(x: 8, y: 24)
    let p2 = -p1
    let p3 = p2 + p1
    return p3.x + p3.y + p1.nested()
}

func InterfaceTest(): Int64 {
    let p1: TestInterface = BaseClass(8, 24)
    let p2 = -p1
    let p3 = p2 + p1
    return p3.GetX() + p3.GetY()
}

interface Num<T> {
    operator func *(b: Bool): T
}

extend Int64 <: Num<Int64> {
    public operator func *(b: Bool): Int64 {
        return this
    }
}

func run(): Int64 {
    var a: Int64 = 1
    let x: Int64 = a * true
    return x
}

main(): Int64 {
    let one = ClassTest()
    let two = RecordTest()
    let three = InterfaceTest()
    let four = run()
    if (one + two + three + four == -7) {
        return 0
    } else {
        return 1
    }
}

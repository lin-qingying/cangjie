// RUN_PIPELINE_TILL: FRONTEND
// 大文件场景：一个文件里连续声明 60 个类 / 结构 / 枚举 / 接口 / 函数，每个成员体都带不同的表达式形态，
// 用于覆盖 CFIR 构树时的声明列表、大量同构子节点、跨行表达式与成员访问链。cjc 1.0.5 / 1.1.3 零诊断。
class Node01 {
    var id: Int64 = 1
    var name: String = "node-01"
    let tag: Int64 = 2

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node01) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node01 {
        return Node01(id, "static")
    }
}

class Node02 {
    var id: Int64 = 2
    var name: String = "node-02"
    let tag: Int64 = 4

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node02) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node02 {
        return Node02(id, "static")
    }
}

class Node03 {
    var id: Int64 = 3
    var name: String = "node-03"
    let tag: Int64 = 6

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node03) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node03 {
        return Node03(id, "static")
    }
}

class Node04 {
    var id: Int64 = 4
    var name: String = "node-04"
    let tag: Int64 = 8

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node04) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node04 {
        return Node04(id, "static")
    }
}

class Node05 {
    var id: Int64 = 5
    var name: String = "node-05"
    let tag: Int64 = 10

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node05) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node05 {
        return Node05(id, "static")
    }
}

class Node06 {
    var id: Int64 = 6
    var name: String = "node-06"
    let tag: Int64 = 12

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node06) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node06 {
        return Node06(id, "static")
    }
}

class Node07 {
    var id: Int64 = 7
    var name: String = "node-07"
    let tag: Int64 = 14

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node07) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node07 {
        return Node07(id, "static")
    }
}

class Node08 {
    var id: Int64 = 8
    var name: String = "node-08"
    let tag: Int64 = 16

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node08) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node08 {
        return Node08(id, "static")
    }
}

class Node09 {
    var id: Int64 = 9
    var name: String = "node-09"
    let tag: Int64 = 18

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node09) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node09 {
        return Node09(id, "static")
    }
}

class Node10 {
    var id: Int64 = 10
    var name: String = "node-10"
    let tag: Int64 = 20

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node10) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node10 {
        return Node10(id, "static")
    }
}

class Node11 {
    var id: Int64 = 11
    var name: String = "node-11"
    let tag: Int64 = 22

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node11) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node11 {
        return Node11(id, "static")
    }
}

class Node12 {
    var id: Int64 = 12
    var name: String = "node-12"
    let tag: Int64 = 24

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node12) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node12 {
        return Node12(id, "static")
    }
}

class Node13 {
    var id: Int64 = 13
    var name: String = "node-13"
    let tag: Int64 = 26

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node13) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node13 {
        return Node13(id, "static")
    }
}

class Node14 {
    var id: Int64 = 14
    var name: String = "node-14"
    let tag: Int64 = 28

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node14) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node14 {
        return Node14(id, "static")
    }
}

class Node15 {
    var id: Int64 = 15
    var name: String = "node-15"
    let tag: Int64 = 30

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node15) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node15 {
        return Node15(id, "static")
    }
}

class Node16 {
    var id: Int64 = 16
    var name: String = "node-16"
    let tag: Int64 = 32

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node16) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node16 {
        return Node16(id, "static")
    }
}

class Node17 {
    var id: Int64 = 17
    var name: String = "node-17"
    let tag: Int64 = 34

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node17) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node17 {
        return Node17(id, "static")
    }
}

class Node18 {
    var id: Int64 = 18
    var name: String = "node-18"
    let tag: Int64 = 36

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node18) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node18 {
        return Node18(id, "static")
    }
}

class Node19 {
    var id: Int64 = 19
    var name: String = "node-19"
    let tag: Int64 = 38

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node19) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node19 {
        return Node19(id, "static")
    }
}

class Node20 {
    var id: Int64 = 20
    var name: String = "node-20"
    let tag: Int64 = 40

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node20) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node20 {
        return Node20(id, "static")
    }
}

class Node21 {
    var id: Int64 = 21
    var name: String = "node-21"
    let tag: Int64 = 42

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node21) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node21 {
        return Node21(id, "static")
    }
}

class Node22 {
    var id: Int64 = 22
    var name: String = "node-22"
    let tag: Int64 = 44

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node22) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node22 {
        return Node22(id, "static")
    }
}

class Node23 {
    var id: Int64 = 23
    var name: String = "node-23"
    let tag: Int64 = 46

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node23) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node23 {
        return Node23(id, "static")
    }
}

class Node24 {
    var id: Int64 = 24
    var name: String = "node-24"
    let tag: Int64 = 48

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node24) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node24 {
        return Node24(id, "static")
    }
}

class Node25 {
    var id: Int64 = 25
    var name: String = "node-25"
    let tag: Int64 = 50

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node25) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node25 {
        return Node25(id, "static")
    }
}

class Node26 {
    var id: Int64 = 26
    var name: String = "node-26"
    let tag: Int64 = 52

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node26) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node26 {
        return Node26(id, "static")
    }
}

class Node27 {
    var id: Int64 = 27
    var name: String = "node-27"
    let tag: Int64 = 54

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node27) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node27 {
        return Node27(id, "static")
    }
}

class Node28 {
    var id: Int64 = 28
    var name: String = "node-28"
    let tag: Int64 = 56

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node28) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node28 {
        return Node28(id, "static")
    }
}

class Node29 {
    var id: Int64 = 29
    var name: String = "node-29"
    let tag: Int64 = 58

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node29) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node29 {
        return Node29(id, "static")
    }
}

class Node30 {
    var id: Int64 = 30
    var name: String = "node-30"
    let tag: Int64 = 60

    public init(id: Int64, name: String) {
        this.id = id
        this.name = name
    }

    public init(other: Node30) {
        this(other.id, other.name)
    }

    public func describe(): String {
        return "#%d:" + name + ":" + id.toString() + ":" + tag.toString()
    }

    public mut prop doubled: Int64 {
        get() { return id * 2 }
        set(v) { id = v }
    }

    public static func make(id: Int64): Node30 {
        return Node30(id, "static")
    }
}

struct Rec01 {
    var a: Int64 = 0
    var b: String = ""

    init() {}
    init(a: Int64, b!: String) { this.a = a; this.b = b }

    public func weight(): Int64 {
        return a * 1 + b.size
    }
}

struct Rec02 {
    var a: Int64 = 0
    var b: String = ""

    init() {}
    init(a: Int64, b!: String) { this.a = a; this.b = b }

    public func weight(): Int64 {
        return a * 2 + b.size
    }
}

struct Rec03 {
    var a: Int64 = 0
    var b: String = ""

    init() {}
    init(a: Int64, b!: String) { this.a = a; this.b = b }

    public func weight(): Int64 {
        return a * 3 + b.size
    }
}

struct Rec04 {
    var a: Int64 = 0
    var b: String = ""

    init() {}
    init(a: Int64, b!: String) { this.a = a; this.b = b }

    public func weight(): Int64 {
        return a * 4 + b.size
    }
}

struct Rec05 {
    var a: Int64 = 0
    var b: String = ""

    init() {}
    init(a: Int64, b!: String) { this.a = a; this.b = b }

    public func weight(): Int64 {
        return a * 5 + b.size
    }
}

struct Rec06 {
    var a: Int64 = 0
    var b: String = ""

    init() {}
    init(a: Int64, b!: String) { this.a = a; this.b = b }

    public func weight(): Int64 {
        return a * 6 + b.size
    }
}

struct Rec07 {
    var a: Int64 = 0
    var b: String = ""

    init() {}
    init(a: Int64, b!: String) { this.a = a; this.b = b }

    public func weight(): Int64 {
        return a * 7 + b.size
    }
}

struct Rec08 {
    var a: Int64 = 0
    var b: String = ""

    init() {}
    init(a: Int64, b!: String) { this.a = a; this.b = b }

    public func weight(): Int64 {
        return a * 8 + b.size
    }
}

struct Rec09 {
    var a: Int64 = 0
    var b: String = ""

    init() {}
    init(a: Int64, b!: String) { this.a = a; this.b = b }

    public func weight(): Int64 {
        return a * 9 + b.size
    }
}

struct Rec10 {
    var a: Int64 = 0
    var b: String = ""

    init() {}
    init(a: Int64, b!: String) { this.a = a; this.b = b }

    public func weight(): Int64 {
        return a * 10 + b.size
    }
}

struct Rec11 {
    var a: Int64 = 0
    var b: String = ""

    init() {}
    init(a: Int64, b!: String) { this.a = a; this.b = b }

    public func weight(): Int64 {
        return a * 11 + b.size
    }
}

struct Rec12 {
    var a: Int64 = 0
    var b: String = ""

    init() {}
    init(a: Int64, b!: String) { this.a = a; this.b = b }

    public func weight(): Int64 {
        return a * 12 + b.size
    }
}

struct Rec13 {
    var a: Int64 = 0
    var b: String = ""

    init() {}
    init(a: Int64, b!: String) { this.a = a; this.b = b }

    public func weight(): Int64 {
        return a * 13 + b.size
    }
}

struct Rec14 {
    var a: Int64 = 0
    var b: String = ""

    init() {}
    init(a: Int64, b!: String) { this.a = a; this.b = b }

    public func weight(): Int64 {
        return a * 14 + b.size
    }
}

struct Rec15 {
    var a: Int64 = 0
    var b: String = ""

    init() {}
    init(a: Int64, b!: String) { this.a = a; this.b = b }

    public func weight(): Int64 {
        return a * 15 + b.size
    }
}

enum Kind01 {
    | A01
    | B01(Int64)
    | C01(Int64, String)

    public func value(): Int64 {
        return match (this) {
            case A01 => 1
            case B01(v) => v
            case C01(v, _) => v
        }
    }
}

enum Kind02 {
    | A02
    | B02(Int64)
    | C02(Int64, String)

    public func value(): Int64 {
        return match (this) {
            case A02 => 2
            case B02(v) => v
            case C02(v, _) => v
        }
    }
}

enum Kind03 {
    | A03
    | B03(Int64)
    | C03(Int64, String)

    public func value(): Int64 {
        return match (this) {
            case A03 => 3
            case B03(v) => v
            case C03(v, _) => v
        }
    }
}

enum Kind04 {
    | A04
    | B04(Int64)
    | C04(Int64, String)

    public func value(): Int64 {
        return match (this) {
            case A04 => 4
            case B04(v) => v
            case C04(v, _) => v
        }
    }
}

enum Kind05 {
    | A05
    | B05(Int64)
    | C05(Int64, String)

    public func value(): Int64 {
        return match (this) {
            case A05 => 5
            case B05(v) => v
            case C05(v, _) => v
        }
    }
}

enum Kind06 {
    | A06
    | B06(Int64)
    | C06(Int64, String)

    public func value(): Int64 {
        return match (this) {
            case A06 => 6
            case B06(v) => v
            case C06(v, _) => v
        }
    }
}

enum Kind07 {
    | A07
    | B07(Int64)
    | C07(Int64, String)

    public func value(): Int64 {
        return match (this) {
            case A07 => 7
            case B07(v) => v
            case C07(v, _) => v
        }
    }
}

enum Kind08 {
    | A08
    | B08(Int64)
    | C08(Int64, String)

    public func value(): Int64 {
        return match (this) {
            case A08 => 8
            case B08(v) => v
            case C08(v, _) => v
        }
    }
}

enum Kind09 {
    | A09
    | B09(Int64)
    | C09(Int64, String)

    public func value(): Int64 {
        return match (this) {
            case A09 => 9
            case B09(v) => v
            case C09(v, _) => v
        }
    }
}

enum Kind10 {
    | A10
    | B10(Int64)
    | C10(Int64, String)

    public func value(): Int64 {
        return match (this) {
            case A10 => 10
            case B10(v) => v
            case C10(v, _) => v
        }
    }
}

interface Shape01 {
    func area(): Int64
    func name(): String { return "shape-01" }
}

interface Shape02 {
    func area(): Int64
    func name(): String { return "shape-02" }
}

interface Shape03 {
    func area(): Int64
    func name(): String { return "shape-03" }
}

interface Shape04 {
    func area(): Int64
    func name(): String { return "shape-04" }
}

interface Shape05 {
    func area(): Int64
    func name(): String { return "shape-05" }
}

interface Shape06 {
    func area(): Int64
    func name(): String { return "shape-06" }
}

interface Shape07 {
    func area(): Int64
    func name(): String { return "shape-07" }
}

open class Base01 {
    public init() {}

    public open func run(x: Int64): Int64 { return x + 1 }
}

class Derived01 <: Base01 {
    public init() { super() }

    public override func run(x: Int64): Int64 {
        if (x > 0) {
            return super.run(x) * 2
        } else {
            return -x
        }
    }
}

open class Base02 {
    public init() {}

    public open func run(x: Int64): Int64 { return x + 2 }
}

class Derived02 <: Base02 {
    public init() { super() }

    public override func run(x: Int64): Int64 {
        if (x > 0) {
            return super.run(x) * 2
        } else {
            return -x
        }
    }
}

open class Base03 {
    public init() {}

    public open func run(x: Int64): Int64 { return x + 3 }
}

class Derived03 <: Base03 {
    public init() { super() }

    public override func run(x: Int64): Int64 {
        if (x > 0) {
            return super.run(x) * 2
        } else {
            return -x
        }
    }
}

open class Base04 {
    public init() {}

    public open func run(x: Int64): Int64 { return x + 4 }
}

class Derived04 <: Base04 {
    public init() { super() }

    public override func run(x: Int64): Int64 {
        if (x > 0) {
            return super.run(x) * 2
        } else {
            return -x
        }
    }
}

open class Base05 {
    public init() {}

    public open func run(x: Int64): Int64 { return x + 5 }
}

class Derived05 <: Base05 {
    public init() { super() }

    public override func run(x: Int64): Int64 {
        if (x > 0) {
            return super.run(x) * 2
        } else {
            return -x
        }
    }
}

open class Base06 {
    public init() {}

    public open func run(x: Int64): Int64 { return x + 6 }
}

class Derived06 <: Base06 {
    public init() { super() }

    public override func run(x: Int64): Int64 {
        if (x > 0) {
            return super.run(x) * 2
        } else {
            return -x
        }
    }
}

open class Base07 {
    public init() {}

    public open func run(x: Int64): Int64 { return x + 7 }
}

class Derived07 <: Base07 {
    public init() { super() }

    public override func run(x: Int64): Int64 {
        if (x > 0) {
            return super.run(x) * 2
        } else {
            return -x
        }
    }
}

open class Base08 {
    public init() {}

    public open func run(x: Int64): Int64 { return x + 8 }
}

class Derived08 <: Base08 {
    public init() { super() }

    public override func run(x: Int64): Int64 {
        if (x > 0) {
            return super.run(x) * 2
        } else {
            return -x
        }
    }
}

func top01(x: Int64, s: String): String {
    let a = x + 1
    let b = s + "-1"
    var c = a
    while (c < 10) {
        c += 2
    }
    for (idx in 0..3) {
        c += idx
    }
    return b + c.toString()
}

func top02(x: Int64, s: String): String {
    let a = x + 2
    let b = s + "-2"
    var c = a
    while (c < 20) {
        c += 2
    }
    for (idx in 0..3) {
        c += idx
    }
    return b + c.toString()
}

func top03(x: Int64, s: String): String {
    let a = x + 3
    let b = s + "-3"
    var c = a
    while (c < 30) {
        c += 2
    }
    for (idx in 0..3) {
        c += idx
    }
    return b + c.toString()
}

func top04(x: Int64, s: String): String {
    let a = x + 4
    let b = s + "-4"
    var c = a
    while (c < 40) {
        c += 2
    }
    for (idx in 0..3) {
        c += idx
    }
    return b + c.toString()
}

func top05(x: Int64, s: String): String {
    let a = x + 5
    let b = s + "-5"
    var c = a
    while (c < 50) {
        c += 2
    }
    for (idx in 0..3) {
        c += idx
    }
    return b + c.toString()
}

func top06(x: Int64, s: String): String {
    let a = x + 6
    let b = s + "-6"
    var c = a
    while (c < 60) {
        c += 2
    }
    for (idx in 0..3) {
        c += idx
    }
    return b + c.toString()
}

func top07(x: Int64, s: String): String {
    let a = x + 7
    let b = s + "-7"
    var c = a
    while (c < 70) {
        c += 2
    }
    for (idx in 0..3) {
        c += idx
    }
    return b + c.toString()
}

func top08(x: Int64, s: String): String {
    let a = x + 8
    let b = s + "-8"
    var c = a
    while (c < 80) {
        c += 2
    }
    for (idx in 0..3) {
        c += idx
    }
    return b + c.toString()
}

func top09(x: Int64, s: String): String {
    let a = x + 9
    let b = s + "-9"
    var c = a
    while (c < 90) {
        c += 2
    }
    for (idx in 0..3) {
        c += idx
    }
    return b + c.toString()
}

func top10(x: Int64, s: String): String {
    let a = x + 10
    let b = s + "-10"
    var c = a
    while (c < 100) {
        c += 2
    }
    for (idx in 0..3) {
        c += idx
    }
    return b + c.toString()
}

func top11(x: Int64, s: String): String {
    let a = x + 11
    let b = s + "-11"
    var c = a
    while (c < 110) {
        c += 2
    }
    for (idx in 0..3) {
        c += idx
    }
    return b + c.toString()
}

func top12(x: Int64, s: String): String {
    let a = x + 12
    let b = s + "-12"
    var c = a
    while (c < 120) {
        c += 2
    }
    for (idx in 0..3) {
        c += idx
    }
    return b + c.toString()
}


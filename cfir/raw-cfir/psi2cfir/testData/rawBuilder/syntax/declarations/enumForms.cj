// RUN_PIPELINE_TILL: FRONTEND
// enum：竖线分隔的构造器（首行可省竖号、单行 / 多行 / 混写）、无参与有参构造器、非穷尽构造器 ...（只能在最后）、递归定义、
// 泛型 enum、enum 成员（函数 / prop / static 函数 / operator 函数）、构造值写法（Enum.Ctor / 裸 Ctor / Ctor(args)，
// 泛型 enum 的构造器必须带类型实参）。
// 官方依据：Cangjie 文档《枚举类型》；cjc 1.0.5 / 1.1.3 零诊断（同名构造器官方文档允许但 1.0.5 / 1.1.3 报
// sema_multiple_constructor_in_enum，见 recovery/enumDuplicateConstructor.cj）。
enum SingleLine {
    | Red | Green | Blue
}

enum MultiLine {
    | A
    | B
    | C
}

enum Mixed {
    A
    | B(Int64)
    | C(String, Bool)
}

enum NonExhaustive {
    | A | B | ...
}

enum Recursive {
    | Num(Int64)
    | Add(Recursive, Recursive)
    | Sub(Recursive, Recursive)
}

enum Generic<T> {
    | Some(T)
    | None
}

enum WithMembers {
    | A
    | B(Int64)

    func weight(): Int64 {
        return match (this) {
            case A => 0
            case B(v) => v
        }
    }

    static func first(): WithMembers {
        return A
    }

    prop isA: Bool {
        get() {
            return match (this) {
                case A => true
                case _ => false
            }
        }
    }

    public operator func +(other: WithMembers): WithMembers {
        return other
    }
}

func enumUses(): Unit {
    let a = SingleLine.Red
    let b = Green
    let c = MultiLine.B
    let d = Mixed.A
    let e = NonExhaustive.A
    let g = Generic<Int64>.Some(1)
    let h = Generic<Int64>.None
    let i = WithMembers.A
    let j = WithMembers.B(2)
    let _ = (a, b, c, d, e, g, h, i, j)
}

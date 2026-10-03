// RUN_PIPELINE_TILL: FRONTEND
// 可重载的操作符函数全集（官方《操作符重载》表）：() [] []= ! - ** * / % + - << >> < <= > >= == != & ^ |，一元与二元形式，
// 分别放在 class / struct / interface / enum / extend 中；`[]=` 用具名形参 value! 表达（只允许在可变类型即 class 上）。
// 不可重载的名字（get / set / ++ / -- / ~ / && / ||）见 recovery/operatorNotOverloadable.cj。
// 官方依据：Cangjie 文档《操作符重载》「可以被重载的操作符」；cjc 1.0.5 / 1.1.3 零诊断。
class Num {
    var v: Int64 = 0

    public init(v: Int64) {
        this.v = v
    }

    public operator func + (o: Num): Num {
        return o
    }

    public operator func - (o: Num): Num {
        return o
    }

    public operator func * (o: Num): Num {
        return o
    }

    public operator func / (o: Num): Num {
        return o
    }

    public operator func % (o: Num): Num {
        return o
    }

    public operator func ** (o: Num): Num {
        return o
    }

    public operator func << (o: Num): Num {
        return o
    }

    public operator func >> (o: Num): Num {
        return o
    }

    public operator func & (o: Num): Num {
        return o
    }

    public operator func ^ (o: Num): Num {
        return o
    }

    public operator func | (o: Num): Num {
        return o
    }

    public operator func < (o: Num): Bool {
        return true
    }

    public operator func <= (o: Num): Bool {
        return true
    }

    public operator func > (o: Num): Bool {
        return true
    }

    public operator func >= (o: Num): Bool {
        return true
    }

    public operator func == (o: Num): Bool {
        return true
    }

    public operator func != (o: Num): Bool {
        return true
    }

    public operator func - (): Num {
        return this
    }

    public operator func ! (): Bool {
        return true
    }

    public operator func [] (i: Int64): Int64 {
        return v
    }

    public operator func () (): Int64 {
        return v
    }
}

class IndexSet {
    var values: Array<Int64> = [0]

    public operator func [] (i: Int64): Int64 {
        return values[i]
    }

    public operator func [] (i: Int64, value!: Int64): Unit {
        values[i] = value
    }
}

struct Pt {
    var x: Int64 = 0
}

extend Pt {
    public operator func + (o: Pt): Pt {
        return o
    }

    public operator func == (o: Pt): Bool {
        return true
    }
}

interface Eq {
    public operator func != (o: Eq): Bool {
        return true
    }
}

enum Op {
    | A

    public operator func + (o: Op): Op {
        return o
    }
}

func operatorOverloadUses(): Unit {
    let a = Num(1)
    let b = a + a
    let c = a - a
    let d = a * a
    let e = a / a
    let f = a % a
    let g = a ** a
    let h = a << a
    let i = a >> a
    let j = a & a
    let k = a ^ a
    let l = a | a
    let m = a < a
    let n = a <= a
    let o = a > a
    let p = a >= a
    let q = a == a
    let r = a != a
    let s = -a
    let t = !a
    let u = a[0]
    let w = a()
    let set = IndexSet()
    let x = set[0]
    set[0] = 1
    let _ = (b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, w, x)
}

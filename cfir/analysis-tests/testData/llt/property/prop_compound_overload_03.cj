// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

open class A {
    var a_ = 0
    public mut open prop a: Int64 {
        get() {
            a_
        }
        set(v) {
            a_ = v
        }
    }
}

class B <: A {
    public mut override prop a: Int64 {
        set(v) {}
    }
}

class C <: A {
    public mut override prop a: Int64 {
        get() {
            a_ + 1
        }
    }
}

main() {
    let b = B()
    b.a++
    if (b.a != 0) {
        return 1
    }
    let c = C()
    c.a++
    if (c.a != 3) {
        return 2
    }
    return 0
}

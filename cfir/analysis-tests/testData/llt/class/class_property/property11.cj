// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    return 0
}

open class Boo {
    public var a: Int64 = 1
    public static var b: Int64 = 1
    protected mut open prop a1: Int64 {
        get() {
            return (a + 1)
        }
        set(v) {
            a = v
        }
    }
}

class Coo <: Boo {
    private mut override prop <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>a1<!>: Int64 {
        get() {
            return (a + 2)
        }
        set(v) {
            a = v
        }
    }
}

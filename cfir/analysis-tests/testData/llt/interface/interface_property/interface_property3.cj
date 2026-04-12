// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

interface I {
    prop a: Int64
}

struct RR <: I {
    public var aa: Int64 = 3
    public prop a: Int64 {
        get() {
            return aa
        }
    }
    public init(aa!: Int64 = 3) {
        this.aa = aa
    }
}

struct R {
    public var a: I = RR()
    public mut func c(b: RR) {
        a = b
        return a
    }
    public init(a!: I = RR()) {
        this.a = a
    }
}

main() {
    var b = R()
    b.c(RR(aa: 98))
    print("${b.a.a}")
    return b.a.a - 98
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

class A {
    public static var a: String = "C"
    public var b: String = "C"
}

extend A {
    public prop f: String {
        get() {
            return this.b
        }
    }

    public static prop f1: String {
        get() {
            return a
        }
    }
}

main(): Int64 {
    if (A().f == "C" && A.f1 == "C") {
        print("${A().f}\n")
        print("${A.f1}\n")
        return 0
    }
    return 1
}

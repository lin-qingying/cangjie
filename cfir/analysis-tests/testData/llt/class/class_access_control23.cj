// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix
// ASSERT: scan 6

interface I2 {
    static prop p3: Int64 {
        get() {
            26
        }
    }
}

interface I1 {
    static prop p3: Int64 {
        get() {
            16
        }
    }
}

open class S <: X {
    public static prop p3: Int64 {
        get() {
            6
        }
    }
}

open class X {
    public static prop p3: Int64 {
        get() {
            36
        }
    }
}

class A <: S & I1 & I2 {
    func check() {
        println(p3)
    }
}

main() {
    A().check()
    return 0
}

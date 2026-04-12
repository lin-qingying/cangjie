// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

abstract class A {
    protected open func f1() {
        return 1
    }
    protected open func f2(): Int64
    static func g1() {
        return 1
    }
    protected mut open prop a1: Int64 {
        get() {
            1
        }
        set(v) {
            v
        }
    }
    protected mut open prop a2: Int64
    mut static prop b1: Int64 {
        get() {
            1
        }
        set(v) {
            v
        }
    }
}

class B <: A {
    public override func f1() {
        return 0
    }
    public func f2(): Int64 {
        return 0
    }
    public static redef func g1() {
        return 0
    }
    public mut override prop a1: Int64 {
        get() {
            0
        }
        set(v) {
            v
        }
    }
    public mut prop a2: Int64 {
        get() {
            0
        }
        set(v) {
            v
        }
    }
    public mut static redef prop b1: Int64 {
        get() {
            0
        }
        set(v) {
            v
        }
    }
}

main() {
    var a = B()
    return a.f1() + B.g1() + a.f2() + a.a1 + a.a2 + B.b1
}

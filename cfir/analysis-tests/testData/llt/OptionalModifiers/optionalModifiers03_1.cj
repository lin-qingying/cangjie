// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

interface I {
    // with default implementation
    func f1() {}
    static func sf1() {}
    mut prop b1: Int64 {
        get() {
            print("get")
            0
        }
        set(value) {
            print("set")
        }
    }

    // no default implementation
    func f2(): Unit
    static func sf2(): Unit
    mut prop b2: Int64
}

struct R <: I {
    // with default implementation
    public override func f1() {}
    public static redef func sf1() {}
    public mut override prop b1: Int64 {
        get() {
            print("get")
            0
        }
        set(value) {
            print("set")
        }
    }

    public override func f2(): Unit {}
    public static redef func sf2() {}
    public mut override prop b2: Int64 {
        get() {
            print("get")
            0
        }
        set(value) {
            print("set")
        }
    }
}

main() {
    return 0
}

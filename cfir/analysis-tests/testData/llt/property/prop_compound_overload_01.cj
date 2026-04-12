// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

class A {
    var a_ = "0"
    mut prop a: String {
        get() {
            a_
        }
        set(v) {
            a_ = v
        }
    }
}

main() {
    let a = A()
    a.a += "str"
    if (a.a != "0str") {
        return 1
    }
    return 0
}

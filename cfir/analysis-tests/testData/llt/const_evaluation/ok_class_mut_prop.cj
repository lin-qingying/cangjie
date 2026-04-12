// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

class A {
    // mut prop can be defined together with const init
    mut prop x: Int64 {
        get() {
            0
        }
        set(_) {}
    }

    const init() {}
}

main() {
    let a = A()
    a.x = 0
    a.x
}

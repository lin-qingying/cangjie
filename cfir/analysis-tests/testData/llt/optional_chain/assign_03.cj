// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

class A {
    var b0: Int64 = 1
    mut prop b: Int64 {
        get() {
            b0
        }
        set(value) {
            b0 = value
        }
    }
}

main() {
    var a = Some(A())
    a?.b--
    a?.b ?? 1
}

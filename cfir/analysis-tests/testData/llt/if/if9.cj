// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

class A {}

class B {}

main() {
    let a = if (true) {
        A()
    } else {
        B()
    }
    let b: Object = a
    if (a is Object) {
        return 0
    }
    return 1
}

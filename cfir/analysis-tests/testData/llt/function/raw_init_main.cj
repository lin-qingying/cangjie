// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// RUN-EXEC: %run %run_opt %output %run_args

class A {
    func `init`() {
        1
    }
}

class B {
    static func `init`(a: Int64) {
        2
    }
    func `main`() {
        3
    }
}

func `main`() {
    6
}

main() {
    let v = A()
    let x = `main`()
    if (x != 6) {
        return 1
    }
    return `main`() - (v.`init`() + B().`main`() + B.`init`(1))
}

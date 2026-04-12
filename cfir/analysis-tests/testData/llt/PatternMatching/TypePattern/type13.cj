// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE) EXEC: %compiler %cmp_opt %f -o %output
// (CJNATIVE) EXEC: %run %run_opt %output %run_args

open class A {}

class B <: A {}

// non-classlike types should be able to downcast to its real type
main() {
    let f0: (A) -> B = {a: A => B()}
    let f: (B) -> A = f0
    if (f is (A) -> B) {
        return 0
    }
    return 1
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f  -o %output
// EXEC: %run %run_opt %output %run_args
open class A {}

open class B <: A {}

class C <: B {}

func test(): A {
    return C()
}

main() {
    var res = test() is C
    if (res) {
        return 0
    }
    return 1
}

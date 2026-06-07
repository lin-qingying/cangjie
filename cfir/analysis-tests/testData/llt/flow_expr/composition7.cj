// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func a(input: Int64): Int64 {
    input
}

func test(<!chir_dce_unused_variable!>input<!>: Int64) {}

func test(<!chir_dce_unused_variable!>input<!>: (Int64) -> Int64) {}

func foo() {
    test(a ~> a)
}

main(): Int64 {
    foo()
    return 0
}

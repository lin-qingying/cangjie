// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


let one: Int32 = 1
let two: Int32 = 2

func foo() {
    return 1
    let a = one
    let b = two
    <!sema_incompatible_func_body_and_return_type!>r<!>eturn a + b
}

main(): Int64 {
    foo()
    return 0
}

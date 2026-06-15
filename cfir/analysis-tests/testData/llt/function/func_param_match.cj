// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func foo(a: Float32) {
    0
}

main() {
    let a: Float32 = <!CANNOT_CONVERT_LITERAL!>1<!>
    return foo(<!CANNOT_CONVERT_LITERAL!>1<!>)
}

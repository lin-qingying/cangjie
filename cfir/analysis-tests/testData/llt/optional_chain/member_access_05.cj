// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


struct A {
    let b: B = B()
}

struct B {
    let c: Int64 = 0
}

main() {
    let a = Some(A())
    let x = <!OPTIONAL_CHAIN_NON_OPTIONAL!>a?.b<!>?.c
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func foo() {}
main() {
    let a = foo 
    let b = foo 
    let c = (1,2)
    let d = (3,4)
    a <!INVALID_BINARY_OPERATOR!>==<!> b 
    c <!INVALID_BINARY_OPERATOR!>+<!> d 
    0
}

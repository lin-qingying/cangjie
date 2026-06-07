// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A {
let m2 = return
}

struct B {
let m2 :Unit = return
}

interface C {
<!parse_unexpected_declaration_in_scope!>let<!> m2 = return
}

var a = return

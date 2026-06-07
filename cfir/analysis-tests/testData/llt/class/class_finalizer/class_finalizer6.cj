// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class C {}

extend C {
    <!parse_unexpected_declaration_in_scope!>~init<!>(){}
}

class D {
    ~init(){}
}

class B {
 <!parse_expected_decl!>~<!>
 init(){}
}

main() {
    D().<!parse_expected_name!>~<!>init()
    0
}

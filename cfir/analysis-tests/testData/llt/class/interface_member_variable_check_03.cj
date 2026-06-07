// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I {
    <!parse_unexpected_declaration_in_scope!>let<!> a: Int64 = 1
    <!parse_unexpected_declaration_in_scope!>let<!> b: Int64 = 1
    <!parse_unexpected_declaration_in_scope!>let<!> c: Int64 = 1
}

main(): Int64 {
    return 0
}

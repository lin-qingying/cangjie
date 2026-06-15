// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main() {
    let a: VArray<Int64, $5> = [1, 2, 3, 4, 5]

    let s: Int32 = <!TYPE_MISMATCH!>a.size<!> // error
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>a.size = 1<!> // error
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>a.size = s<!> // error
    a.<!UNRESOLVED_REFERENCE!>sss<!> // error
    0
}

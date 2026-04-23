// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func f() {
    var a = <!UNRESOLVED_REFERENCE!>a<!>
    var b: (Unit) -> Int32 = <!TYPE_MISMATCH!>{=> 1}<!>
    var c: (Unit) -> Int32 = <!TYPE_MISMATCH!>1<!>
}

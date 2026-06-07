// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


foreign func foo1(p: CPointer<Int64>): Unit

struct S {
    public var arr: VArray<Int32, $3> = [1, 2, 3]
}

unsafe main() {
    var a = VArray<Int64, $3>(repeat: 0)
    foo1(inout <!INOUT_MUST_BE_VAR_VARIABLE!>a.size<!>)
    return 0
}

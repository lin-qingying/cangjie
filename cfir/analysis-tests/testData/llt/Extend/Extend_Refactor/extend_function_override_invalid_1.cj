// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class C1 {}

extend C1 {
    public func foo(): Int64 {
        0
    }
}

class C2 <: C1 {
    public func <!EXTEND_FUNCTION_CANNOT_OVERRIDDEN!>foo<!>(): Int64 {
        0
    }
}

main() {
    return -1
}

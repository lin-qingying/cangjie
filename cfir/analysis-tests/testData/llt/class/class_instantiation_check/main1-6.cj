// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class C1<T> {
    public func c1(a: Int32) {
        return 0
    }
    public func c1(a: T) {
        return 0
    }
}

open class C2<T> <: C1<T> {}

class C3 <: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C2<Int32><!> {}

main() {
    var a = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C1<Int32><!>()
    var b = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C2<Int32><!>()
    return 0
}

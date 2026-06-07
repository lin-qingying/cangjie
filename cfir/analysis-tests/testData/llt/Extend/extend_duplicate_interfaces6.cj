// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    if (<!SUPER_TYPES_DUPLICATE!>B<String><!>().m != 1) {
        return 1
    }
    return 0
}

interface I<T> {
    func foo(): Int64 {
        return 1
    }
}

class B<T> <: I<T> {}

extend<T> B<T> <: <!EXTEND_DUPLICATE_INTERFACE!>I<T><!> {
    public prop m: Int64 {
        get() {
            return foo()
        }
    }
}

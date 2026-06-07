// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    if (B<String, Int64>().m != 1) {
        return 1
    }
    return 0
}

interface I<T> {
    func foo(): Int64 {
        return 1
    }
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>interface J<T, V><!> <: I<T> & I<V> {}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class B<T, V><!> <: I<T> & I<V> {
    public prop m: Int64 {
        get() {
            return foo()
        }
    }
}

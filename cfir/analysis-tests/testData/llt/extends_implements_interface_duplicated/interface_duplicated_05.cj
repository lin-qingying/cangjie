// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I0<T> {}

interface I1<K> <: I0<K> & I0<Rune> {}

<!SUPER_TYPES_DUPLICATE!>class A<Q><!> <: I1<Q> {
    public func doit(): Int64 {
        return 5
    }
}

main() {
    if (A<Rune>().doit() == 5) {
        return 0
    }
    return 1
}

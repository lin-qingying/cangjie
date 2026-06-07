// FILE: pkg/pkg.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

public interface A<T> {
    func foo(a: T): T {
        return a
    }
}

// FILE: class_no_override_modifier_invalid_9.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


internal import pkg.*

interface B<T> {
    func foo(b: T): T {
        return b
    }
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class D<T><!> <: A<T> & B<T> {}

main() {
    return 1
}

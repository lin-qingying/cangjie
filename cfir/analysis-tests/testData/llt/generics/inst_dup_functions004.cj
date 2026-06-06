<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

class A<X, Y, Z> {
    public func test(a: <!sema_generic_instantiation_causes_ambiguous_functions!>A<X, Y, Z><!>, b: A<X, Y, Z>) {}
    public func test(a: <!sema_generic_instantiation_causes_ambiguous_functions!>A<X, Y, Int8><!>, b: A<X, Y, Z>) {}
    public func test(a: <!sema_generic_instantiation_causes_ambiguous_functions!>A<Int8, Int8, Int8><!>, b: A<X, Y, Z>) {}
}

interface B<X, Y, Z> {
    func test(a: <!sema_generic_instantiation_causes_ambiguous_functions!>B<X, Y, Z><!>, b: B<X, Y, Z>) {}
    func test(a: <!sema_generic_instantiation_causes_ambiguous_functions!>B<X, Y, Int8><!>, b: B<X, Y, Z>) {}
    func test(a: <!sema_generic_instantiation_causes_ambiguous_functions!>B<Int8, Int8, Int8><!>, b: B<X, Y, Z>) {}
}

struct C<X, Y, Z> {
    public func test(a: <!sema_generic_instantiation_causes_ambiguous_functions!>C<X, Y, Z><!>, b: C<X, Y, Z>) {}
    public func test(a: <!sema_generic_instantiation_causes_ambiguous_functions!>C<X, Y, Int8><!>, b: C<X, Y, Z>) {}
    public func test(a: <!sema_generic_instantiation_causes_ambiguous_functions!>C<Int8, Int8, Int8><!>, b: C<X, Y, Z>) {}
}

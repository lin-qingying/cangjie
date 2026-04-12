// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class C<M> {}

interface Z1 {
    func f1() {
        1
    }
    static func f3() {
        1
    }
    static func f4<T>(v: T) {
        v
    }
}

class C1<M> where M <: Z1 {}

extend<M> C1<M> where M <: Z1 {
    public func test(x2: (M) -> M, x3: C<M>): (M) -> M {
        static_test(x2, x3)
    }
    public static func static_test(x2: (M) -> M, x3: C<M>): (M) -> M {
        var v2 = x2
        var v3 = x3
        var v5 = v2 as Option<(M) -> M>
        var v6 = match (v3) {
            case _: Option<C<M>> => false
            case _: C<M> => true
        }

        var v7 = M.f4<Int64>(0)
        if (!v6 || v7 != 0) {
            throw Exception()
        }
        v2
    }
}

type T2<M> = C1<M>

main() {
    var a = T2<Nothing>()
    var v1 = a.test({x: Nothing => x}, C<Nothing>())
    if (v1 is (Nothing) -> Nothing) {
        return 0
    }
    return 1
}

/* SCAN
error: can not instantiate 'Generics-M' by 'Nothing' for it has unimplemented static member
  ==> interface_static_impl_with_generic13.cj:46:17:
   | 
46 | type T2<M> = C1<M>
   |                 ^ 
   | 
   # note: 'Nothing' type has no members

1 error generated, 1 error printed.
*/

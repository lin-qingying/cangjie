// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


// test err msg for arg/param and ret/expected type mismatch
func f<T>(a: A<T>) {}

func g<T>(a: A<T>): A<T> {
    a
}

class A<T> {}

class B<T> {}

main() {
    <!UNABLE_TO_INFER_GENERIC_FUNC!>f(<!TYPE_MISMATCH!>B<Int>()<!>)<!>
    let s: B<Int> = <!TYPE_MISMATCH!>g(A<Int>())<!>
}

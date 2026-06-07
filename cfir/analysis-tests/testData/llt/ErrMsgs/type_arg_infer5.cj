// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {}
// test tracking nested constraints
func f<T>() where T <: A {
    func g<R>(): R where R <: <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!> & A {
        let v: Int64 = <!UNABLE_TO_INFER_GENERIC_FUNC!>g()<!>
        return g<R>()
    }
}

main() {
}

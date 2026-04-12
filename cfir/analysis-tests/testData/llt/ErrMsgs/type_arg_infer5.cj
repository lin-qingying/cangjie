// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %diag_json %f -o %output
class A {}
// test tracking nested constraints
func f<T>() where T <: A {
    func g<R>(): R where R <: T & A {
        let v: Int64 = g()
        return g<R>()
    }
}

main() {
}

// ASSERT: scan-1 sema_unable_to_infer_generic_func
// ASSERT: scan-1 sema_upper_bound_must_be_class_or_interface
// ASSERT: scan "Errors": 2

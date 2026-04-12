// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt --diagnostic-format json %f -o %output

// ASSERT: scan sema_unable_to_infer_generic_func
// ASSERT: scan Generics-T <: Interface-Comparable<Generics-T>

// ensure no temporary type var names like 'T-0' exist in constraints
func f<T>(x: T) where T <: Comparable<T> {
}

class C {}

main() {
    f(C())
}

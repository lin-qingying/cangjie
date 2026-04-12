// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt --diagnostic-format json %f -o %output

// ASSERT: scan sema_unable_to_infer_generic_func
// ASSERT: scan following constraints for type variable 'Generics-T' cannot be solved
// ASSERT: scan Struct-Array<Int64> <: Generics-T
// ASSERT: scan Generics-T <: Struct-Array<Struct-String>

// test constraints from destruction
func f<T>(a: Array<T>): Array<T> {
    a
}

main() {
    let v: Array<Array<String>> = f([[1]])
}

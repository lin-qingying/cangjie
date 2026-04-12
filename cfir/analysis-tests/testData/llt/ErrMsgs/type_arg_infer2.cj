// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt --diagnostic-format json %f -o %output

// ASSERT: scan sema_unable_to_infer_generic_func
// ASSERT: scan following constraints for type variable 'Generics-T' cannot be solved
// ASSERT: scan Int64 <: Generics-T
// ASSERT: scan of type 'Int64', should match parameter type 'Generics-T'
// ASSERT: scan Struct-String <: Generics-T
// ASSERT: scan of type 'Struct-String', should match parameter type 'Generics-T'

// test err msg for conflicting constraints
func f<T>(x: T, y: T) {
}

main() {
    f(1, "Hello")
}

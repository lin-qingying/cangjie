// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// ASSERT: scan arguments type of CFunc must be instantiated with CType

struct A {}

foreign func c_f1(a: VArray<A, $3>): Unit // error

@C
func c_f2(a: VArray<A, $3>): Unit {} // error

var c_f3: CFunc<(VArray<A, $3>) -> Unit> = {a => ()} //error

main() {}

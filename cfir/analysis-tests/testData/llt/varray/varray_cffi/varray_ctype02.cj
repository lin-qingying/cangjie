// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// ASSERT: scan return type of CFunc cannot be 'VArray' type

foreign func c_f1(a: VArray<Int32, $3>): VArray<Int32, $3> // error

@C
func c_f2(a: VArray<Int32, $3>): VArray<Int32, $3> {
    a
} // error

var c_f3: CFunc<(VArray<Int32, $3>) -> VArray<Int32, $3>> = {a => a} //error

foreign func c_f4(a: CPointer<VArray<Int32, $1>>): CPointer<VArray<Int32, $1>> // error

@C
func c_f5(a: CPointer<VArray<Int32, $1>>): CPointer<VArray<Int32, $1>> {
    a
} // error

var c_f6: CFunc<(CPointer<VArray<Int32, $1>>) -> CPointer<VArray<Int32, $1>>> = {a => a} //error

main() {}

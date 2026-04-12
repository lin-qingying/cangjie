// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-0: %compiler %noColor %cmp_opt_chir2hlir %f -o %n.%suffix
// EXEC-0: %run %n.%suffix

var node1: CPointer<Unit> = CPointer<Unit>()
var node2 = CPointer<Unit>()
var node3: CPointer<Unit> = CPointer()

type C<T> = CPointer<T>

var node4 = C<Int64>()
var node4_1: C<Int64> = C()

type D = C<Int64>

var node5 = D()
var node5_1: D = D()
var node5_3: D = C()
var node7: CType = CPointer<Unit>()

interface I<T> where T <: CType {}

extend<T> CPointer<T> <: I<T> {}

var node10: I<Unit> = CPointer<Unit>()
var node11: I<Unit> = CPointer()
var node12 = CPointer<Unit>(CPointer<Unit>())

foreign func malloc(size: UIntNative): CPointer<Unit>

var node18 = CPointer<Unit>(malloc)

interface I2 {}

extend Int32 <: I2 {}

extend<T> CPointer<T> <: I2 where T <: I2 {}

var node19: I2 = CPointer<Int32>()
let buf = CPointer<Unit>()
var node21 = CString(CPointer<UInt8>(buf))

main() {}

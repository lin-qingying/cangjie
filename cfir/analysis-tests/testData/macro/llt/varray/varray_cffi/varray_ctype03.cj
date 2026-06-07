// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


struct A {}

foreign func c_f1(a: <!INVALID_CFUNC_PARAMETER_TYPE!>VArray<A, $3><!>): Unit // error

@C
func c_f2(a: <!INVALID_CFUNC_PARAMETER_TYPE!>VArray<A, $3><!>): Unit {} // error

var c_f3: CFunc<((<!INVALID_CFUNC_PARAMETER_TYPE!>VArray<A, $3><!>) -> Unit)> = {a => ()} //error

main() {}

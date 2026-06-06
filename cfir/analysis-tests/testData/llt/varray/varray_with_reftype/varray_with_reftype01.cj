// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class A {}

func foo(a: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>A<!>, $1>): VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>A<!>, $1> { // error x2
    return [A()]
}

main() {
    var a: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>A<!>, $1> = [A()] // error
    var b = VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>A<!>, $1>(repeat: A()) // error
    var c = VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>A<!>, $1> {i => A()} // error
    0
}

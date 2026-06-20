// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


struct A {}

@C
struct S {
    var a = VArray<VArray<VArray<VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>A<!>, $1>, $1>, $1>, $1>(repeat: [[[A()]]]) // error
    var b = VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>A<!>, $1>(repeat: A()) // error
}

main() {}

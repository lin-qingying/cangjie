// Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class GennericClassA<T> {}

type TypeClass<Y> = GennericClassA<Y>

type TypeB<X> = TypeClass<X>

type TypeC = TypeClass<Int64>

main() {
    var a = <!UNABLE_TO_INFER_GENERIC_FUNC!>TypeB()<!>
    var b = <!UNABLE_TO_INFER_GENERIC_FUNC!>TypeClass()<!>
    var c = <!UNABLE_TO_INFER_GENERIC_FUNC!>GennericClassA()<!>
    var d = TypeC()
    0
}

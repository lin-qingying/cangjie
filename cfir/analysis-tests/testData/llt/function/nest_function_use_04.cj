// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A {
    static func <!UNABLE_TO_INFER_RETURN_TYPE!>foo<!><T>(a: T) {
        bar<Int64>(1)
        a
    }
}

func bar<T>(a: T) {
    let b = {a: Any => ()}(<!NO_MATCH_FUNCTION_DECLARATION_FOR_REF!>A<!>.foo<Int64>) // Use type func unknown as reference
    a
}

func baz<T>(a: T): T {
    a
}

main(): Unit {
    bar(1)
}

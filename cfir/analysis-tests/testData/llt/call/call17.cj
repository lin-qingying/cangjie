// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func test<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>X<!>, Y>() where X <: Y {}

class C<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>X<!>, Y> where X <: Y {}

struct R<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>X<!>, Y> where X <: Y {}

main() {
    var i = test<Int8, Int8>()
    var c = C<Int8, Int8>()
    var r = R<Int8, Int8>()
    return 0
}

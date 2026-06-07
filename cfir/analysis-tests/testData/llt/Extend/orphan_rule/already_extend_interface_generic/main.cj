// FILE: pkg/pkg.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

public interface I<T> {}

public class C {}

extend C <: I<Int64> {}

// FILE: main.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


internal import pkg.*

interface II <: I<Int8> {}

extend <!TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE!>C<!> <: II {}

interface III <: I<Int64> {}

extend C <: III {}

main() {
    0
}

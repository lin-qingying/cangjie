// FILE: pkg/pkg.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

public class A {}

public class A1 {}

public interface B {}

// FILE: main.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


internal import pkg.*

type C = A

type D = B

extend <!TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE!>C<!> <: D {}

interface E <: D {}

extend <!TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE!>A1<!> <: E {}

main() {
    return 0
}

// FILE: pkg.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

internal import pkg1.*

public interface I<T> {}

extend<T2> A<T2> <: I<T2> {}

extend<T2> B<T2> <: I<Int64> {}

// FILE: pkg1.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg1

public class A<T1> {}

public class B<T1> {}

// FILE: main.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


internal import pkg1.*

internal import pkg.*

interface I2<T> <: I<T> {}

extend<T3> A<T3> <: I2<T3> {}

extend<T3> <!TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE!>B<T3><!> <: I2<T3> {}

main() {}

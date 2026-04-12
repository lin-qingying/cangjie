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

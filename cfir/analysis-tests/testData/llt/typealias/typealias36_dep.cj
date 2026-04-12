// Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkga

public class A<X, Y> {}

public type BB<BX, BY> = A<BX, BY>

public type AA<XX> = BB<XX, Array<XX>>
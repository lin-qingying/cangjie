// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkgs

public enum TimeUnit<T> {
    century
    | Year(T)
    | Month(Int32, Int32)
    | Day(Int32, Int32, T)
}

public var g_time: TimeUnit<Int32> = TimeUnit<Int32>.Year(10000)

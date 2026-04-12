// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f

interface MyFactorCandidatesPlugin {}

extend<T> ?T <: MyFactorCandidatesPlugin {}

class MydataFactor<T> where T <: MyFactorCandidatesPlugin {}

let deviceTypeFactor = MydataFactor<Option<String>>()

main() {}

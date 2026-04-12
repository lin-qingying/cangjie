// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: testa.cj testb.cj
// (CJNATIVE) EXEC: %compiler %cmp_opt %compile_lib_opt testa.cj -o testa.%middle %enableO2
// (CJNATIVE) EXEC: %compiler %cmp_opt %compile_lib_opt testb.cj -o testb.%middle %enableO2
// (CJNATIVE) EXEC: %compiler %cmp_opt testb.%middle testa.%middle %f -o %output %enableO2
// (CJNATIVE) EXEC: %run %run_opt %output

internal import adapter.lsk.pb.ServerEventType
internal import std.collection.concurrent.*

main() {
    let callbackMap: ConcurrentHashMap<ServerEventType, Int64> = ConcurrentHashMap<ServerEventType, Int64>()
    callbackMap.get(ServerEventType.EVENTTYPE_UNDEFINED)
    0
}

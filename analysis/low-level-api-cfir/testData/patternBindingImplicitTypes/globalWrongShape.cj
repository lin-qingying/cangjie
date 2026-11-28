// 官方 TypeCheckPattern.cpp:480 在当前层形状或元数错误时直接失败。
// 本项目要求该层全部未完成 binder 收敛为 ConeErrorType；不按较短元数组配对类型。
package test

let (nonTupleNumber, nonTupleFlag) = 1

let (shortNumber, shortFlag, shortText) = (1, true)

let (longNumber, longFlag) = (1, true, "ready")

// initializer 本身无法得到有效类型时，每个 active leaf 仍必须完成错误收敛。
let (initializerNumber, initializerFlag) = unresolvedInitializerForPattern

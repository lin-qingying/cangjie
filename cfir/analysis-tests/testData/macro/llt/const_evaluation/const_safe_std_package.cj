// 官方 ParseAnnotations.cpp:153–157 仅在 std 模块识别 ConstSafe；
// Parser.cpp:305–307 从 package 的首段取得该身份，与文件名无关。
package std.cfir_annotation_contract

@ConstSafe
const cachedBytes: Array<UInt8> = []

main() {}

// 官方按位置递归且遇到首个失败即返回；前两个兄弟已经成功获得 Int64、String。
// 本项目补足失败嵌套层的错误收敛，保留上述成功兄弟的类型。
package test

let (shapeNumber, shapeText, (shapeFlag, shapeTail)) = (1, "ready", true)

// 嵌套当前层的元数不等时，该层两个 binder 全部为 error。
let (arityNumber, arityText, (arityFlag, arityTail)) = (1, "ready", (true, "tail", 2))

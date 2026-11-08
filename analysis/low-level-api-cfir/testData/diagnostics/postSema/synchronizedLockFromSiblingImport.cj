// MODULE: main
// MAIN_FILE_NAME: synchronizedLockFromSiblingImport.cj
// FILE: lockHolder.cj
package test

// 同包兄弟文件导入 `std.sync`：官方 `GetImportedDecl(std.sync, "Lock")` 依赖的是
// 包级 import 并集（cjoManager 已加载的包），不是当前文件自己的 import 列表。
import std.sync.*

public let sharedLock = ReentrantMutex()

// FILE: synchronizedLockFromSiblingImport.cj
package test

func useLock(): Unit {
    synchronized(sharedLock) {
        let _ = 1
    }
}

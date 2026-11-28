// FILE: lockHolder.cj
package syncpkg

// 同包兄弟文件导入 `std.sync`：官方 `ImportManager::GetSyncDecl` 查的是 cjoManager 里
// 已加载的包（由**包级** import 并集决定），因此本包其它文件里的 `synchronized` 不再要求
// 自己再写 `import std.sync`。真实项目 mariadb `src/cdbc/connection.cj` 与 `transaction.cj`
// 正是这个形态。
import std.sync.*

public let sharedLock = <!DEPRECATED_WARNING!>ReentrantMutex<!>()

// FILE: syncUse.cj
package syncpkg

func useLock(): Unit {
    synchronized(sharedLock) {
        let _ = 1
    }
}

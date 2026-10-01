// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：synchronized 缺少锁实参（官方要求 `synchronized (lock) { }`）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_left_paren "expected '(', found '{'"。
func synchronizedWithoutLock(): Unit {
    synchronized {
    }
}

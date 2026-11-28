package source_exceptions_resources

// 使用真实 std.core.Resource 契约；不访问文件或其他外部资源。
public class SourceResource <: Resource {
    private var closed = false

    public func isClosed(): Bool {
        closed
    }

    public func close(): Unit {
        closed = true
    }

    public func read(): Int64 {
        7
    }
}

public interface ExceptionsResources {
    func recover(fail: Bool): Int64 {
        var result: Int64 = 0
        try {
            if (fail) {
                throw Exception("source body")
            }
            result = 1
        } catch (error: Exception) {
            result = 2
        } finally {
            result += 10
        }
        return result
    }

    func withResources(fail: Bool): Int64 {
        var result: Int64 = 0
        try (first = SourceResource(), second = SourceResource()) {
            result = first.read() + second.read()
            if (fail) {
                throw Exception("resource body")
            }
        } catch (error: Exception) {
            result = 3
        } finally {
            result += 1
        }
        return result
    }

    // 无 finally 时仍须保留 TryExpr.operands 最后的 0 占位。
    func withoutFinally(): Int64 {
        try {
            4
        } catch (error: Exception) {
            5
        }
    }
}

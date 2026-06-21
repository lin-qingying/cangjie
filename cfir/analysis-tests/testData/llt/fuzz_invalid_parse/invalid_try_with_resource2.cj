
 
public open class ResourceA <: Resource {
    public func isClosed(): Bool {
        return true
    }
    public func close(): Unit {}
}
 
public struct StructA {}
 
extend StructA {
    static func funcInStructA(): Bool {
        () |> {
            lambdaParameterUnit: Unit => try (resource = ResourceA()) {
            } catch (e: Exception) {
                <!UNRESOLVED_REFERENCE!>xxx<!>
            } finally {
            } |> {a: Unit => return true}
        }
    }
}
 
public struct StructB<T> {}
 
extend<T> StructB<T> {
    static func funcInStructA(): Bool {
        () |> {
            lambdaParameterUnit: Unit => try (resource = ResourceA()) {
            } catch (e: Exception) {
                <!UNRESOLVED_REFERENCE!>yyy<!>
            } finally {
            } |> {a: Unit => return true}
        }
    }
}
 
main(): Int64 {
    return 0
}
 

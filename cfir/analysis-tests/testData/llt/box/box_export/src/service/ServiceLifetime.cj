

package asp.service

/*
服务声明周期
 */
public enum ServiceLifetime {
    Singleton | Scoped | Transient

    public func isScoped() {
        match (this) {
            case Scoped => true
            case _ => false
        }
    }

    public func isTransient() {
        match (this) {
            case Transient => true
            case _ => false
        }
    }

    public func isSingleton() {
        match (this) {
            case Singleton => true
            case _ => false
        }
    }
}

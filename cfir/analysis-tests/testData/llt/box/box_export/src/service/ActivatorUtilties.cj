

package asp.service

import std.reflect.{ClassTypeInfo, TypeInfo}
import std.collection.{ArrayList, first, filter, collectArray, collectArrayList}

/*
用于基于容器提供者构建未注册单依赖容器服务的实列
 */
public class ActivatorUtilties {
    public static func createInstance<T>(sp: ServiceProvider, args: Array<Any>) {
        let constructor = ReflectUtilities.getServiceConstructor(TypeInfo.of<T>())
        let parameters = ArrayList<Any>()
        for (pattern in constructor.parameters) {
            let parameter = sp.getService(pattern.typeInfo)
            if (parameter.isNone()) {
                let arg = args |> filter {a => pattern.typeInfo.isSubtypeOf(TypeInfo.of(a))} |> first
                parameters.add(arg.getOrThrow())
            } else {
                parameters.add(parameter.getOrThrow())
            }
        }
        let obj = constructor.apply(parameters |> collectArray) as T
        return obj.getOrThrow()
    }
}

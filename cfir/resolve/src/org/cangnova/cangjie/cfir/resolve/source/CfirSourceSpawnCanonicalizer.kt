package org.cangnova.cangjie.cfir.resolve.source

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirSpawnExpression
import org.cangnova.cangjie.cfir.source.*
import org.cangnova.cangjie.cfir.types.ConeFunctionType

/** 固定 SpawnExpr.cpp：task 只移入 Future initializer 一次，SOURCE Spawn 只保留 future 和 scheduler。 */
internal class CfirSourceSpawnCanonicalizer(private val context: CfirSourceGraphProducer.Context) {
    fun produce(spawn: CfirSpawnExpression, owner: CfirDeclaration): CfirSourceExpressionPayload.Spawn {
        val task = spawn.body.statements.single() as? CfirAnonymousFunctionExpression
            ?: error("SOURCE spawn requires its completed function task")
        check(context.typeOf(task) is ConeFunctionType)
        val taskIdentity = context.expression(task, owner)
        val futureType = context.typeOf(spawn)
        val future = context.freshVariable("futureObj", futureType, false, owner, task)
        val initializer = context.generated(owner, futureType, context.constructorCall(futureType, listOf(taskIdentity), owner), CfirSourceSpan.capture(task.source))
        context.builder.initializer(future, initializer)
        val scheduler = spawn.threadContextArgument?.let {
            context.memberCall(context.expression(it, owner), context.typeOf(it), "getSchedulerHandle", emptyList(), owner)
        }
        return CfirSourceExpressionPayload.Spawn(scheduler, future)
    }
}

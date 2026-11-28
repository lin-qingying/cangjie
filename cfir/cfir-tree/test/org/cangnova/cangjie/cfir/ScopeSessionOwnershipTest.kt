package org.cangnova.cangjie.cfir

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** 从调用方观察代次、失败、重入退役及两个所有权域的隔离，不依赖具体 map 布局。 */
class ScopeSessionOwnershipTest {
    private class Product
    private val key = object : ScopeSessionKey<ScopeSessionOwnedId<String>, Product>() {}
    private val ordinaryKey = object : ScopeSessionKey<String, Product>() {}

    @Test
    fun retirementReleasesOnlyRegisteredDomainAndPreservesOtherQueries() {
        val session = ScopeSession()
        val owner = session.createCacheOwner()
        val peer = session.createCacheOwner()
        val first = session.getOrBuild(owner.id("shared"), key) { Product() }
        val peerProduct = session.getOrBuild(peer.id("shared"), key) { Product() }
        val ordinary = session.getOrBuild("ordinary", ordinaryKey) { Product() }
        assertSame(first, session.getOrBuild(owner.id("shared"), key) { Product() })

        assertEquals(1, session.retire(owner))
        val next = session.getOrBuild(owner.id("shared"), key) { Product() }
        assertNotSame(first, next)
        assertSame(peerProduct, session.getOrBuild(peer.id("shared"), key) { Product() })
        assertSame(ordinary, session.getOrBuild("ordinary", ordinaryKey) { Product() })
        assertEquals(1, session.retire(owner))
        assertEquals(0, session.retire(owner))
    }

    @Test
    fun failedFactoriesPublishNoOwnedEntryAndCanRetry() {
        val session = ScopeSession()
        val owner = session.createCacheOwner()
        assertThrows(IllegalStateException::class.java) {
            session.getOrBuild(owner.id("failed"), key) { error("failed computation") }
        }
        assertEquals(0, session.retire(owner))
        val completed = session.getOrBuild(owner.id("failed"), key) { Product() }
        assertSame(completed, session.getOrBuild(owner.id("failed"), key) { Product() })
        assertEquals(1, session.retire(owner))
    }

    @Test
    fun retirementDuringFactoryDoesNotResurrectAnOldGeneration() {
        val session = ScopeSession()
        val owner = session.createCacheOwner()
        val oldId = owner.id("reentrant")
        val transient = session.getOrBuild(oldId, key) {
            session.retire(owner)
            Product()
        }
        val completed = session.getOrBuild(owner.id("reentrant"), key) { Product() }
        assertNotSame(transient, completed)
        assertSame(completed, session.getOrBuild(owner.id("reentrant"), key) { Product() })
        assertThrows(IllegalStateException::class.java) { session.getOrBuild(oldId, key) { Product() } }
        assertEquals(1, session.retire(owner))
    }

    @Test
    fun cacheOwnersCannotCrossPhysicalScopeSessions() {
        val ownerSession = ScopeSession()
        val otherSession = ScopeSession()
        val owner = ownerSession.createCacheOwner()
        assertThrows(IllegalArgumentException::class.java) {
            otherSession.getOrBuild(owner.id("foreign"), key) { Product() }
        }
        assertThrows(IllegalArgumentException::class.java) { otherSession.retire(owner) }
        assertEquals(0L, owner.generation)
    }

    @Test
    fun genericOwnedFactorySharesTheInlineStorageAndRetiresEveryKey() {
        val session = ScopeSession()
        val owner = session.createCacheOwner()
        val otherKey = object : ScopeSessionKey<ScopeSessionOwnedId<String>, Product>() {}
        val id = owner.id("two-products")
        val first = buildGenericOwned(session, id, key) { Product() }
        val second = session.getOrBuild(id, otherKey) { Product() }

        assertSame(first, session.getOrBuild(id, key) { Product() })
        assertSame(second, buildGenericOwned(session, id, otherKey) { Product() })
        assertEquals(2, session.retire(owner))
        assertNotSame(first, buildGenericOwned(session, owner.id(id.value), key) { Product() })
        assertNotSame(second, session.getOrBuild(owner.id(id.value), otherKey) { Product() })
        assertEquals(2, session.retire(owner))
    }

    @Test
    fun inlineFactoryKeepsNonLocalReturnWithoutPublishingTheAbandonedProduct() {
        val session = ScopeSession()
        val owner = session.createCacheOwner()
        val result = Product()

        fun leaveFactory(): Product {
            return session.getOrBuild(owner.id("non-local"), key) { return result }
        }

        assertSame(result, leaveFactory())
        assertEquals(0, session.retire(owner))
        val next = session.getOrBuild(owner.id("non-local"), key) { Product() }
        assertNotSame(result, next)
        assertSame(next, session.getOrBuild(owner.id("non-local"), key) { Product() })
    }

    @Test
    fun nestedNewGenerationPublicationSurvivesTheOldGenericFactory() {
        val session = ScopeSession()
        val owner = session.createCacheOwner()
        val oldId = owner.id("nested")
        val newProduct = Product()
        val abandoned = buildGenericOwned(session, oldId, key) {
            session.retire(owner)
            assertSame(newProduct, session.getOrBuildOwned(owner.id("nested"), key) { newProduct })
            Product()
        }

        assertNotSame(abandoned, newProduct)
        assertSame(newProduct, session.getOrBuild(oldId.owner.id(oldId.value), key) { Product() })
        assertThrows(IllegalStateException::class.java) { session.getOrBuildOwned(oldId, key) { Product() } }
        assertEquals(1, session.retire(owner))
    }

    @Test
    fun nestedNewGenerationPublicationSurvivesTheOldInlineFactory() {
        val session = ScopeSession()
        val owner = session.createCacheOwner()
        val oldId = owner.id("nested")
        val newProduct = Product()
        val abandoned = session.getOrBuild(oldId, key) {
            session.retire(owner)
            assertSame(newProduct, session.getOrBuildOwned(owner.id("nested"), key) { newProduct })
            Product()
        }

        assertNotSame(abandoned, newProduct)
        assertSame(newProduct, session.getOrBuildOwned(owner.id("nested"), key) { Product() })
        assertEquals(1, session.retire(owner))
    }

    @Test
    fun genericOwnedFailureCannotPublishOrCrossThePhysicalStorage() {
        val session = ScopeSession()
        val owner = session.createCacheOwner()
        assertThrows(IllegalStateException::class.java) {
            buildGenericOwned(session, owner.id("failed"), key) { error("failed computation") }
        }
        assertEquals(0, session.retire(owner))
        assertThrows(IllegalArgumentException::class.java) {
            buildGenericOwned(ScopeSession(), owner.id("foreign"), key) { Product() }
        }
        assertEquals(1L, owner.generation)
    }

    /** 此入口保留真正的泛型 PRODUCT，编译和调用均不依赖 reified 类型参数。 */
    private fun <PRODUCT : Any> buildGenericOwned(
        session: ScopeSession,
        id: ScopeSessionOwnedId<String>,
        key: ScopeSessionKey<ScopeSessionOwnedId<String>, PRODUCT>,
        build: () -> PRODUCT,
    ): PRODUCT = session.getOrBuildOwned(id, key, build)
}

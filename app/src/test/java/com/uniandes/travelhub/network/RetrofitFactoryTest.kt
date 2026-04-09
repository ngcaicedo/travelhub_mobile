package com.uniandes.travelhub.network

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

class RetrofitFactoryTest {

    @Test
    fun `securityApi is non-null and idempotent`() {
        val first = RetrofitFactory.securityApi
        val second = RetrofitFactory.securityApi

        assertNotNull(first)
        assertSame("securityApi must be cached as a singleton", first, second)
    }

    @Test
    fun `usersApi is non-null and idempotent`() {
        val first = RetrofitFactory.usersApi
        val second = RetrofitFactory.usersApi

        assertNotNull(first)
        assertSame("usersApi must be cached as a singleton", first, second)
    }

    @Test
    fun `securityApi and usersApi are distinct retrofit proxies`() {
        // They wrap different baseURLs, so the generated proxies must not be the same instance.
        val security: Any = RetrofitFactory.securityApi
        val users: Any = RetrofitFactory.usersApi

        assert(security !== users)
    }
}

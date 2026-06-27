package com.zeal.btctrack.data.remote

import com.zeal.btctrack.domain.model.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TorOnlyEndpointPolicyTest {
    @Test
    fun `accepts onion endpoint`() {
        val source = TorOnlyEndpointPolicy.requireOnion("http://mempoolhqx4isw62.onion/api")
        assertEquals("mempoolhqx4isw62.onion", source.host)
        assertTrue(source.isOnion)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects clearnet endpoint`() {
        TorOnlyEndpointPolicy.requireOnion("https://mempool.space/api")
    }

    @Test
    fun `builds socks proxy from app settings`() {
        val proxy = TorSocksProxyFactory.from(
            AppSettings(socksHost = "127.0.0.1", socksPort = 9050)
        )
        requireNotNull(proxy) { "Expected non-null proxy for non-blank socksHost" }
        val address = proxy.address() as java.net.InetSocketAddress
        assertEquals("127.0.0.1", address.hostString)
        assertEquals(9050, address.port)
        assertEquals(java.net.Proxy.Type.SOCKS, proxy.type())
    }

    @Test
    fun `returns null proxy when socksHost is blank`() {
        val proxy = TorSocksProxyFactory.from(AppSettings(socksHost = ""))
        assertEquals(null, proxy)
    }
}

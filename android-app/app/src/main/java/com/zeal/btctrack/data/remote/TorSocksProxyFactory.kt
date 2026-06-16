package com.zeal.btctrack.data.remote

import com.zeal.btctrack.domain.model.AppSettings
import java.net.InetSocketAddress
import java.net.Proxy

object TorSocksProxyFactory {
    fun from(settings: AppSettings): Proxy = Proxy(
        Proxy.Type.SOCKS,
        InetSocketAddress(settings.socksHost, settings.socksPort),
    )
}

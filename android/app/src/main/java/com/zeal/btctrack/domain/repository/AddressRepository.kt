package com.zeal.btctrack.domain.repository

import com.zeal.btctrack.domain.model.AddressEntry
import kotlinx.coroutines.flow.Flow

interface AddressRepository {
    fun observeAll(): Flow<List<AddressEntry>>
    suspend fun replaceAll(entries: List<AddressEntry>)
    suspend fun findByAddress(address: String): AddressEntry?
    suspend fun add(entry: AddressEntry)
    suspend fun update(entry: AddressEntry)
    suspend fun delete(address: String)
}

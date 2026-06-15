package com.zeal.btctrack.data.repository

import com.zeal.btctrack.data.local.dao.AddressDao
import com.zeal.btctrack.data.mapper.toDomain
import com.zeal.btctrack.data.mapper.toEntity
import com.zeal.btctrack.domain.model.AddressEntry
import com.zeal.btctrack.domain.repository.AddressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AddressRepositoryImpl(
    private val addressDao: AddressDao,
) : AddressRepository {
    override fun observeAll(): Flow<List<AddressEntry>> =
        addressDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun replaceAll(entries: List<AddressEntry>) {
        addressDao.clearAll()
        addressDao.upsertAll(entries.map { it.toEntity() })
    }

    override suspend fun findByAddress(address: String): AddressEntry? =
        addressDao.findByAddress(address)?.toDomain()

    override suspend fun add(entry: AddressEntry) {
        addressDao.upsert(entry.toEntity())
    }

    override suspend fun update(entry: AddressEntry) {
        addressDao.upsert(entry.toEntity())
    }

    override suspend fun delete(address: String) {
        addressDao.deleteByAddress(address)
    }
}

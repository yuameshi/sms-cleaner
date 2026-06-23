package top.yuameshi.sms.cleaner.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import top.yuameshi.sms.cleaner.data.repository.FilterHistoryRepository
import top.yuameshi.sms.cleaner.data.repository.FilterHistoryRepositoryImpl
import top.yuameshi.sms.cleaner.data.repository.SmsRepository
import top.yuameshi.sms.cleaner.data.repository.SmsRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindSmsRepository(impl: SmsRepositoryImpl): SmsRepository

    @Binds
    abstract fun bindFilterHistoryRepository(impl: FilterHistoryRepositoryImpl): FilterHistoryRepository
}

package top.yuameshi.sms.cleaner.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import top.yuameshi.sms.cleaner.data.datasource.SmsDataSource
import top.yuameshi.sms.cleaner.data.repository.SmsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideSmsRepository(smsDataSource: SmsDataSource): SmsRepository {
        return SmsRepository(smsDataSource)
    }
}

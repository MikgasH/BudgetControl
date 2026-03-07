package com.example.budgetcontrol.core.di

import com.example.budgetcontrol.BuildConfig
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.data.remote.cerps.CerpsAnalyticsApiService
import com.example.budgetcontrol.core.data.remote.cerps.CerpsApiService
import com.example.budgetcontrol.core.data.remote.cerps.CerpsRepository
import com.example.budgetcontrol.core.data.remote.gemini.GeminiApiService
import com.example.budgetcontrol.core.data.repository.NetworkStatusRepository
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("cerps")
    fun provideCerpsOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(@Named("cerps") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.CERPS_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideCerpsApiService(retrofit: Retrofit): CerpsApiService {
        return retrofit.create(CerpsApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("cerps_analytics")
    fun provideCerpsAnalyticsRetrofit(@Named("cerps") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.CERPS_ANALYTICS_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideCerpsAnalyticsApiService(@Named("cerps_analytics") retrofit: Retrofit): CerpsAnalyticsApiService {
        return retrofit.create(CerpsAnalyticsApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideCerpsRepository(
        @ApplicationContext context: Context,
        apiService: CerpsApiService,
        analyticsApiService: CerpsAnalyticsApiService,
        preferencesManager: PreferencesManager
    ): CerpsRepository {
        return CerpsRepository(context, apiService, analyticsApiService, preferencesManager)
    }

    @Provides
    @Singleton
    fun provideNetworkStatusRepository(
        @ApplicationContext context: Context,
        cerpsApiService: CerpsApiService
    ): NetworkStatusRepository {
        return NetworkStatusRepository(context, cerpsApiService)
    }

    @Provides
    @Singleton
    @Named("gemini")
    fun provideGeminiRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideGeminiApiService(@Named("gemini") retrofit: Retrofit): GeminiApiService {
        return retrofit.create(GeminiApiService::class.java)
    }
}
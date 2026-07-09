package com.vdub.ml.di

import android.content.Context
import com.vdub.ml.EmbeddingModel
import com.vdub.ml.ModelDownloader
import com.vdub.ml.ONNXRuntimeManager
import com.vdub.ml.SegmentationModel
import com.vdub.ml.SpeakerClustering
import com.vdub.ml.VADModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MLModule {

    @Provides
    @Singleton
    fun provideONNXRuntimeManager(@ApplicationContext context: Context): ONNXRuntimeManager =
        ONNXRuntimeManager(context)

    @Provides
    @Singleton
    fun provideSegmentationModel(onnxManager: ONNXRuntimeManager): SegmentationModel =
        SegmentationModel(onnxManager)

    @Provides
    @Singleton
    fun provideEmbeddingModel(onnxManager: ONNXRuntimeManager): EmbeddingModel =
        EmbeddingModel(onnxManager)

    @Provides
    @Singleton
    fun provideVADModel(onnxManager: ONNXRuntimeManager): VADModel =
        VADModel(onnxManager)

    @Provides
    @Singleton
    fun provideSpeakerClustering(): SpeakerClustering = SpeakerClustering()

    @Provides
    @Singleton
    fun provideModelDownloader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): ModelDownloader = ModelDownloader(context, okHttpClient)
}

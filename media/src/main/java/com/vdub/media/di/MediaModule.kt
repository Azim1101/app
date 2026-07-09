package com.vdub.media.di

import android.content.Context
import com.vdub.media.AudioExtractor
import com.vdub.media.AudioReader
import com.vdub.media.FFmpegProcessor
import com.vdub.media.WaveformGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Provides
    @Singleton
    fun provideFFmpegProcessor(): FFmpegProcessor = FFmpegProcessor()

    @Provides
    @Singleton
    fun provideAudioExtractor(
        @ApplicationContext context: Context,
        processor: FFmpegProcessor
    ): AudioExtractor = AudioExtractor(context, processor)

    @Provides
    @Singleton
    fun provideWaveformGenerator(@ApplicationContext context: Context): WaveformGenerator =
        WaveformGenerator(context)

    @Provides
    @Singleton
    fun provideAudioReader(): AudioReader = AudioReader()
}

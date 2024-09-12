package net.mortalsilence.droidfoss.modules

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.mortalsilence.droidfoss.DandroidApplication
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class CoroutineScopeModule {

    @Provides
    @Singleton
    fun provideApplicationScope(application: Application) =
        (application as DandroidApplication).applicationScope

}
/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.data.network

import android.content.Context
import android.util.Base64
import android.util.Log
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.data.model.Algorithm
import com.aurora.store.data.model.ProxyInfo
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_PROXY_INFO
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.Authenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OkHttpClientModule {

    private const val TAG = "HttpClient"

    private const val CERT_BEGIN = "-----BEGIN CERTIFICATE-----"
    private const val CERT_END = "-----END CERTIFICATE-----"

    @Provides
    @Singleton
    fun providesOkHttpClientInstance(certPinner: CertificatePinner, proxy: Proxy?): OkHttpClient {
        val okHttpClientBuilder = OkHttpClient().newBuilder()
            .proxy(proxy)
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)

        if (!BuildConfig.DEBUG) {
            okHttpClientBuilder.certificatePinner(certPinner)
        }

        return okHttpClientBuilder.build()
    }

    @Provides
    @Singleton
    fun providesCertificatePinnerInstance(@ApplicationContext context: Context): CertificatePinner {
        // Google needs special handling, see: https://pki.goog/faq/#faq-27
        val googleRootCerts = getGoogleRootCertHashes(context).map { "sha256/$it" }
            .toTypedArray()

        return  CertificatePinner.Builder()
            .add("*.googleapis.com", *googleRootCerts)
            .add("*.google.com", *googleRootCerts)
            .add("auroraoss.com", "sha256/mEflZT5enoR1FuXLgYYGqnVEoZvmf9c2bVBpiOjYQ0c=") // GTS Root R4
            .add("*.exodus-privacy.eu.org", "sha256/C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M=") // ISRG Root X1
            .add("gitlab.com", "sha256/x4QzPSC810K5/cMjb05Qm4k3Bw5zBn4lTdO/nEW/Td4=") // USERTrust RSA Certification Authority
            .add("plexus.techlore.tech", "sha256/C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M=") // ISRG Root X1
            .build()
    }

    @Provides
    @Singleton
    fun providesProxyInstance(@ApplicationContext context: Context, gson: Gson): Proxy? {
        val proxyInfoString = Preferences.getString(context, PREFERENCE_PROXY_INFO)
        if (proxyInfoString.isNotBlank() && proxyInfoString != "{}") {
            val proxyInfo = gson.fromJson(proxyInfoString, ProxyInfo::class.java)

            val proxy = Proxy(
                if (proxyInfo.protocol.removeSuffix("5") == "SOCKS") Proxy.Type.SOCKS else Proxy.Type.HTTP,
                InetSocketAddress.createUnresolved(proxyInfo.host, proxyInfo.port)
            )

            val proxyUser = proxyInfo.proxyUser
            val proxyPassword = proxyInfo.proxyPassword

            if (!proxyUser.isNullOrBlank() && !proxyPassword.isNullOrBlank()) {
                Authenticator.setDefault(object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(proxyUser, proxyPassword.toCharArray())
                    }
                })
            }
            return proxy
        } else {
            Log.i(TAG, "Proxy is disabled")
            return null
        }
    }

    private fun getGoogleRootCertHashes(context: Context): List<String> {
        return try {
            val certs = getX509Certificates(context.resources.openRawResource(R.raw.google_roots_ca))
            certs.map {
                val messageDigest = MessageDigest.getInstance(Algorithm.SHA256.value)
                messageDigest.update(it.publicKey.encoded)
                Base64.encodeToString(messageDigest.digest(), Base64.NO_WRAP)
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to get SHA256 certificate hash", exception)
            emptyList()
        }
    }

    private fun getX509Certificates(inputStream: InputStream): List<X509Certificate> {
        val certificateFactory = CertificateFactory.getInstance("X509")
        val rawCerts = inputStream
            .bufferedReader()
            .use { it.readText() }
            .split(CERT_END)
            .map { it.substringAfter(CERT_BEGIN).substringBefore(CERT_END).replace("\n", "") }
            .filterNot { it.isBlank() }
        val decodedCerts = rawCerts.map { Base64.decode(it, Base64.DEFAULT) }
        return decodedCerts.map {
            certificateFactory.generateCertificate(ByteArrayInputStream(it)) as X509Certificate
        }
    }
}

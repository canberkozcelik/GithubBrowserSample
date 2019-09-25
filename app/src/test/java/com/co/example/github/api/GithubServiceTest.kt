/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.co.example.github.api

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.co.example.github.util.LiveDataCallAdapterFactory
import com.co.example.github.util.LiveDataTestUtil.getValue
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Okio
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.core.IsNull.notNullValue
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@RunWith(JUnit4::class)
class GithubServiceTest {
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var service: GithubService

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun createService() {
        mockWebServer = MockWebServer()
        service = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(LiveDataCallAdapterFactory())
            .build()
            .create(GithubService::class.java)
    }

    @After
    fun stopService() {
        mockWebServer.shutdown()
    }

    @Test
    fun getRepos() {
        enqueueResponse("repos-canberk.json")
        val repos = (getValue(service.getUserRepos("canberkozcelik")) as ApiSuccessResponse).body

        val request = mockWebServer.takeRequest()
        assertThat(request.path, `is`("/users/canberkozcelik/repos"))

        assertThat(repos.size, `is`(2))

        val repo = repos[0]
        assertThat(repo.fullName, `is`("canberkozcelik/StatusStories"))

        val owner = repo.owner
        assertThat(owner, notNullValue())
        assertThat(owner.login, `is`("canberkozcelik"))
        assertThat(owner.url, `is`("https://api.github.com/users/canberkozcelik"))

        val repo2 = repos[1]
        assertThat(repo2.fullName, `is`("canberkozcelik/stories"))
    }

    private fun enqueueResponse(fileName: String, headers: Map<String, String> = emptyMap()) {
        val inputStream = javaClass.classLoader
            .getResourceAsStream("api-response/$fileName")
        val source = Okio.buffer(Okio.source(inputStream))
        val mockResponse = MockResponse()
        for ((key, value) in headers) {
            mockResponse.addHeader(key, value)
        }
        mockWebServer.enqueue(
            mockResponse
                .setBody(source.readString(Charsets.UTF_8))
        )
    }
}

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

package com.co.example.github.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.co.example.github.repository.UserRepoRepository
import com.co.example.github.testing.OpenForTesting
import com.co.example.github.util.AbsentLiveData
import com.co.example.github.vo.Resource
import com.co.example.github.vo.UserRepo
import java.util.*
import javax.inject.Inject

@OpenForTesting
class SearchViewModel @Inject constructor(repoRepository: UserRepoRepository) : ViewModel() {

    private val _query = MutableLiveData<String>()

    val query : LiveData<String> = _query

    val results: LiveData<Resource<List<UserRepo>>> = Transformations
        .switchMap(_query) { search ->
            if (search.isNullOrBlank()) {
                AbsentLiveData.create()
            } else {
                repoRepository.search(search)
            }
        }

    fun setQuery(originalInput: String) {
        val input = originalInput.toLowerCase(Locale.getDefault()).trim()
        if (input == _query.value) {
            return
        }
        _query.value = input
    }

    fun refresh() {
        _query.value?.let {
            _query.value = it
        }
    }

}

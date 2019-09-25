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

import androidx.lifecycle.MutableLiveData
import androidx.databinding.DataBindingComponent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressKey
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import androidx.recyclerview.widget.RecyclerView
import android.view.KeyEvent
import androidx.navigation.NavController
import com.co.example.github.R
import com.co.example.github.binding.FragmentBindingAdapters
import com.co.example.github.testing.SingleFragmentActivity
import com.co.example.github.util.CountingAppExecutorsRule
import com.co.example.github.util.DataBindingIdlingResourceRule
import com.co.example.github.util.EspressoTestUtil
import com.co.example.github.util.RecyclerViewMatcher
import com.co.example.github.util.TaskExecutorWithIdlingResourceRule
import com.co.example.github.util.TestUtil
import com.co.example.github.util.ViewModelUtil
import com.co.example.github.util.mock
import com.co.example.github.vo.Resource
import com.co.example.github.vo.UserRepo
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class SearchFragmentTest {
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(SingleFragmentActivity::class.java, true, true)
    @Rule
    @JvmField
    val executorRule = TaskExecutorWithIdlingResourceRule()
    @Rule
    @JvmField
    val countingAppExecutors = CountingAppExecutorsRule()
    @Rule
    @JvmField
    val dataBindingIdlingResourceRule = DataBindingIdlingResourceRule(activityRule)

    private lateinit var mockBindingAdapter: FragmentBindingAdapters
    private lateinit var viewModel: SearchViewModel
    private val results = MutableLiveData<Resource<List<UserRepo>>>()
    private val searchFragment = TestSearchFragment()

    @Before
    fun init() {
        viewModel = mock(SearchViewModel::class.java)
        `when`(viewModel.results).thenReturn(results)

        mockBindingAdapter = mock(FragmentBindingAdapters::class.java)

        searchFragment.appExecutors = countingAppExecutors.appExecutors
        searchFragment.viewModelFactory = ViewModelUtil.createFor(viewModel)
        searchFragment.dataBindingComponent = object : DataBindingComponent {
            override fun getFragmentBindingAdapters(): FragmentBindingAdapters {
                return mockBindingAdapter
            }
        }
        activityRule.activity.setFragment(searchFragment)
        EspressoTestUtil.disableProgressBarAnimations(activityRule)
    }

    @Test
    fun search() {
        onView(withId(R.id.progress_bar)).check(matches(not(isDisplayed())))
        onView(withId(R.id.input)).perform(
            typeText("foo"),
            pressKey(KeyEvent.KEYCODE_ENTER)
        )
        verify(viewModel).setQuery("foo")
        results.postValue(Resource.loading(null))
        onView(withId(R.id.progress_bar)).check(matches(isDisplayed()))
    }

    @Test
    fun loadResults() {
        val repo = TestUtil.createRepo("foo", "bar", "desc")
        results.postValue(Resource.success(arrayListOf(repo)))
        onView(listMatcher().atPosition(0)).check(matches(hasDescendant(withText("foo/bar"))))
        onView(withId(R.id.progress_bar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun dataWithLoading() {
        val repo = TestUtil.createRepo("foo", "bar", "desc")
        results.postValue(Resource.loading(arrayListOf(repo)))
        onView(listMatcher().atPosition(0)).check(matches(hasDescendant(withText("foo/bar"))))
        onView(withId(R.id.progress_bar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun error() {
        results.postValue(Resource.error("failed to load", null))
        onView(withId(R.id.error_msg)).check(matches(isDisplayed()))
    }

    @Test
    fun loadMore() {
        val repos = TestUtil.createRepos(50, "foo", "barr", "desc")
        results.postValue(Resource.success(repos))
        val action = RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(49)
        onView(withId(R.id.repo_list)).perform(action)
        onView(listMatcher().atPosition(49)).check(matches(isDisplayed()))
        verify(viewModel).loadNextPage()
    }

    @Test
    fun navigateToRepo() {
        doNothing().`when`<SearchViewModel>(viewModel).loadNextPage()
        val repo = TestUtil.createRepo("foo", "bar", "desc")
        results.postValue(Resource.success(arrayListOf(repo)))
        onView(withText("desc")).perform(click())
        verify(searchFragment.navController).navigate(
                SearchFragmentDirections.showRepo("foo", "bar")
        )
    }

    private fun listMatcher(): RecyclerViewMatcher {
        return RecyclerViewMatcher(R.id.repo_list)
    }

    class TestSearchFragment : SearchFragment() {
        val navController = mock<NavController>()
        override fun navController() = navController
    }
}
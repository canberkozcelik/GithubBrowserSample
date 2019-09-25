package com.co.example.github.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.co.example.github.AppExecutors
import com.co.example.github.api.ApiSuccessResponse
import com.co.example.github.api.GithubService
import com.co.example.github.db.GithubDb
import com.co.example.github.db.UserRepoDao
import com.co.example.github.util.AbsentLiveData
import com.co.example.github.util.RateLimiter
import com.co.example.github.vo.RepoSearchResult
import com.co.example.github.vo.Resource
import com.co.example.github.vo.UserRepo
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepoRepository @Inject constructor(
        private val appExecutors: AppExecutors,
        private val db: GithubDb,
        private val userRepoDao: UserRepoDao,
        private val githubService: GithubService
) {
    private val repoListRateLimit = RateLimiter<String>(10, TimeUnit.MINUTES)

    fun loadRepos(owner: String): LiveData<Resource<List<UserRepo>>> {
        return object : NetworkBoundResource<List<UserRepo>, List<UserRepo>>(appExecutors) {
            override fun saveCallResult(item: List<UserRepo>) {
                userRepoDao.insertRepos(item)
            }

            override fun shouldFetch(data: List<UserRepo>?): Boolean {
                return data == null || data.isEmpty() || repoListRateLimit.shouldFetch(owner)
            }

            override fun loadFromDb() = userRepoDao.loadRepositories(owner)

            override fun createCall() = githubService.getUserRepos(owner)

            override fun onFetchFailed() {
                repoListRateLimit.reset(owner)
            }
        }.asLiveData()
    }

    fun loadRepo(owner: String, name: String): LiveData<Resource<UserRepo>> {
        return object : NetworkBoundResource<UserRepo, UserRepo>(appExecutors) {
            override fun saveCallResult(item: UserRepo) {
                userRepoDao.insert(item)
            }

            override fun shouldFetch(data: UserRepo?) = data == null

            override fun loadFromDb() = userRepoDao.load(
                    ownerLogin = owner,
                    name = name
            )

            override fun createCall() = githubService.getUserRepo(
                    owner = owner,
                    name = name
            )
        }.asLiveData()
    }

    /**
     * query stands for user name
     */
    fun search(query: String): LiveData<Resource<List<UserRepo>>> {
        return object : NetworkBoundResource<List<UserRepo>, List<UserRepo>>(appExecutors) {

            override fun saveCallResult(item: List<UserRepo>) {
                val repoIds = item.map { it.id }
                val repoSearchResult = RepoSearchResult(
                        query = query,
                        repoIds = repoIds
                )
                db.beginTransaction()
                try {
                    userRepoDao.insertRepos(item)
                    userRepoDao.insert(repoSearchResult)
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }

            override fun shouldFetch(data: List<UserRepo>?) = data == null

            override fun loadFromDb(): LiveData<List<UserRepo>> {
                return Transformations.switchMap(userRepoDao.search(query)) { searchData ->
                    if (searchData == null) {
                        AbsentLiveData.create()
                    } else {
                        userRepoDao.loadOrdered(searchData.repoIds)
                    }
                }
            }

            override fun createCall() = githubService.getUserRepos(query)

            override fun processResponse(response: ApiSuccessResponse<List<UserRepo>>)
                    : List<UserRepo> {
                return response.body
            }
        }.asLiveData()
    }
}
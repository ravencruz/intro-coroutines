package tasks

import contributors.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

fun loadContributorsCallbacks(service: GitHubService, req: RequestData, updateResults: (List<User>) -> Unit) {
    service.getOrgReposCall(req.org).onResponse { responseRepos ->
        logRepos(req, responseRepos)
        val repos = responseRepos.bodyList()

        val allUsers = Collections.synchronizedList(mutableListOf<User>())

        // comparando con el index
        // . If the request processing the last repo returns faster than some prior requests (which is likely to happen),
        // all the results for requests that take more time to process will be lost.

        // One of the ways to fix this is to introduce an index and check whether
        // we've processed all the repositories already
        val numberOfProcessed = AtomicInteger()

        for ( (index, repo) in repos.withIndex()) {
            service.getRepoContributorsCall(req.org, repo.name).onResponse { responseUsers ->
                logUsers(repo, responseUsers)
                val users = responseUsers.bodyList()
                allUsers += users

                if (numberOfProcessed.incrementAndGet() == repos.size) {
                    updateResults(allUsers.aggregate())
                }
            }
        }
        // TODO: Why this code doesn't work? How to fix that?
//        updateResults(allUsers.aggregate())
    }
}

inline fun <T> Call<T>.onResponse(crossinline callback: (Response<T>) -> Unit) {
    enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            callback(response)
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            log.error("Call failed", t)
        }
    })
}

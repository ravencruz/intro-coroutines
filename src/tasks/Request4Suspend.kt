package tasks

import contributors.*

/**
 * In this version, while waiting for the result, we don't reuse the thread for sending other requests, because
 *  we have written our code in a sequential way. The new request is sent only when the previous result is received.
 * suspend functions treat the thread fairly and don't block it for "waiting",
 * but it doesn't yet bring any concurrency to the picture.
 * Let's see how this can be improved
 */
suspend fun loadContributorsSuspend(service: GitHubService, req: RequestData): List<User> {
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .bodyList()

    return repos.flatMap { repo ->
        service
            .getRepoContributors(req.org, repo.name)
            .also { logUsers(repo, it) }
            .bodyList()
    }.aggregate()
}
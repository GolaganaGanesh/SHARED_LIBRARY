#!/usr/bin/env groovy

//--------------------------------------------------------------------------
// This shared library will colorized message
//--------------------------------------------------------------------------

import groovy.json.*
import org.codehaus.groovy.runtime.StackTraceUtils
import groovy.transform.Field

// import jenkins.model.*
@Field  sURL = env.SWARM_URL ?: constants.SWARM_URL
@Field  sAPI = env.SWARM_API ?: constants.SWARM_API

def queueStatus(fields="") {
    // queueStatus output example:
    //[futureTasks:13, maxWorkers:12, pingError:false, tasks:0, workerLifetime:75s, workers:2]

    try {
        def response = commons.httpRequest()
        log.trace "RESPONSE TYPE in SWARM.queueStatus:" +  response.getClass() + response
        return response
    } catch(e){
        log.warn( "NON-CRITICAL FAILURE: Cant get queue status")
        log.err(e.toString())
    }
}

def getReviewInfo(reviewID = params.review, fields="") {
    // need to add fields validaton
    // an empty value will show all fields
    // when incorrect fail will return empty list
    try {
        def config = [
            'url': "${sURL}${sAPI}/reviews/${reviewID}?fields=${fields}"
        ]
        log.trace "CONFIG:\n${config}"
        def response = commons.httpRequest(config)
        log.trace response
        return response.review
    } catch(e) {
        log.warn  ( "NON-CRITICAL FAILURE: Cannot get ${fields} fields for ${review} from Swarm")
        log.err Thread.currentThread().stackTrace[10].methodName  + " : " +  e.getMessage()
    }

}

def getAuthorID(reviewID = params.review, fullName = false) {
    String author
    author = getReviewInfo(reviewID, "author").author
    log.trace "[SWARM.getAuthorID]: ${author}"
    if (fullName) { author = "${author} ${getUserName(author)}" }
    return author
}

def getUserName(userID, fields='User,FullName,Email') {
    if (!userID) { return "No user provided" }

    try {
        url = "${sURL}${sAPI}/users?fields=${fields}&users=${userID}"
        log.trace "getUserName: URL: ${url}"
        def response = commons.httpRequest([url: url])
        log.trace "RESPONSE TYPE in SWARM.getAuthorName:" +  response.getClass()  + " : " + response
        return  "<${response[0].Email}> (${response[0].FullName})"
    } catch(e) {
        log.err Thread.currentThread().stackTrace[10].methodName  + " : " +  e.getMessage()
        log.err e.toString()
    }
}

// function to update SWARM with a Jenkins URL and build status
// If 'update' parameter exists, call it
def updateReview(reviewID = params.review, status = "running", message = "") {
    try {
        if (params.update && params.update.startsWith("https"))  {
            def json = commons.arrayToJsonString([messages: [message], status: status, url: env.BUILD_URL])
            def config = [
                'url': params.update,
                'method': "POST",
                'jstr': json
            ]

            log.trace "[swarm.updateReview]: config: ${config}"
            def response = commons.httpRequest(config)
            log.trace "[swarm.updateReview]: response: <${response.getClass()}>: ${response}"
            log.pass  "Review's ${reviewID} testDefinition is uppdated"

        } else {
            log.debug "No update param provided, nothing to do..."
        }
    } catch (e) {
        log.err e
        def marker = new Throwable()
        log.err "${StackTraceUtils.sanitize(marker).stackTrace[0]} ${Thread.currentThread().stackTrace[10].methodName}: ${e.toString()}"
    }
}

//Get Reveiw Description
//return Description multiline string
def getReviewDescription(reviewID = params.review) {
    return getReviewInfo(reviewID, "description").description
}

def updateReviewDescription(desc, reviewID = params.review) {
    if (! desc?.trim()) {
        log.warn "Description can not be empty"
        return
    }
    def json = commons.arrayToJsonString([description: desc ])

    log.trace "updateReviewDescription:JSON:\n${json}"
    def config = [
        'url': "${sURL}/api/v10/reviews/${reviewID}/description",
        'method': "PUT",
        'jstr': json
    ]

    log.trace "CONFIG:\n ${config}"
    def response = commons.httpRequest(config)
    return "Description of ${reviewID} review is uppdated"
}

// Get test details
// https://sp4-fp-swarm.cisco.com/docs/Content/Swarm/swarm-apidoc_endpoint_integration_tests.html#Get_testrun_details_of_a_review_version
def getTestInfo(version="", reviewID = params.review) {

    def url = "${sURL}/api/v10/reviews/${reviewID}/testruns?version=${version}"
    log.trace "[SWARM.getTestInfo]:URL: ${url}"
    def response = commons.httpRequest([url:url])
    log.trace "[SWARM.getTestInfo]: response: ${response.getClass()}:\n ${response}"

    if (response.error) {
        log.warn "[getTestInfo] Failed to get testinfo for ${reviewID}\n${response.error}"
        return response
    } else {
        log.trace "[SWARM.getTestInfo]:\n${response.data.testruns}"
        return response.data.testruns
    }
}

def getListOfTestURLs(reviewID = params.review, all=false) {
    // return list of all test for given review
    // by default, we want to exclude the latest test
    // if 'all' is true, include the latest/curren resulst as well

    // get latest version of the review
    def reviewInfo = getTestInfo("",reviewID)
    log.trace "[SWARM.getListOfTestURLs]: reviewInfo:\n ${reviewInfo}"

    def urlsList = []
    if (all) {
        urlsList << [ 'url': reviewInfo.url, 'status': reviewInfo.status]
    }
    log.trace "[SWARM.getListOfTestURLs]: version: class=${reviewInfo.version.getClass()}, value=${reviewInfo.version[0]}"
    def version = reviewInfo.version[0] - 1

    while ( version > 1 ) {
        reviewInfo  = getTestInfo(version,reviewID)
        urlsList << [ 'url': reviewInfo.url[0], 'status': reviewInfo.status[0]]
        version--
    }
    return urlsList
}

def abortPreviousBuilds( reviewID = params.review) {
    def urlsList = getListOfTestURLs(reviewID)
    urlsList.each {
	log.trace "[abortPreviousBuilds]: ${it}"
        if (it.status == "running") {
            log.trace "[abortPreviousBuilds]: url: ${it.url}"
            commons.StopJenkinsJob(it.url)
        }
    }
}

// Get Review status
def getcommitStatus(reviewID = params.review) {
    return getReviewInfo(reviewID, "commitStatus" ).commitStatus
}

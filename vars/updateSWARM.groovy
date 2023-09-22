#!/usr/bin/env groovy

//--------------------------------------------------------------------------
// This shared library will check if 'update' parameter exists
// and updates SWARM with the job URL
// For more details check the following documention
// hhttps://www.perforce.com/manuals/swarm-admin/Content/Swarm/admin.integrate_test_suite.html
// The {update} callback url accepts a JSON body where you can specify any or all of the following: messages, url, and status.
// for exmaple:
// {
//     "messages" : ["My First Message", "My Second Message"],
//     "url" : "http://jenkins_host:8080/link_to_run",
//     "status": "pass"
// }
// Valid test status values are 'running', 'pass', and 'fail'.
//--------------------------------------------------------------------------

import groovy.json.*

def call(String status="running", String message="") {
    try {

        // If update parameter exists, call it
        // to update SWARM with a Jenkins URL and status
        if(params.update.startsWith("https"))  {
            postRequest(status, message)
        } else {
          println ("No update param provided, nothing to do...")
        }

    } catch (err) {
        println ("Error: ${err.getMessage()}")
        //throw err
    }
}

def postRequest(String status, String message) {

    def str = '{ "url": "' + "${env.BUILD_URL}" + '", "status": "' +  status+ '", "messages": [ "'+ message + '" ] }'
    println( "JSON TO POST: " +  JsonOutput.prettyPrint(str))

    def http = new URL(params.update).openConnection() as HttpURLConnection
    http.setRequestMethod('POST')
    http.setDoOutput(true)
    http.setRequestProperty("Accept", 'application/json')
    http.setRequestProperty("Content-Type", 'application/json')

    http.outputStream.write(str.getBytes("UTF-8"))
    http.connect()

    def response = [:]

    if (http.responseCode == 200) {
        response = new JsonSlurper().parseText(http.inputStream.getText('UTF-8'))
    } else {
        response = new JsonSlurper().parseText(http.errorStream.getText('UTF-8'))
    }

    println ("Response: " +  JsonOutput.prettyPrint(JsonOutput.toJson(response)))
}

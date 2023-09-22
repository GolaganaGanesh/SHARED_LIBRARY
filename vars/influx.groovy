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
influxCustomTags = influxCustomTags("${params.branchName}", "${params.review}", "${AUTHOR}", "${JENKINSHOST}"  )



def influxCustomTags(branchName, review, AUTHOR, JENKINSHOST){
    def influxCustomTags = [
    "Branch": "fxplatform:${branchName}",
    "ReviewID":"${review}",
    "Review_Author":"${AUTHOR}",
    "project_and_branch": JOB_NAME.replaceAll('/','_') + ":${branchName}",
    "Jenkinshost": "${JENKINSHOST}",
    "Coverity": " ", 
    "Coverity_runstatus":"None",
    "Build_Stage":" ",
    "Exception":"",
    "Build_Node":" "
    ]
    
    return influxCustomTags
} 



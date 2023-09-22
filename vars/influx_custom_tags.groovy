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
def influxCustomTags(branchName, review, author, jenkinshost, buildNode , stage, except, coverity,coverityStatus){
	def influxCustomTags = [
		"Branch": "${branchName}",
		"ReviewID":"${review}",
		"Review_Author":"${author}",
		"project_and_branch": JOB_NAME.replaceAll('/','_') + ":${branchName}",
		"Jenkinshost": "${jenkinshost}",
		"Coverity": "${coverity}",
		"Coverity_runstatus":"${coverityStatus}",
		"Build_Stage":"${stage} ",
		"Except":"${except}",
		"Build_Node":"${buildNode}"
]	

return influxCustomTags
}






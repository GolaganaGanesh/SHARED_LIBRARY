import groovy.json.JsonOutput
def stageStatuses = [:]
node {
    try {
        stage('BUILD') {
            try {
                echo 'BUILD'
                stageStatuses['BUILD    '] = 'SUCCESS '
            } catch (Exception e) {
                stageStatuses['TEST      '] = 'FAILURE '
                echo 'BUILD stage failed'
                currentBuild.result = 'FAILURE'
            }
        }
        
        stage('TEST') {
            try {
                sh 'TEST'//this is windows machine
                stageStatuses['TEST      '] = 'SUCCESS '
            } catch (Exception e) {
                stageStatuses['TEST      '] = 'FAILURE '
                echo 'stage-3 failed'
                currentBuild.result = 'FAILURE'
            }
        }
        stage('DEPLOY') {
            try {
                echo 'DEPLOY'
                stageStatuses['DEPLOY '] = 'SUCCESS '
            } catch (Exception e) {
                stageStatuses['DEPLOY '] = 'FAILURE '
                echo 'DEPLOY stage failed'
                currentBuild.result = 'FAILURE'
            }
        }
    } catch (Exception e) {
        currentBuild.result = 'FAILURE'
        throw e
    } finally {
        def BUILD_URL = env.BUILD_URL
        def botToken =  'Yzc4ZDFjMTQtOWRjMS00MzdhLWFkMGYtMjcxNTE3ZjEwNmYxODhiNGYyNTktNWRj_PF84_1eb65fdf-9643-417f-9974-ad72cae0e10f'
        def roomId = 'Y2lzY29zcGFyazovL3VzL1JPT00vZmJhOTQzYzAtNDcxYS0xMWVlLWJhNGItYWYxOGFjMzkwN2Ew'
        def buildNumber = currentBuild.fullDisplayName
        def buildResult = currentBuild.result
        def currentStage = env.STAGE_NAME
        def build_url = currentBuild.absoluteUrl
        def build_fullDisplayName = env.fullDisplayName
        def STAGES_STATUS_1 = ''
        stageStatuses.each { stageName, status ->
            echo "Stage Name: ${stageName}, Status: ${status}"
            STAGES_STATUS_1 += stageName + ' : ' + status + '\n '
            echo STAGES_STATUS_1
        }
        def Job_triggered_By = ''
        if (currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause)) {
            def userIdCause = currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause)
            def userName = userIdCause.getUserName()
            echo "This job was started by: ${userName}"
            Job_triggered_By = "This job was started by: ${userName}"
        } else {
            echo "This job was not started by a user."
            Job_triggered_By = "This job was not started by a user."
        }
        def message = "${buildNumber}  >> [${buildResult}](${BUILD_URL}). \n ${STAGES_STATUS_1}${Job_triggered_By}"
        echo "Stage Statuses: ${stageStatuses}"
        def response = httpRequest(
            contentType: 'APPLICATION_JSON',
            httpMode: 'POST',
            requestBody: JsonOutput.toJson([roomId: roomId, text: message]),
            url: "https://api.ciscospark.com/v1/messages",
            customHeaders: [[name: 'Authorization', value: "Bearer ${botToken}"]]
        )
        if (response.status != 200) {
            error("Failed to send Webex message. Status code: ${response.status}")
        }
    }
}

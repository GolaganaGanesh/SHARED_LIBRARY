import groovy.json.JsonOutput
def stageStatuses = [:]
def roomFound = false
def response=""
def room_ID=''
def room_name=env.JOB_BASE_NAME
def botToken = 'ZmYwNmE2Y2UtMTFmZi00ODc2LTgxMDgtNzg4NmJhNjM4YzkzMDI2MzUxOWYtNWE2_PF84_1eb65fdf-9643-417f-9974-ad72cae0e10f'
def apiUrl = "https://api.ciscospark.com/v1/rooms"
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
        try{
                        response = sh(script: "curl -H 'Authorization: Bearer ${botToken}' ${apiUrl}", returnStdout: true).trim()
                        def rooms = new groovy.json.JsonSlurper().parseText(response)
                        rooms.items.each { room ->
                            def roomId = room.id
                            roomName = room.title ?: room.roomTitle  // Use 'title' or 'roomTitle' depending on API response
                            if (roomName == room_name) {
                                 roomFound = true
                                 room_ID = roomId
                                 echo "SUCCESSFULLY ROOM NAME WAS THERE  ${room_ID}"
                            }
                        }
                    }catch (Exception e) {
                        echo "this is  not exist"
                    }
                    if (roomFound) {
                            echo "Room '${room_name}' exists."
                    } else {
                            echo "Room '${room_name}' not found."
                            def NEW_ROOM=room_name
                            def payload = '{"title":"' + NEW_ROOM + '"}'
                            try{
                                response = sh(script: "curl -X POST -H 'Authorization: Bearer ${botToken}' -H 'Content-Type: application/json' -d '${payload}' ${apiUrl}", returnStatus: false)
                            }catch (Exception e) {
                                echo "room was created "
                            }
                            
                            echo "Stage Name_RESPONSE: ${response}"
                            if (response == null) {
                               echo "Webex Teams room '${room_name}' created successfully."
                               try{
                                response = sh(script: "curl -H 'Authorization: Bearer ${botToken}' ${apiUrl}", returnStdout: true).trim()
                                def rooms = new groovy.json.JsonSlurper().parseText(response)
                                rooms.items.each { room ->
                                    def roomId = room.id
                                    roomName = room.title ?: room.roomTitle  // Use 'title' or 'roomTitle' depending on API response
                                    if (roomName == room_name) {
                                        room_ID = roomId
                                        echo "SUCCESSFULLY ROOM NAME WAS CREATED  :  ${room_ID}"
                                    }
                                }
                            }catch (Exception e) {
                                echo "this is  not exist"
                            }
                            } else {
                               error "Failed to create Webex Teams room. HTTP response code: ${response}"
                            }
                    }
        def BUILD_URL = env.BUILD_URL
        def ACCESS_TOKEN ="ZmYwNmE2Y2UtMTFmZi00ODc2LTgxMDgtNzg4NmJhNjM4YzkzMDI2MzUxOWYtNWE2_PF84_1eb65fdf-9643-417f-9974-ad72cae0e10f"
        def WEBEX_API_URL = "https://webexapis.com/v1"
        //def WEBEX_GROUP_ID = "Y2lzY29zcGFyazovL3VzL1JPT00vMDBmZWFiNzAtNWU4Ny0xMWVlLWE1NmMtNDc3MGY3ODlmZTNh"
        def WEBEX_GROUP_ID=room_ID
        def Job_triggered_By = ''
        def userId
        if (currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause)) {
            def userIdCause = currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause)
            def userName = userIdCause.getUserName()
            echo "This job was started by: ${userName}"
            Job_triggered_By = "This job was started by: ${userName}"
            userId = userIdCause.getUserId()
            echo "User ID of the person who triggered the build: ${userId}"
        } else {
            echo "This job was not started by a user."
            Job_triggered_By = "This job was not started by a user."
            echo "This build was not triggered by a specific user."
        }
        def USER_EMAIL = userId+"@cisco.com"
        echo "User ID of the person who triggered the build: ${USER_EMAIL}"
        def curlCmd = """
            curl -X POST -H "Authorization: Bearer ${ACCESS_TOKEN}" \
            -H "Content-Type: application/json" \
            -d '{
                "roomId": "${WEBEX_GROUP_ID}",
                "personEmail": "${USER_EMAIL}"
             }' \
            ${WEBEX_API_URL}/memberships
            """
        response = sh(script: curlCmd, returnStatus: true)
        if (response == 0) {
             echo "User ${USER_EMAIL} added to Webex group with ID ${WEBEX_GROUP_ID} successfully."
        } else {
             echo "Failed to add user to Webex group. Exit code: ${response}"
        }
        def buildNumber = currentBuild.fullDisplayName
        def buildResult = currentBuild.result
        def currentStage = env.STAGE_NAME
        def build_url = env.BUILD_URL
        def build_fullDisplayName = env.fullDisplayName
        def STAGES_STATUS_1 = ''
        stageStatuses.each { stageName, status ->
            echo "Stage Name: ${stageName}, Status: ${status}"
            STAGES_STATUS_1 += stageName + ' : ' + status + '\\n '
            echo STAGES_STATUS_1
        }
        def message1 = "${room_name}  >> [${buildResult}](${BUILD_URL}) \\n ${STAGES_STATUS_1}${Job_triggered_By}"
        def message =  "${buildNumber}  >> [${buildResult}](${BUILD_URL}). \\n ${STAGES_STATUS_1}${Job_triggered_By} "
        sh """
            curl -X POST \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer ZmYwNmE2Y2UtMTFmZi00ODc2LTgxMDgtNzg4NmJhNjM4YzkzMDI2MzUxOWYtNWE2_PF84_1eb65fdf-9643-417f-9974-ad72cae0e10f" \
            -d '{"roomId": "${WEBEX_GROUP_ID}", "markdown": "$message1"}' \
            https://api.ciscospark.com/v1/messages
        """
        echo "Stage Statuses: ${stageStatuses}"
    }
}
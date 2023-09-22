import groovy.time.*
import java.util.concurrent.TimeUnit
import hudson.model.*
import jenkins.model.Jenkins
import java.util.regex.*
import javax.mail.internet.MimeUtility

def call(emailNotify) {
  def inputParams = [:]
  emailNotify.resolveStrategy = Closure.DELEGATE_FIRST
  emailNotify.delegate = inputParams
  emailNotify()
  
  def build = [:]  
  node("master") {
    // Clean workspace before doing anything
    deleteDir()

    def start_epoch = "${currentBuild.startTimeInMillis}".toLong()
    def build_time  = "${currentBuild.duration}".toLong()
    def end_epoch = start_epoch + build_time

    def build_start = new Date(start_epoch)
    build.tim = "${build_start}"
    def build_end = new Date(end_epoch)
    elapsedTime = deltaTime(build_start, build_end)
    build.dur = "${elapsedTime}"

    // Input build parameters
    build.gen = "${inputParams.Genre}" // Project/Product/Component
    build.brn = "${inputParams.Branch}" // Branch name
    build.rec = "${inputParams.Maillist}" // Recipients Mail List
    build.ich = "${inputParams.Ichnaea}" // Ichnaea Build number
    build.ver = "${inputParams.Version}" // Version number

    // Pipeline Global variables
    build.job = "${env.JOB_BASE_NAME}" // Jenkins Job name
    build.url = "${currentBuild.absoluteUrl}" // Jenkins URL
    build.res = "${currentBuild.result}" // Build result
    build.num = "${currentBuild.number}" // Build Number
    build.des = "${currentBuild.description}" // Build Description
    build.dis = "${currentBuild.displayName}" // Build Display name

    // Build publish directory for CSM multibranch pipeline builds
    // NOTE: We call sendmail JSL w/ Branch set to "<release>#<branch>"
    build.pub = "${build.job}" 
    if(build.gen == "CSM" && "${build.brn}".count('#') == 1) {
        rel_name = "${build.brn}".tokenize('#')[0] // Release name 
        brn_name = "${build.brn}".tokenize('#')[1] // Branch name
        build.pub = "${rel_name}-${brn_name}"
    }

    sendNotification(build)
  } // Run on master
} // call

//triggers email upon success
def sendNotification(bld) {
    def build_bulb = ""
    def build_status = ""
    if("${bld.res}" == "SUCCESS") {
      build_bulb = "blue.gif"
      build_status = "Successful"
    }
    else if("${bld.res}" == "FAILURE") {
      build_bulb = "red.gif"
      build_status = "Failure"
    }
    else { // UNSTABLE Build
      build_bulb = "yellow.gif"
      build_status = "Unstable"
    }

    // -- CONSOLE OUTPUT --
    def consoleOutput = printConsoleOutput()
    
    // Parse the changelists
    def buildInfo = parseChangeLists()

    // -- GENERAL INFO --
    def generalInfo = []
    generalInfo.add("Build^${bld.dis}")
    if("${bld.gen}" == "USM") {
        generalInfo.add("Tag^${bld.des}")
    }
    else if("${bld.gen}" == "CSM") {
        generalInfo.add("Ichnaea URL^${bld.des}")
    }
    generalInfo.add("Build URL^${bld.url}")
    generalInfo.add("Project^${bld.gen}")
    generalInfo.add("Date of build^${bld.tim}")
    generalInfo.add("Build duration^${bld.dur}")
  
    def details = printGeneralInfo(bld, generalInfo)

    // -- CHANGE LIST --
    def changeLists = printChangeSets(buildInfo)
    details += "${changeLists}"

    details += "${consoleOutput}" // Appending the Console output to the email details (body)

    def sendToMailer = "vkarkad@cisco.com" // FIXME
    def sendToUsers = ""
    def sendCcUsers = ""
    buildInfo.CEC.each {
        // Tokenize and get "userid" out of "User Name#userid"
        def cecId = it.tokenize('#')[-1]
        if(cecId != null) {
          if(cecId == 'vmurphy' || cecId == 'tvkelley' || cecId == 'yifu2' || cecId == 'vkarkad') {
	    sendCcUsers += "${cecId}@cisco.com," 
          }
          else {
            sendToUsers += "${cecId}@cisco.com,"
          }
        }
    }
    buildInfo.OWN.each {
       // Get the actual owner of the defect
       def engineer = "${it}"
       if(engineer != null)
         sendToUsers += "${engineer}@cisco.com,"
    }
    def triggeredBy  = getBuildUser()
    if(triggeredBy != null)
          sendToMailer += ",${triggeredBy}@cisco.com"
    sendToUsers += sendToMailer

    def unicodeChar = "\u00BB" // Right-pointing Double Angle Quotation mark
    def subject = "${bld.gen}" + " ${unicodeChar} " + "${bld.brn}" + " ${unicodeChar} " + "${bld.job}" + " - Build " + "#${bld.num}" + " - " + "${build_status}" + "!"
    if("${bld.gen}".startsWith("USM")) {
        subject = "${bld.gen}" + " ${unicodeChar} " + "${bld.job}" + " - Build " + "#${bld.num}" + " - " + "${build_status}" + "!"
    }
    else if("${bld.gen}".startsWith("CSM")) {
        subject = "${bld.gen}" + " ${unicodeChar} " + "${bld.pub}" + " - Build " + "#${bld.num}" + " - " + "${build_status}" + "!"
    }
    else if("${bld.gen}".startsWith("Tools")) {
        subject = "${bld.gen}" + " ${unicodeChar} " + "${bld.job}" + " - Build " + "#${bld.num}" + " - " + "${build_status}" + "!"
    }
    def encodedSubject = MimeUtility.encodeText(subject, "utf-8", null)
    mail subject: "${encodedSubject}",
            body: details,
        mimeType: 'text/html',
              to: "${sendToUsers}", 
              cc: "${bld.rec},${sendCcUsers}",
         replyTo: 'csmrebld@cisco.com',
            from: 'csmrebld@cisco.com'
}

def deltaTime(first, last) {
    long delta = last.getTime() - first.getTime()

    long hh = delta / (60 * 60 * 1000)
    delta = delta % (60 * 60 * 1000)
    long mm = delta / (60 * 1000)
    delta = delta % (60 * 1000)
    long ss = delta / 1000
      
    dispHrs=""; dispMins=""; dispSecs=""; buildTime=""
    if(hh == 1) { dispHrs="${hh} hr " } else if(hh > 1) { dispHrs="${hh} hrs " }
    if(mm >= 1 && mm <= 59) { dispMins="${mm} min " }
    if(ss >= 1 && ss <= 59) { dispSecs="${ss} sec" }

    buildTime = "${dispHrs}${dispMins}${dispSecs}"
    return buildTime
}

def printGeneralInfo(build, general) {
    def bulb_path = "https://firepower-build.service.ntd.ciscolabs.com/static/5db4bb92/images/32x32"
    if("${build.res}" == "SUCCESS")
      build_bulb = "${bulb_path}/blue.gif"
    else if("${build.res}" == "FAILURE")
      build_bulb = "${bulb_path}/red.gif"
    else if("${build.res}" == "ABORTED")
      build_bulb = "${bulb_path}/grey.gif"
    else // UNSTABLE Build
      build_bulb = "${bulb_path}/yellow.gif"

    def style ="""<STYLE>BODY, TABLE, TD, TH, P {  font-family:Verdana,Helvetica,sans serif;  font-size:11px;  color:black;}h1 { color:black; }h2 { color:black; }h3 { color:black; }TD.bg1 { color:white; background-color:#0000C0; font-size:120% }TD.bg2 { color:white; background-color:#4040FF; font-size:110% }TD.bg3 { color:white; background-color:#8080FF; }TD.test_passed { color:blue; }TD.test_failed { color:red; }TD.console { font-family:Courier New; }.commit { font-family:Verdana; margin-bottom: 4px; background-color: #F5F0FA; border: 1px solid #CCB; padding: 4px; white-space: normal; }.message { font-family:Verdana; }</STYLE>"""
    def buildGenInfo = """${style}<BODY><TABLE>"""
    buildGenInfo += """<TR><TD colspan="2"><IMG SRC="${build_bulb}" /><B style="font-size: 200%">BUILD ${build.res}</B></TD></TR>"""
    general.each { data ->
      tokenList = data.tokenize('^')
      if(tokenList[0] =~ /URL/) {
        last = """<A href="${tokenList[-1]}">${tokenList[-1]}</A>"""
      }
      buildGenInfo += """<TR><TD>${tokenList[0]}:</TD><TD>${tokenList[-1]}</TD></TR>"""
    }   
    def build_pub = "" 
    if("${build.gen}" == "USM") {
	def project = "${build.gen}".toLowerCase()
        build_pub = "http://csm-site.esl.cisco.com/BLD/${project}/${build.job}/DEV/${build.ver}-${build.num}"
        buildGenInfo += """<TR><TD>Artifacts URL:</TD><TD><A href="${build_pub}">${build_pub}</A></TD></TR>"""
    }
    else if("${build.gen}" == "CSM") {
        build_pub = "http://blr-csm-site.cisco.com/${build.gen}/${build.pub}/builds/${build.num}/artifacts"
        buildGenInfo += """<TR><TD>Artifacts URL:</TD><TD><A href="${build_pub}">${build_pub}</A></TD></TR>"""
    }
    def ichnaea_build = "${build.ich}"
    if(!ichnaea_build) {
        def ichnaea_url = "https://stbu-releng.cisco.com/builds/${build.ich}"
        buildGenInfo += """<TR><TD>Ichnaea URL:</TD><TD><A href="${ichnaea_url}">${ichnaea_url}</A></TD></TR>"""
    }
    buildGenInfo += "</TABLE><BR/>"
    return buildGenInfo
}

def printChangeSets(clInfo) {
    def changeSets = """<div class="content"><h1>Changes</h1>"""
    if(clInfo.CLS) {
        clInfo.CLS.eachWithIndex { change, i ->
            def commitMsg = "${clInfo.MSG[i]}"
            def committer = "${clInfo.CEC[i]}"
            def owner = "${clInfo.OWN[i]}" // Actual owner of the defect
            def changeUrl = """<A href="https://stbu-p4-swarm.cisco.com/changes/${change}">${change}</A>"""
            def replaceStr = ""
            commitMsg = "${commitMsg}".replaceAll(/\</, "&lt;")
            commitMsg = "${commitMsg}".replaceAll(/\>/, "&gt;")
            Matcher m1 = "${commitMsg}" =~ /CSC[a-z]{2}[0-9]{5}/
            if(m1.find()) { // CDETS
              def bug_id = m1.group(0)
              replaceStr = """<B><A href="http://wwwin-metrics.cisco.com/cgi-bin/ddtsdisp.cgi?id=${bug_id}">${bug_id}</A></B>"""
              commitMsg = "${commitMsg}".replaceAll(/CSC[a-z]{2}[0-9]{5}/, "${replaceStr}")
            }
            Matcher m2 = "${commitMsg}" =~ /\#[0-9]+/ // If CDETS is NOT found, find targetprocess Userstory
            if(m2.find()) { // Targetprocess
              def user_story = m2.group(0).replaceFirst("#","")
              replaceStr = """<B><A href="https://targetprocess.cisco.com/entity/${user_story}">#${user_story}</A></B>"""
              commitMsg = "${commitMsg}".replaceAll(/\#[0-9]+/, "${replaceStr}")
            }
            def full_name = committer.tokenize('#')[0] // User Name e.g: Mutum Singh
            def cisco_id = committer.tokenize('#')[-1] // UserId e.g: mutsingh
            def author = full_name // For generic accounts like csmrebld or builders
	    def cisco_dir = """<B><A href="http://directory.cisco.com/dir/details/${cisco_id}">(${cisco_id})</A></B>"""
            if(cisco_id != owner) {
              def owner_dir = """<B><A href="http://directory.cisco.com/dir/details/${owner}">${owner}</A></B>"""
              author = "Owner: ${owner_dir} [WF checkin by: ${full_name} ${cisco_dir}]"
            }
            else if(cisco_id != "csmrebld" && cisco_id != "builders") {
              author = "${full_name} ${cisco_dir}"
            }
            changeSets += """<div class="commit">Revision <B>${changeUrl}</B> by <B>${author}</B><hr/><pre class="message">${commitMsg}</pre></div>"""
        }
    }
    else {
        changeSets += """<div class="commit"><B>No changes in this build</B><hr/>Fixes: None</div>"""
    }
    changeSets += """<br/></div>"""
    return changeSets
}

def printConsoleOutput() {
    def consoleOut = ""
    if(currentBuild.result == "FAILURE") {
        consoleOut = """<TABLE width="100%" cellpadding="0" cellspacing="0"><TR><TD class="bg1"><B>CONSOLE OUTPUT</B></TD></TR>"""
        def getLogList = currentBuild.rawBuild.getLog(100)
        getLogList.each { line ->
          consoleOut += """<TR><TD class="console">${org.apache.commons.lang.StringEscapeUtils.escapeHtml(line)}</TD></TR>"""
        }
        consoleOut += "</TABLE><BR/>"
    } 
    return consoleOut
}

@NonCPS
def getBuildUser() {
    // Get all Causes for the current build
    def causes = currentBuild.rawBuild.getCauses()
    echo "[DEBUG] causes: " + causes
    // Get a specific Cause type (in this case the user who kicked off the build), if present
    def specificCause = currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause)
    echo "[DEBUG] specificCause: " + specificCause
    def buildUserId = null
    if(specificCause != null) {
        buildUserId = currentBuild.rawBuild.getCause(Cause.UserIdCause).getUserId()
        echo "[DEBUG] Value of buidUserId: >>" + buildUserId + "<<"
    }
    if(!buildUserId || buildUserId == 'builders') {
        return "vkarkad"
    }
    return buildUserId
}

@NonCPS
def parseChangeLists() {
    def ChangeNum = []
    def CommitMsg = []
    def Committer = []
    def Owner = []

    // Iterate over all builds upto the one before the last successful build
    while(true) {
        // read the changenumber,message,cecid for each changelist and add to an array
        def changes = currentBuild.rawBuild.changeSets.items
        echo "++++++++++++++++++++++++++++++++++++++++++++++++++++"
        echo "Current Build Num: " + currentBuild.number
        echo "Current Build Res: " + currentBuild.result
        echo "Current Build Url: " + currentBuild.absoluteUrl
        echo "Current Build Trg: " + getBuildUser()
        changes.each {
            if(!it.toString().startsWith("[hudson.plugins.git")) {
                echo "Perforce changeNumber: " + it.changeNumber
                echo "Perforce commitId: " + it.commitId
                it.changeNumber.eachWithIndex { change, i ->
                    ChangeNum.add(change)
                    CommitMsg.add(it.msg[i])
                
                    def name_words = []
                    def name = it.author.fullName[i]
                    Matcher m3 = "${name}" =~ /\([a-zA-Z0-9]+\)/
                    if(m3.find()) {
                        name_words = name.tokenize('(')
                        name = name_words[0..<-1].join(' ').trim()
                    }
                    def user = it.author.absoluteUrl[i].tokenize('/').last()
                    def name_cec = name + "#" + user
                    Committer.add(name_cec)

		    def commitMsg = "${it.msg[i]}"
		    def owner = user
		    Matcher m4 = "${commitMsg}" =~ /Owner: ([a-zA-Z0-9]*)/
		    if(m4.find()) { // Owner of the defect 
		        owner = m4.group(1)
                    }
                    Owner.add(owner)
                }
            }
            //else {
                //echo "Git commit: " + it.commitId
            //}
        }
        //bail out if the previous build is successful
        previousBuild = currentBuild.getPreviousBuild()
        if(previousBuild == null || previousBuild.result == "SUCCESS")
            break

        currentBuild = previousBuild
    } // end of while
    echo "++++++++++++++++++++++++++++++++++++++++++++++++++++"
    
    return [CLS: ChangeNum.minus(null) , MSG: CommitMsg , CEC: Committer, OWN: Owner]
}

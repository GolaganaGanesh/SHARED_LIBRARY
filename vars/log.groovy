#!/usr/bin/env groovy

//--------------------------------------------------------------------------
// This shared library will colorized message
//--------------------------------------------------------------------------

import groovy.json.*
import groovy.transform.Field

@Field def colors = [
    white: '\033[0m',
    red: '\033[31m',
    green: '\033[0;32m',
    yellow: '\033[38;5;178;1m',
    orange: '\033[38;5;208;1m',
    blue: '\033[0;34m',
    purple: '\033[0;35m',
    indigo: '\033[38;5;21;1m',
    violet: '\033[38;5;5;1m',
    cyan: '\033[0;36m',
    grey: '\033[38;5;250;1m',
    nc: '\033[0m'
]

@Field def Symbols = [
    error: '\u274c',
    warn: '\u26a0\ufe0f',
    pass: '\u2705',
    limit: '\u26D4',
    block: '\ud83d\udeab',
    quest: '\u2753',
    excl: '\u2757',
    chat: '\ud83d\udcac',
    flag: '\ud83d\udea9',
    tag: '\ud83c\udff7\ufe0f',
    pin: '\ud83d\udccc',
    mag: '\ud83d\udd0e',
    bell: '\ud83d\udd14',
    star: '\u2b50',
    up: '\ud83d\udc4d',
    point: '\ud83d\udc49',
    down: '\ud83d\udc4e',
    bfly: '\ud83e\udd8b'
]

def err(message="") {
    inColor("[ERROR] $message", 1)
}

def critical(message="") {
    inColor("${Symbols.block} [CRITICAL] ${message}", 1)
}

def fail(message=""){
    inColor("${Symbols.error} ${message}", 1)
}

def failBuild(message="", ex = null) {
    currentBuild.result = "FAILURE"

    if(ex) {
        message += "\nException: ${ex.class}: ${ex.message}"
    }
    err message
    error("Marking build as Failed")
}

def pass(message="") {
    inColor("${Symbols.pass}$message",2 )
}

def passBuild(message="") {
    currentBuild.result = "SUCCESS"
    if ( message) { pass message }
}

def warn(message="") {
    inColor("${Symbols.warn} [WARNING] ${message}", 3)
}

def unstableBuild(message="", ex = null) {
    if(ex) {
        message += "\nException: ${ex.class}: ${ex.message}"
    }
    warn message
    unstable("Marking build as UNSTABLE")
}

def info(message="") {
    inColor("[INFO] ${message}", 4)
}

def comment(message="") {
    inColor("${Symbols.point} ${message}",172)
}

def debug(message="", col = 242) {
    // do not print if debug is false
    if ( [ params.TRACE, env.trace, params.DEBUG, env.debug ].collect{ it as String }.contains("true") ) {
       message = message.toString().split('\n').collect { "[DEBUG] $it" }.join('\n')
       inColor(message, col)
    }
}

def showEnv(cond=false) {
    if ( [ params.TRACE, env.trace, params.DEBUG, env.debug, cond ].collect{ it as String }.contains("true") ) {
        line ( sh(script: 'env|sort', returnStdout: true), 56)
    }
}

def trace(message="", col = 249) {
    if ( [ params.TRACE, env.trace ].collect{ it as String }.contains("true") ) {
       message = message.toString().split('\n').collect { "[TRACE] $it" }.join('\n')
       inColor (message, col)
    }
}

def point(message="") {
    inColor("${Symbols.point} ${message}", 136)
}

def flag(message="") {
    inColor("${Symbols.flag} ${message}", 124)
}

def inColor(message="", color='0') {
    println "${getColor(color)}$message${colors.nc}"
}

def colorsExample(ints=(0..255)){
    println "List of supported Symbols"
    Symbols.each { v,k ->  println "${v}: ${k}" }
    println "By Names:"
    colors.each { v,k ->  println "${v}: ${k}" }
    println "By Numbers:"
    ints.each { inColor("[EXAMPLE] message in custom ${it}", "${it}")  }
}

def line(text = "", color=72, String c="꒷꒦꒷", screen = 120) {
    String line = c * (screen /c.size())
    if (text) {
        line =  [ line, text.center(screen), line ].join('\n')
    }
    inColor(line, color)
}

def getColor(color){
    if ( color.toString().isNumber() ) {
            color = "\033[38;5;${color};1m"
    } else {
        color = colors[color]
    }
    return color
}

def getRoy(){
    def m = "Richard of York Gave Battle in Vain".split(' ') as List
    def str = []
    m.each {
        s = it[0].toLowerCase()
        c = colors.find{ it.key.startsWith(s)}?.value
        str.add(c + it)
    }
    println str.join(' ')
}

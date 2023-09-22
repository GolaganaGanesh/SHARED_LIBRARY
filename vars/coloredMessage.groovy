#!/usr/bin/env groovy

//--------------------------------------------------------------------------
// This shared library will colorized message
//--------------------------------------------------------------------------

def call(String color="white", String message="") {
    def colors = [
        white: '\033[0m',
        red: '\u274C\u001B[31m',
        green: '\u2705\033[0;32m',
        yellow: '\u26A0\uFE0F\033[0;33m',
        blue: '\033[0;34m',
        purple: '\033[0;35m',
        cyan: '\033[0;36m',
        nc: '\033[0m'
    ]

    echo ("${colors[color]} $message ${colors.nc}")
}

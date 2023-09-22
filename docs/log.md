# log

<font face="Verdana, Geneva, sans-serif" size="+1">shared library to colorize messages</font>


## List of methods:
|Name|Description|Usage|Example|
|:--|:--|:--|:--|
| error |Print error message| log.error "The message" | <pre><font color="red">[ERROR] The message </font></pre>|
| blocked |Print blocked message| log.blocked "The message" | <pre><font color="red">🚫  [CRITICAL] The message </font></pre>|
| fail |Print fail message| log.fail "The message" | <pre><font color="red">❌  [FAIL] The message </font></pre>|
| pass |Print pass message| log.pass "The message" | <pre><font color="green">✅  The message </font></pre>|
| warn |Print warn message| log.warn "The message" | <pre><font color="#d7af00">⚠️ [WARN] The message </font></pre>|
| info |Print info message| log.info "The message" | <pre><font color="blue">[INFO] The message </font></pre>|
| comment |Print comment message| log.comment "The message" | <pre><font color="#d78700">👉 The message </font></pre>|
| debug |Print message if params.debug is true| log.debug "The message" | <pre><font color="grey">[DEBUG] The message </font></pre>|
| trace |Print message if env.trace is true| log.trace "The message" | <pre><font color="black">[TRACE] The message </font></pre>|
| inColor |Print a message in color | log.inColor "The message", 'indigo' | <pre><font color="violet">[INCOLOR] The message </font></pre>|
| inColor |Print a message in color <a href=https://www.ditig.com/256-colors-cheat-sheet>(0..255)</a>| log.inColor "The message", 111 | <pre><font color="#87afff">The message </font></pre>|
| colorsExample |Print colors example support range| log.colorsExample (21..23) | <pre><font color="#0000ff">[EXAMPLE] The message </font><br><font color="#005f00">[EXAMPLE] The message </font><br><font color="#005f5f">[EXAMPLE] The message </font></pre>|
| line |Print line separetor| log.line "The message" | <pre><font color="#5faf87">꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷</font></pre>|
| line |Print a title | log.line "The title",88 | <pre><font color="#ff5f00">꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷<br>             The title          <br>꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷꒦꒷꒷</font></pre>|
| getColor |Print  getColor message| echo log.getColor('purple') + "The message" | <pre><font color="#800080">The message </font></pre>|



## List of supported sybols:

| Name      | Syntax      | Icon |
| :---      |    :----   | ---: |
|error|logs.Symbols.error|❌|
|pass|logs.Symbols.pass|✅|
|limit|logs.Symbols.limit|⛔|
|block|logs.Symbols.block|🚫|
|quest|logs.Symbols.quest|❓|
|excl|logs.Symbols.excl|❗|
|chat|logs.Symbols.chat|💬|
|flag|logs.Symbols.flag|🚩|
|tag|logs.Symbols.tag|🏷️|
|pin|logs.Symbols.pin|📌|
|mag|logs.Symbols.mag|🔎|
|bell|logs.Symbols.bell|🔔|
|star|logs.Symbols.star|⭐|
|up|logs.Symbols.up|👍|
|point|logs.Symbols.point|👉|
|down|logs.Symbols.down|👎|
|warn|logs.Symbols.warn|<pre>⚠️</pre>|

Quick view of colors in terminal:
>
>```bash
>for i in {0..255}; { printf '\e[38;5;%s;1m %3s \e[0m' $i  $i ; if [ $((( $i + 1) % 16 )) == 0 ] ; then echo ; fi; }
>```
